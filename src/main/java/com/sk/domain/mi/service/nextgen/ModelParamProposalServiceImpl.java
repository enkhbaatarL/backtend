package com.sk.domain.mi.service.nextgen;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sk.common.enums.ResultCode;
import com.sk.common.exception.AppErrorException;
import com.sk.common.exception.AppHttpStatusException;
import com.sk.domain.mi.dto.response.nextgen.CurrentModelParamFileRes;
import com.sk.domain.mi.dto.response.nextgen.CurrentModelParamProposalRes;
import com.sk.domain.mi.dto.response.nextgen.ModelExecutionInputFilesRes;
import com.sk.domain.mi.dto.response.nextgen.ModelExecutionOptionsRes;
import com.sk.domain.mi.dto.response.nextgen.ModelFileNode;
import com.sk.domain.mi.dto.response.nextgen.ModelParamAlign;
import com.sk.domain.mi.dto.response.nextgen.ModelParamColumn;
import com.sk.domain.mi.dto.response.nextgen.ModelParamColumnKind;
import com.sk.domain.mi.dto.response.nextgen.ModelParamProposalAcceptRes;
import com.sk.domain.mi.dto.response.nextgen.ModelParamProposalRes;
import com.sk.domain.mi.dto.response.nextgen.ModelParamShape;
import com.sk.domain.mi.dto.response.nextgen.ModelSheetNode;
import com.sk.domain.mi.service.nextgen.paramproposal.ModelParamSlotAxisResolver;
import com.sk.domain.mi.service.nextgen.paramproposal.ModelParamSlotAxisResolver.InvalidSlotAxisMetadataException;
import com.sk.domain.mi.service.nextgen.paramproposal.ModelParameterResponseProjector;
import com.sk.domain.mi.service.nextgen.paramproposal.ModelParameterResponseProjector.InvalidParameterPayloadException;
import com.sk.domain.mi.service.nextgen.paramproposal.ModelParameterTreeProjector;
import com.sk.domain.mi.service.nextgen.paramproposal.ModelParameterTreeProjector.InvalidModelParameterTreeException;
import com.sk.domain.mi.service.nextgen.paramproposal.excel.ModelInputFile;
import com.sk.domain.mi.service.nextgen.query.ModelExecutionInputFilesProjector;
import com.sk.domain.shared.client.ModelParamProposalAgentApi;
import com.sk.domain.shared.client.dto.request.AgentModelParamConfirmationReq;
import com.sk.domain.shared.client.dto.request.ModelParamPayloadItem;
import com.sk.domain.shared.client.dto.request.ModelParamTarget;
import com.sk.domain.shared.client.dto.response.AgentModelParamConfirmationRes;
import com.sk.domain.shared.client.dto.response.AgentModelParamProposalCreateRes;
import com.sk.domain.shared.entity.master.tmd.ModelExecutionPreparation;
import com.sk.domain.shared.entity.master.tmd.ScenarioCase;
import com.sk.domain.shared.entity.master.tmd.ScenarioCaseRunHistory;
import com.sk.domain.shared.entity.master.tmm.AgentModelParamProposal;
import com.sk.domain.shared.entity.master.tmm.MiModelParam;
import com.sk.domain.shared.entity.master.tmm.MiScenario;
import com.sk.domain.shared.enums.fundamentalmodel.FundamentalModelCaseStatus;
import com.sk.domain.shared.enums.fundamentalmodel.FundamentalModelExecType;
import com.sk.domain.shared.enums.nextgen.AgentModelParamProposalStatus;
import com.sk.domain.shared.enums.nextgen.BasecaseReadMode;
import com.sk.domain.shared.enums.nextgen.CurrentModelParamProposalState;
import com.sk.domain.shared.enums.nextgen.ModelExecutionPreparationStatus;
import com.sk.domain.shared.enums.nextgen.ModelExecutionStatus;
import com.sk.domain.shared.enums.nextgen.ModelInputMode;
import com.sk.domain.shared.enums.nextgen.ModelParamParameterSource;
import com.sk.domain.shared.enums.nextgen.ScenarioStatus;
import com.sk.domain.shared.repository.master.tmd.ModelExecutionPreparationRepository;
import com.sk.domain.shared.repository.master.tmd.ScenarioCaseRepository;
import com.sk.domain.shared.repository.master.tmd.ScenarioCaseRunHistoryRepository;
import com.sk.domain.shared.repository.master.tmm.AgentModelParamProposalRepository;
import com.sk.domain.shared.repository.master.tmm.MiModelParamRepository;
import com.sk.domain.shared.repository.master.tmm.MiScenarioGroupRepository;
import com.sk.domain.shared.repository.master.tmm.MiScenarioRepository;
import com.sk.domain.shared.service.permissions.AuthorizationService;
import java.net.SocketTimeoutException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionOperations;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ModelParamProposalServiceImpl implements ModelParamProposalService {

    private static final List<ModelExecutionPreparationStatus> REQUEST_CREATE_TRANSITION_FROM = List.of(
        ModelExecutionPreparationStatus.REQUESTING,
        ModelExecutionPreparationStatus.PROPOSAL_PENDING
    );
    private static final List<ModelExecutionPreparationStatus> IN_FLIGHT_PREPARATION_STATUSES = List.of(
        ModelExecutionPreparationStatus.REQUESTING,
        ModelExecutionPreparationStatus.PROPOSAL_PENDING,
        ModelExecutionPreparationStatus.CONFIRMING,
        ModelExecutionPreparationStatus.PREPARING_EXECUTION
    );

    /**
     * basecase_column_roles 파싱 시 role 경계 — 콤마 뒤에 "컬럼문자:"가 올 때만 분리한다.
     * 라벨에 콤마가 포함된 경우(예: "% change (+/-)")를 대비하며, 엑셀 병합의 ColumnRoleMap과 동일 규칙.
     */
    private static final Pattern COLUMN_ROLE_BOUNDARY = Pattern.compile(",(?=\\s*[A-Za-z]{1,3}:)");

    private final MiScenarioRepository miScenarioRepository;
    private final MiScenarioGroupRepository miScenarioGroupRepository;
    private final ScenarioCaseRepository scenarioCaseRepository;
    private final ScenarioCaseRunHistoryRepository scenarioCaseRunHistoryRepository;
    private final ModelExecutionPreparationRepository modelExecutionPreparationRepository;
    private final AgentModelParamProposalRepository agentModelParamProposalRepository;
    private final MiModelParamRepository miModelParamRepository;
    private final ModelParamProposalAgentApi modelParamProposalAgentApi;
    private final AuthorizationService authorizationService;
    private final TransactionOperations transactionOperations;
    private final ObjectMapper objectMapper;
    private final ModelParameterResponseProjector modelParameterResponseProjector;
    private final ModelParamSlotAxisResolver modelParamSlotAxisResolver;
    private final ModelParameterTreeProjector modelParameterTreeProjector;
    private final ModelExecutionInputFilesProjector modelExecutionInputFilesProjector;

    @Override
    public ModelExecutionOptionsRes getModelExecutionOptions(Long scenarioId) {
        MiScenario scenario = findScenario(scenarioId);
        ModelInputMode inputMode = resolveInputMode(scenario);
        return ModelExecutionOptionsRes.of(scenarioId, inputMode);
    }

    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public ModelParamProposalAcceptRes createProposal(Long scenarioId) {
        ModelExecutionPreparation preparation = createRequestingPreparation(scenarioId);

        AgentModelParamProposalCreateRes agentResponse;
        try {
            agentResponse = callAgent(() -> modelParamProposalAgentApi.createProposal(scenarioId), "create model parameter proposal");
            validateCreateAgentResponse(agentResponse);
        } catch (AppErrorException e) {
            handleCreateAgentError(preparation.getExecPrepId(), e);
            throw e;
        }

        ModelExecutionPreparation updated = attachAgentProposalIdAndMarkPending(
            preparation.getExecPrepId(),
            agentResponse.data().modelParamProposalId()
        );

        return new ModelParamProposalAcceptRes(
            agentResponse.data().modelParamProposalId(),
            scenarioId,
            agentResponse.data().status(),
            updated.getCreateDt(),
            buildProposalStatusEndpoint(scenarioId, agentResponse.data().modelParamProposalId())
        );
    }

    @Override
    public CurrentModelParamProposalRes getCurrentProposal(Long scenarioId) {
        MiScenario scenario = findScenario(scenarioId);
        rejectIfNotParameterProposalMode(scenario);

        return modelExecutionPreparationRepository.findFirstByScenarioIdAndIsDeletedFalseOrderByExecPrepIdDesc(scenarioId)
            .map(this::toCurrentProposalResponse)
            .orElseGet(() -> currentResponse(
                scenarioId,
                CurrentModelParamProposalState.NOT_REQUESTED,
                ModelParamParameterSource.NONE,
                null,
                null,
                null,
                null,
                null,
                null
            ));
    }

    @Override
    public ModelParamProposalRes getProposal(Long scenarioId, Long modelParamProposalId) {
        ModelExecutionPreparation preparation = modelExecutionPreparationRepository
            .findFirstByScenarioIdAndAgentProposalIdAndIsDeletedFalseOrderByExecPrepIdDesc(
                scenarioId,
                modelParamProposalId
            )
            .orElseThrow(() -> AppErrorException.of(
                ResultCode.Error.RSC_NOT_FOUND,
                "model parameter proposal preparation not found. scenarioId: %d, modelParamProposalId: %d"
                    .formatted(scenarioId, modelParamProposalId)
            ));

        AgentModelParamProposal proposal = agentModelParamProposalRepository
            .findByModelParamProposalIdAndScenarioIdAndIsDeletedFalse(modelParamProposalId, scenarioId)
            .orElseThrow(() -> AppErrorException.of(
                ResultCode.Error.RSC_NOT_FOUND,
                "Agent model parameter proposal not found. scenarioId: %d, modelParamProposalId: %d"
                    .formatted(scenarioId, modelParamProposalId)
            ));

        JsonNode parameters = resolveParameters(proposal);
        return new ModelParamProposalRes(
            proposal.getModelParamProposalId(),
            proposal.getScenarioId(),
            proposal.getScenarioGroupId(),
            proposal.getEventId(),
            resolveDisplayStatus(preparation, proposal),
            parameters,
            buildModelFiles(parameters),
            buildParameterShapes(parameters),
            resolveFailureReason(proposal),
            preparation.getCreateDt(),
            resolveCompletedAt(proposal)
        );
    }

    private CurrentModelParamProposalRes toCurrentProposalResponse(ModelExecutionPreparation preparation) {
        Optional<AgentModelParamProposal> proposal = findAgentProposal(preparation);

        if (preparation.getStatus() == ModelExecutionPreparationStatus.REQUEST_FAILED) {
            return currentResponse(
                preparation,
                proposal.orElse(null),
                CurrentModelParamProposalState.PROPOSAL_FAILED,
                ModelParamParameterSource.NONE,
                null,
                null,
                null,
                preparation.getErrorMessage()
            );
        }

        if (proposal.map(AgentModelParamProposal::getStatus).orElse(null) == AgentModelParamProposalStatus.FAILED) {
            return currentResponse(
                preparation,
                proposal.orElse(null),
                CurrentModelParamProposalState.PROPOSAL_FAILED,
                ModelParamParameterSource.NONE,
                null,
                null,
                null,
                // 26.07.29 취약점 진단 검토, '표준 라이브러리의 널 반환 값 역참조-102244', 오탐 -> if 조건이 FAILED 상태 매칭으로 proposal의 present를 보장하며, orElseThrow()는 null을 반환하지 않는 계약.
                resolveFailureReason(proposal.orElseThrow())
            );
        }

        if (preparation.getCaseId() != null) {
            return toCaseLinkedCurrentProposalResponse(preparation, proposal.orElse(null));
        }

        if (preparation.getStatus() == ModelExecutionPreparationStatus.EXECUTION_FAILED) {
            return currentResponse(
                preparation,
                proposal.orElse(null),
                CurrentModelParamProposalState.EXECUTION_FAILED,
                executionFailureParameterSource(proposal.orElse(null)),
                null,
                null,
                null,
                preparation.getErrorMessage()
            );
        }

        AgentModelParamProposal agentProposal = proposal.orElse(null);
        AgentModelParamProposalStatus agentStatus = agentProposal == null ? null : agentProposal.getStatus();

        if (preparation.getStatus() == ModelExecutionPreparationStatus.EXECUTING) {
            throw dataMismatch("EXECUTING preparation has no case_id. execPrepId: %d".formatted(preparation.getExecPrepId()));
        }

        if (preparation.getStatus() == ModelExecutionPreparationStatus.CONFIRMING
            || preparation.getStatus() == ModelExecutionPreparationStatus.PREPARING_EXECUTION
            || agentStatus == AgentModelParamProposalStatus.CONFIRMED) {
            return currentResponse(
                preparation,
                agentProposal,
                CurrentModelParamProposalState.CONFIRMED_PENDING_EXECUTION,
                ModelParamParameterSource.CONFIRMATION_PARAMETERS,
                null,
                null,
                null,
                null
            );
        }

        if (preparation.getStatus() == ModelExecutionPreparationStatus.REQUESTING
            || agentProposal == null
            || agentStatus == AgentModelParamProposalStatus.PROCESSING) {
            return currentResponse(
                preparation,
                agentProposal,
                CurrentModelParamProposalState.GENERATING,
                ModelParamParameterSource.NONE,
                null,
                null,
                null,
                null
            );
        }

        if (agentStatus == AgentModelParamProposalStatus.PROPOSED) {
            return currentResponse(
                preparation,
                agentProposal,
                CurrentModelParamProposalState.PROPOSAL_READY,
                ModelParamParameterSource.PROPOSAL_PARAMETERS,
                null,
                null,
                null,
                null
            );
        }

        throw dataMismatch(
            "Unsupported model parameter proposal current-state combination. execPrepId: %d, backendStatus: %s, agentStatus: %s"
                .formatted(preparation.getExecPrepId(), preparation.getStatus(), agentStatus)
        );
    }

    private Optional<AgentModelParamProposal> findAgentProposal(ModelExecutionPreparation preparation) {
        if (preparation.getAgentProposalId() == null) {
            return Optional.empty();
        }
        return agentModelParamProposalRepository.findByModelParamProposalIdAndScenarioIdAndIsDeletedFalse(
            preparation.getAgentProposalId(),
            preparation.getScenarioId()
        );
    }

    private CurrentModelParamProposalRes toCaseLinkedCurrentProposalResponse(
        ModelExecutionPreparation preparation,
        AgentModelParamProposal proposal
    ) {
        if (proposal == null) {
            throw dataMismatch("Case-linked preparation has no Agent proposal row. execPrepId: %d".formatted(preparation.getExecPrepId()));
        }

        ScenarioCase scenarioCase = scenarioCaseRepository.findByCaseIdAndScenarioIdAndExecTypeAndIsDeletedFalse(
                preparation.getCaseId(),
                preparation.getScenarioId(),
                FundamentalModelExecType.SCENARIO
            )
            .orElseThrow(() -> dataMismatch(
                "Case-linked preparation has no matching scenario case. execPrepId: %d, caseId: %d"
                    .formatted(preparation.getExecPrepId(), preparation.getCaseId())
            ));

        if (scenarioCase.getLastRunId() == null || scenarioCase.getLastRunId().isBlank()) {
            throw dataMismatch(
                "Case-linked preparation has no latest run id. execPrepId: %d, caseId: %d"
                    .formatted(preparation.getExecPrepId(), preparation.getCaseId())
            );
        }

        ScenarioCaseRunHistory latestRun = scenarioCaseRunHistoryRepository.findByRunIdAndIsDeletedFalse(scenarioCase.getLastRunId())
            .orElseThrow(() -> dataMismatch(
                "Case-linked preparation has no latest run history. execPrepId: %d, caseId: %d, runId: %s"
                    .formatted(preparation.getExecPrepId(), preparation.getCaseId(), scenarioCase.getLastRunId())
            ));

        if (!preparation.getCaseId().equals(latestRun.getCaseId())) {
            throw dataMismatch(
                "Latest run case mismatch. execPrepId: %d, caseId: %d, runCaseId: %d"
                    .formatted(preparation.getExecPrepId(), preparation.getCaseId(), latestRun.getCaseId())
            );
        }

        ModelExecutionStatus modelExecutionStatus = toModelExecutionStatus(latestRun);
        CurrentModelParamProposalState currentState = switch (modelExecutionStatus) {
            case RUNNING -> CurrentModelParamProposalState.EXECUTING;
            case COMPLETED -> CurrentModelParamProposalState.EXECUTED_CONFIRMED;
            case FAILED -> CurrentModelParamProposalState.EXECUTION_FAILED;
        };

        return currentResponse(
            preparation,
            proposal,
            currentState,
            modelExecutionStatus == ModelExecutionStatus.FAILED
                ? executionFailureParameterSource(proposal)
                : ModelParamParameterSource.CONFIRMATION_PARAMETERS,
            scenarioCase.getCaseId(),
            latestRun.getRunId(),
            modelExecutionStatus,
            modelExecutionStatus == ModelExecutionStatus.FAILED
                ? firstNonBlank(latestRun.getFailureReasonCode(), preparation.getErrorMessage())
                : null
        );
    }

    private CurrentModelParamProposalRes currentResponse(
        ModelExecutionPreparation preparation,
        AgentModelParamProposal proposal,
        CurrentModelParamProposalState currentState,
        ModelParamParameterSource parameterSource,
        Long caseId,
        String runId,
        ModelExecutionStatus modelExecutionStatus,
        String failureReason
    ) {
        return currentResponse(
            preparation.getScenarioId(),
            currentState,
            parameterSource,
            preparation.getExecPrepId(),
            preparation.getAgentProposalId(),
            proposal,
            caseId,
            runId,
            modelExecutionStatus,
            preparation.getCreateDt(),
            failureReason
        );
    }

    private CurrentModelParamProposalRes currentResponse(
        Long scenarioId,
        CurrentModelParamProposalState currentState,
        ModelParamParameterSource parameterSource,
        Long execPrepId,
        Long modelParamProposalId,
        AgentModelParamProposal proposal,
        Long caseId,
        String runId,
        ModelExecutionStatus modelExecutionStatus
    ) {
        return currentResponse(
            scenarioId,
            currentState,
            parameterSource,
            execPrepId,
            modelParamProposalId,
            proposal,
            caseId,
            runId,
            modelExecutionStatus,
            null,
            null
        );
    }

    private CurrentModelParamProposalRes currentResponse(
        Long scenarioId,
        CurrentModelParamProposalState currentState,
        ModelParamParameterSource parameterSource,
        Long execPrepId,
        Long modelParamProposalId,
        AgentModelParamProposal proposal,
        Long caseId,
        String runId,
        ModelExecutionStatus modelExecutionStatus,
        LocalDateTime requestedAt,
        String failureReason
    ) {
        ModelExecutionInputFilesRes executionInputFiles = resolveExecutionInputFiles(
            scenarioId,
            currentState,
            caseId
        );
        return new CurrentModelParamProposalRes(
            scenarioId,
            currentState,
            parameterSource,
            execPrepId,
            modelParamProposalId,
            proposal == null ? null : proposal.getScenarioGroupId(),
            proposal == null ? null : proposal.getEventId(),
            caseId,
            runId,
            modelExecutionStatus,
            executionInputFiles,
            projectCurrentModelFiles(parameterSource, proposal),
            failureReason,
            requestedAt,
            proposal == null ? null : proposal.getProposalCompletedAt(),
            proposal == null ? null : proposal.getConfirmedAt()
        );
    }

    private ModelExecutionInputFilesRes resolveExecutionInputFiles(
        Long scenarioId,
        CurrentModelParamProposalState currentState,
        Long caseId
    ) {
        if (currentState != CurrentModelParamProposalState.EXECUTED_CONFIRMED) {
            return null;
        }
        if (caseId == null) {
            throw dataMismatch("EXECUTED_CONFIRMED current-state has no case_id. scenarioId: %d".formatted(scenarioId));
        }
        return modelExecutionInputFilesProjector.project(scenarioId, caseId);
    }

    private ModelParamParameterSource executionFailureParameterSource(AgentModelParamProposal proposal) {
        return proposal == null || proposal.getConfirmationParameters() == null || proposal.getConfirmationParameters().isBlank()
            ? ModelParamParameterSource.NONE
            : ModelParamParameterSource.CONFIRMATION_PARAMETERS;
    }

    private ModelExecutionStatus toModelExecutionStatus(ScenarioCaseRunHistory runHistory) {
        return ModelExecutionStatus.valueOf(runHistory.getStatus().name());
    }

    private AppHttpStatusException dataMismatch(String message) {
        return AppHttpStatusException.of(HttpStatus.CONFLICT, ResultCode.Error.BIZ_RULE_VIOLATION, message);
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public AgentModelParamConfirmationRes confirmProposal(Long modelParamProposalId, List<ModelParamPayloadItem> confirmationParameters) {
        if (confirmationParameters == null || confirmationParameters.isEmpty()) {
            throw AppErrorException.of(ResultCode.Error.REQ_INVALID_PARAMETER, "confirmationParameters is required.");
        }

        AgentModelParamConfirmationRes response = callAgent(
            () -> modelParamProposalAgentApi.confirmProposal(
                modelParamProposalId,
                AgentModelParamConfirmationReq.of(toAgentConfirmationParameters(confirmationParameters))
            ),
            "confirm model parameter proposal"
        );

        if (!response.isSuccess()
            || response.data() == null
            || !modelParamProposalId.equals(response.data().modelParamProposalId())
            || response.data().status() != AgentModelParamProposalStatus.CONFIRMED) {
            throw AppErrorException.of(
                ResultCode.Error.API_RESPONSE_ERROR,
                "Agent confirmation response is invalid. modelParamProposalId: %d".formatted(modelParamProposalId)
            );
        }
        return response;
    }

    private List<ModelParamPayloadItem> toAgentConfirmationParameters(
        List<ModelParamPayloadItem> confirmationParameters
    ) {
        return confirmationParameters.stream()
            .map(item -> new ModelParamPayloadItem(
                item.parameterName(),
                toAgentTarget(item.modelTarget()),
                item.rationale(),
                item.values()
            ))
            .toList();
    }

    private ModelParamTarget toAgentTarget(ModelParamTarget target) {
        if (target == null) {
            return null;
        }
        String modelFile = target.modelFile();
        if (modelFile != null && !modelFile.isBlank()) {
            modelFile = ModelInputFile.from(modelFile).getCode();
        }
        return new ModelParamTarget(
            modelFile,
            target.modelSheet(),
            target.tableName(),
            target.valueFieldNames(),
            target.slotAxesFieldNames()
        );
    }

    private ModelExecutionPreparation createRequestingPreparation(Long scenarioId) {
        return transactionOperations.execute(status -> {
            MiScenario scenario = findScenarioForUpdate(scenarioId);
            rejectDiscardedScenario(scenario);
            rejectIfNotParameterProposalMode(scenario);
            rejectIfActiveExecutionExists(scenarioId);
            rejectIfActivePreparationExists(scenarioId);

            Integer actorUserNo = authorizationService.getUserNo();
            try {
                return modelExecutionPreparationRepository.saveAndFlush(
                    ModelExecutionPreparation.createRequesting(scenarioId, actorUserNo)
                );
            } catch (DataIntegrityViolationException e) {
                log.info("Active model parameter proposal preparation unique guard rejected create. scenarioId={}", scenarioId, e);
                throw activePreparationConflict(scenarioId);
            }
        });
    }

    private ModelExecutionPreparation attachAgentProposalIdAndMarkPending(Long execPrepId, Long modelParamProposalId) {
        return transactionOperations.execute(status -> {
            String actorLoginId = authorizationService.getSubject();
            LocalDateTime now = LocalDateTime.now();
            int writeCount = modelExecutionPreparationRepository.writeAgentProposalIdIfAbsent(
                execPrepId,
                modelParamProposalId,
                actorLoginId,
                now
            );
            ModelExecutionPreparation current = modelExecutionPreparationRepository.findById(execPrepId)
                .orElseThrow(() -> AppErrorException.of(
                    ResultCode.Error.RSC_NOT_FOUND,
                    "model execution preparation not found after Agent response. execPrepId: %d".formatted(execPrepId)
                ));
            if (writeCount == 0 && !modelParamProposalId.equals(current.getAgentProposalId())) {
                throw AppHttpStatusException.of(
                    HttpStatus.CONFLICT,
                    ResultCode.Error.BIZ_RULE_VIOLATION,
                    "Agent proposal correlation conflict. execPrepId: %d".formatted(execPrepId)
                );
            }

            modelExecutionPreparationRepository.transitionStatus(
                execPrepId,
                REQUEST_CREATE_TRANSITION_FROM,
                ModelExecutionPreparationStatus.PROPOSAL_PENDING,
                null,
                actorLoginId,
                now
            );
            return modelExecutionPreparationRepository.findById(execPrepId)
                .orElseThrow(() -> AppErrorException.of(
                    ResultCode.Error.RSC_NOT_FOUND,
                    "model execution preparation not found after status transition. execPrepId: %d".formatted(execPrepId)
                ));
        });
    }

    private void handleCreateAgentError(Long execPrepId, AppErrorException e) {
        ResultCode.Error resultCode = e.getErrorCode();
        if (resultCode == ResultCode.Error.API_TIMEOUT) {
            log.info("Keep REQUESTING preparation after Agent timeout. execPrepId={}", execPrepId);
            return;
        }

        transactionOperations.executeWithoutResult(status -> modelExecutionPreparationRepository.transitionStatus(
            execPrepId,
            REQUEST_CREATE_TRANSITION_FROM,
            ModelExecutionPreparationStatus.REQUEST_FAILED,
            truncateErrorMessage(e.getMessage()),
            authorizationService.getSubject(),
            LocalDateTime.now()
        ));
    }

    private void validateCreateAgentResponse(AgentModelParamProposalCreateRes response) {
        if (!response.isSuccess()
            || response.data() == null
            || response.data().modelParamProposalId() == null
            || response.data().status() != AgentModelParamProposalStatus.PROCESSING) {
            throw AppErrorException.of(ResultCode.Error.API_RESPONSE_ERROR, "Agent proposal create response is invalid.");
        }
    }

    private <T> T callAgent(Supplier<T> call, String action) {
        try {
            return call.get();
        } catch (ResourceAccessException e) {
            ResultCode.Error code = isTimeout(e) ? ResultCode.Error.API_TIMEOUT : ResultCode.Error.API_SERVICE_UNAVAILABLE;
            log.error("[ModelParamProposal Agent] {} communication error: {}", action, e.getMessage(), e);
            throw AppErrorException.of(code, "Agent communication error (%s)".formatted(action));
        } catch (RestClientResponseException e) {
            log.error("[ModelParamProposal Agent] {} response error: status={}, body={}", action, e.getStatusCode(), e.getResponseBodyAsString(), e);
            throw AppErrorException.of(ResultCode.Error.API_RESPONSE_ERROR, "Agent response error (%s)".formatted(action));
        } catch (RestClientException e) {
            log.error("[ModelParamProposal Agent] {} call failed: {}", action, e.getMessage(), e);
            throw AppErrorException.of(ResultCode.Error.API_CALL_FAILED, "Agent call failed (%s)".formatted(action));
        }
    }

    private boolean isTimeout(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof SocketTimeoutException) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private MiScenario findScenario(Long scenarioId) {
        return miScenarioRepository.findByScenarioIdAndIsDeletedFalse(scenarioId)
            .orElseThrow(() -> AppErrorException.of(
                ResultCode.Error.RSC_NOT_FOUND,
                "Scenario not found. scenarioId: %d".formatted(scenarioId)
            ));
    }

    private MiScenario findScenarioForUpdate(Long scenarioId) {
        return miScenarioRepository.findByScenarioIdForUpdate(scenarioId)
            .orElseThrow(() -> AppErrorException.of(
                ResultCode.Error.RSC_NOT_FOUND,
                "Scenario not found. scenarioId: %d".formatted(scenarioId)
            ));
    }

    /**
     * 수급모델 입력 방식 판정.
     * 그룹의 직접 L2 컬럼 제거(멀티 이벤트, migration 0023)로 매핑의 기준 이벤트 L2 를 사용한다.
     * 다중 이벤트 그룹의 L2 집합 판정 규칙이 확정되기 전까지의 잠정 규칙이며, L2 미확인 시 제안 대상으로 판정된다.
     */
    private ModelInputMode resolveInputMode(MiScenario scenario) {
        Long scenarioGroupId = scenario.getScenarioGroup().getScenarioGroupId();
        Long eventTypeL2Id = miScenarioGroupRepository.findEventTypeL2IdByScenarioGroupId(scenarioGroupId).orElse(null);
        return ModelInputMode.from(eventTypeL2Id);
    }

    private void rejectDiscardedScenario(MiScenario scenario) {
        if (scenario.getStatus() == ScenarioStatus.DISCARDED) {
            throw AppErrorException.of(
                ResultCode.Error.BIZ_RULE_VIOLATION,
                "Discarded scenario cannot request model parameter proposal. scenarioId: %d".formatted(scenario.getScenarioId())
            );
        }
    }

    private void rejectIfNotParameterProposalMode(MiScenario scenario) {
        if (!resolveInputMode(scenario).isParameterProposal()) {
            throw AppHttpStatusException.of(
                HttpStatus.CONFLICT,
                ResultCode.Error.BIZ_RULE_VIOLATION,
                "This scenario uses FILE_UPLOAD mode. model parameter proposal is not allowed. scenarioId: %d"
                    .formatted(scenario.getScenarioId())
            );
        }
    }

    private void rejectIfActiveExecutionExists(Long scenarioId) {
        boolean exists = scenarioCaseRepository.existsByScenarioIdAndExecTypeAndStatusAndIsDeletedFalse(
            scenarioId,
            FundamentalModelExecType.SCENARIO,
            FundamentalModelCaseStatus.RUNNING
        );
        if (exists) {
            throw AppHttpStatusException.of(
                HttpStatus.CONFLICT,
                ResultCode.Error.RSC_ALREADY_EXISTS,
                "해당 scenario에는 이미 RUNNING 상태의 model execution이 존재합니다. scenarioId: %d".formatted(scenarioId)
            );
        }
    }

    private void rejectIfActivePreparationExists(Long scenarioId) {
        Optional<ModelExecutionPreparation> existing = modelExecutionPreparationRepository.findLatestByScenarioIdAndStatuses(
            scenarioId,
            IN_FLIGHT_PREPARATION_STATUSES
        );
        if (existing.isPresent()) {
            throw activePreparationConflict(scenarioId);
        }
    }

    private AppHttpStatusException activePreparationConflict(Long scenarioId) {
        return AppHttpStatusException.of(
            HttpStatus.CONFLICT,
            ResultCode.Error.RSC_ALREADY_EXISTS,
            "해당 scenario에는 이미 진행 중인 model parameter proposal preparation이 존재합니다. scenarioId: %d".formatted(scenarioId)
        );
    }

    private String resolveDisplayStatus(ModelExecutionPreparation preparation, AgentModelParamProposal proposal) {
        if (preparation.getStatus() == ModelExecutionPreparationStatus.PREPARING_EXECUTION
            || preparation.getStatus() == ModelExecutionPreparationStatus.EXECUTING
            || preparation.getStatus() == ModelExecutionPreparationStatus.EXECUTION_FAILED) {
            return preparation.getStatus().name();
        }
        if (preparation.getStatus() == ModelExecutionPreparationStatus.REQUEST_FAILED) {
            return ModelExecutionPreparationStatus.REQUEST_FAILED.name();
        }
        return proposal.getStatus().name();
    }

    private JsonNode resolveParameters(AgentModelParamProposal proposal) {
        String json = switch (proposal.getStatus()) {
            case PROPOSED -> proposal.getProposalParameters();
            case CONFIRMED -> proposal.getConfirmationParameters();
            case PROCESSING, FAILED -> null;
        };
        JsonNode parameters = parseArrayOrEmpty(json);
        try {
            return modelParameterResponseProjector.projectParameterArray(
                parameters,
                resolveGroupedRationaleSlotAxes(parameters)
            );
        } catch (InvalidParameterPayloadException | InvalidSlotAxisMetadataException e) {
            throw invalidAgentParameterJson();
        }
    }

    private List<List<String>> resolveGroupedRationaleSlotAxes(JsonNode parameters) {
        if (parameters == null || !parameters.isArray()) {
            throw new InvalidSlotAxisMetadataException();
        }
        if (parameters.isEmpty()) {
            return List.of();
        }

        Set<String> parameterNames = new LinkedHashSet<>();
        for (JsonNode parameter : parameters) {
            parameterNames.add(requiredGroupingParameterName(parameter));
        }
        Map<String, List<MiModelParam>> mastersByName = miModelParamRepository
            .findByParameterNameInAndIsDeletedFalse(parameterNames)
            .stream()
            .collect(Collectors.groupingBy(
                MiModelParam::getParameterName,
                LinkedHashMap::new,
                Collectors.toList()
            ));

        List<List<String>> groupingAxes = new ArrayList<>();
        for (JsonNode parameter : parameters) {
            String parameterName = requiredGroupingParameterName(parameter);
            List<MiModelParam> masters = mastersByName.get(parameterName);
            if (masters == null || masters.size() != 1) {
                throw new InvalidSlotAxisMetadataException();
            }
            groupingAxes.add(modelParamSlotAxisResolver.resolve(
                masters.get(0).getBasecaseColumnRoles(),
                requiredGroupingSlotAxes(parameter)
            ));
        }
        return List.copyOf(groupingAxes);
    }

    private String requiredGroupingParameterName(JsonNode parameter) {
        JsonNode parameterName = parameter == null ? null : parameter.get("parameterName");
        if (parameterName == null || !parameterName.isTextual() || parameterName.textValue().isBlank()) {
            throw new InvalidSlotAxisMetadataException();
        }
        return parameterName.textValue();
    }

    private List<String> requiredGroupingSlotAxes(JsonNode parameter) {
        JsonNode modelTarget = parameter == null ? null : parameter.get("modelTarget");
        JsonNode slotAxes = modelTarget == null ? null : modelTarget.get("slotAxesFieldNames");
        if (slotAxes == null || !slotAxes.isArray() || slotAxes.isEmpty()) {
            throw new InvalidSlotAxisMetadataException();
        }
        List<String> axes = new ArrayList<>();
        for (JsonNode axis : slotAxes) {
            if (!axis.isTextual() || axis.textValue().isBlank()) {
                throw new InvalidSlotAxisMetadataException();
            }
            axes.add(axis.textValue());
        }
        return List.copyOf(axes);
    }

    private List<CurrentModelParamFileRes> projectCurrentModelFiles(
        ModelParamParameterSource parameterSource,
        AgentModelParamProposal proposal
    ) {
        if (parameterSource == ModelParamParameterSource.NONE) {
            return List.of();
        }
        if (proposal == null) {
            throw currentParameterMismatch();
        }

        String json = switch (parameterSource) {
            case NONE -> null;
            case PROPOSAL_PARAMETERS -> proposal.getProposalParameters();
            case CONFIRMATION_PARAMETERS -> proposal.getConfirmationParameters();
        };
        try {
            return modelParameterTreeProjector.project(json);
        } catch (InvalidModelParameterTreeException e) {
            throw currentParameterMismatch();
        }
    }

    private AppHttpStatusException currentParameterMismatch() {
        return AppHttpStatusException.of(
            HttpStatus.CONFLICT,
            ResultCode.Error.BIZ_RULE_VIOLATION,
            "Model parameter response data is inconsistent."
        );
    }

    private List<ModelFileNode> buildModelFiles(JsonNode parameters) {
        Map<String, Map<String, Set<String>>> tree = new LinkedHashMap<>();
        if (parameters != null && parameters.isArray()) {
            for (JsonNode parameter : parameters) {
                JsonNode modelTarget = parameter.get("modelTarget");
                if (modelTarget == null || modelTarget.isNull()) {
                    continue;
                }
                String modelFile = textOrNull(modelTarget.get("modelFile"));
                String modelSheet = textOrNull(modelTarget.get("modelSheet"));
                if (modelFile == null || modelSheet == null) {
                    continue;
                }
                Set<String> tableNames = tree
                    .computeIfAbsent(modelFile, key -> new LinkedHashMap<>())
                    .computeIfAbsent(modelSheet, key -> new LinkedHashSet<>());
                String tableName = textOrNull(modelTarget.get("tableName"));
                if (tableName != null) {
                    tableNames.add(tableName);
                }
            }
        }
        return tree.entrySet().stream()
            .map(fileEntry -> new ModelFileNode(
                fileEntry.getKey(),
                fileEntry.getValue().entrySet().stream()
                    .map(sheetEntry -> new ModelSheetNode(sheetEntry.getKey(), List.copyOf(sheetEntry.getValue())))
                    .toList()
            ))
            .toList();
    }

    private String textOrNull(JsonNode node) {
        return node == null || node.isNull() ? null : node.asText();
    }

    private List<ModelParamShape> buildParameterShapes(JsonNode parameters) {
        if (parameters == null || !parameters.isArray() || parameters.isEmpty()) {
            return List.of();
        }
        // parameterName 순서 유지·중복 제거. 컬럼 태깅(slot/value)과 정렬(값 타입)에 payload 원소가 필요
        Map<String, JsonNode> paramByName = new LinkedHashMap<>();
        for (JsonNode parameter : parameters) {
            String parameterName = textOrNull(parameter.get("parameterName"));
            if (parameterName == null || paramByName.containsKey(parameterName)) {
                continue;
            }
            paramByName.put(parameterName, parameter);
        }
        if (paramByName.isEmpty()) {
            return List.of();
        }
        Map<String, MiModelParam> masterByName = miModelParamRepository
            .findByParameterNameInAndIsDeletedFalse(paramByName.keySet()).stream()
            .collect(Collectors.toMap(MiModelParam::getParameterName, Function.identity(), (first, second) -> first));
        return paramByName.entrySet().stream()
            .map(entry -> {
                MiModelParam master = masterByName.get(entry.getKey());
                return master == null ? null : toModelParamShape(master, entry.getValue());
            })
            .filter(Objects::nonNull)
            .toList();
    }

    /** payload modelTarget의 문자열 배열 필드 추출 (원래 순서·표기 유지). */
    private List<String> extractStringArray(JsonNode parameter, String fieldName) {
        JsonNode modelTarget = parameter.get("modelTarget");
        if (modelTarget == null || modelTarget.isNull()) {
            return List.of();
        }
        JsonNode array = modelTarget.get(fieldName);
        if (array == null || !array.isArray()) {
            return List.of();
        }
        List<String> values = new ArrayList<>();
        for (JsonNode node : array) {
            String value = textOrNull(node);
            if (value != null) {
                values.add(value.trim());
            }
        }
        return values;
    }

    private ModelParamShape toModelParamShape(MiModelParam master, JsonNode parameter) {
        List<String> slotAxes = extractStringArray(parameter, "slotAxesFieldNames");
        List<String> valueFields = extractStringArray(parameter, "valueFieldNames");
        Set<String> slotAxisNames = slotAxes.stream()
            .map(axis -> axis.toLowerCase())
            .collect(Collectors.toCollection(LinkedHashSet::new));
        JsonNode values = parameter.get("values");

        List<ModelParamColumn> columns = parseColumnRoles(master.getBasecaseColumnRoles(), slotAxisNames, values);
        if (master.getBasecaseReadMode() == BasecaseReadMode.MATRIX_TIME_COLUMNS) {
            columns = appendMatrixColumns(columns, slotAxes, valueFields, values);
        }
        return new ModelParamShape(
            master.getParameterName(),
            master.getModelFile(),
            master.getModelSheet(),
            master.getModelTable(),
            master.getBasecaseReadMode(),
            columns,
            master.getSemanticAxes()
        );
    }

    private List<ModelParamColumn> parseColumnRoles(String columnRoles, Set<String> slotAxisNames, JsonNode values) {
        if (columnRoles == null || columnRoles.isBlank()) {
            return List.of();
        }
        List<ModelParamColumn> columns = new ArrayList<>();
        for (String token : COLUMN_ROLE_BOUNDARY.split(columnRoles)) {
            String trimmed = token.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            int separator = trimmed.indexOf(':');
            String column = separator < 0 ? null : trimmed.substring(0, separator).trim();
            String role = separator < 0 ? trimmed : trimmed.substring(separator + 1).trim();
            ModelParamColumnKind kind = slotAxisNames.contains(role.toLowerCase())
                ? ModelParamColumnKind.SLOT
                : ModelParamColumnKind.VALUE;
            columns.add(new ModelParamColumn(column, role, kind, resolveAlign(kind, role, values)));
        }
        return columns;
    }

    /**
     * MATRIX_TIME_COLUMNS 보강. 마스터 column_roles는 왼쪽 고정 컬럼(=slot 축 일부)만 담으므로,
     * 가로 시간 컬럼 헤더 축(slot 축 중 고정 컬럼에 없는 나머지, 예: Year/Month/Period)과
     * 값 컬럼(valueFieldNames)을 뒤에 붙인다. 실제 시간 컬럼은 FE가 values[]로 생성한다.
     */
    private List<ModelParamColumn> appendMatrixColumns(
        List<ModelParamColumn> fixedColumns,
        List<String> slotAxes,
        List<String> valueFields,
        JsonNode values
    ) {
        Set<String> existingRoles = fixedColumns.stream()
            .map(column -> column.role().toLowerCase())
            .collect(Collectors.toCollection(LinkedHashSet::new));

        List<ModelParamColumn> columns = new ArrayList<>(fixedColumns);
        for (String axis : slotAxes) {
            if (!existingRoles.contains(axis.toLowerCase())) {
                columns.add(new ModelParamColumn(
                    null, axis, ModelParamColumnKind.TIME_AXIS,
                    resolveAlign(ModelParamColumnKind.TIME_AXIS, axis, values)
                ));
            }
        }
        for (String valueField : valueFields) {
            columns.add(new ModelParamColumn(
                null, valueField, ModelParamColumnKind.VALUE,
                resolveAlign(ModelParamColumnKind.VALUE, valueField, values)
            ));
        }
        return columns;
    }

    /** 데이터 타입 기준 정렬: values[] 샘플이 숫자면 RIGHT, 문자면 LEFT. 샘플이 없으면 값 컬럼은 RIGHT, 축은 LEFT. */
    private ModelParamAlign resolveAlign(ModelParamColumnKind kind, String role, JsonNode values) {
        JsonNode sample = sampleColumnValue(kind, role, values);
        if (sample == null) {
            return kind == ModelParamColumnKind.VALUE ? ModelParamAlign.RIGHT : ModelParamAlign.LEFT;
        }
        return sample.isNumber() ? ModelParamAlign.RIGHT : ModelParamAlign.LEFT;
    }

    private JsonNode sampleColumnValue(ModelParamColumnKind kind, String role, JsonNode values) {
        if (values == null || !values.isArray()) {
            return null;
        }
        for (JsonNode row : values) {
            JsonNode cell = kind == ModelParamColumnKind.VALUE
                ? firstPresent(row.get("proposedValue"), row.get("basecaseValue"))
                : fieldIgnoreCase(row, role);
            if (cell != null && !cell.isNull()) {
                return cell;
            }
        }
        return null;
    }

    private JsonNode firstPresent(JsonNode... nodes) {
        for (JsonNode node : nodes) {
            if (node != null && !node.isNull()) {
                return node;
            }
        }
        return null;
    }

    private JsonNode fieldIgnoreCase(JsonNode row, String fieldName) {
        if (row == null || !row.isObject()) {
            return null;
        }
        JsonNode exact = row.get(fieldName);
        if (exact != null) {
            return exact;
        }
        Iterator<Map.Entry<String, JsonNode>> fields = row.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> field = fields.next();
            if (field.getKey().equalsIgnoreCase(fieldName)) {
                return field.getValue();
            }
        }
        return null;
    }

    private JsonNode parseArrayOrEmpty(String json) {
        if (json == null || json.isBlank()) {
            return objectMapper.createArrayNode();
        }
        try {
            JsonNode node = objectMapper.readTree(json);
            return node != null && node.isArray() ? node : objectMapper.createArrayNode();
        } catch (JsonProcessingException e) {
            throw invalidAgentParameterJson();
        }
    }

    private AppErrorException invalidAgentParameterJson() {
        return AppErrorException.of(
            ResultCode.Error.API_RESPONSE_ERROR,
            "Agent proposal parameter JSON is invalid."
        );
    }

    private String resolveFailureReason(AgentModelParamProposal proposal) {
        if (proposal.getStatus() != AgentModelParamProposalStatus.FAILED) {
            return null;
        }
        if (proposal.getErrorMessage() != null && !proposal.getErrorMessage().isBlank()) {
            return proposal.getErrorMessage();
        }
        return proposal.getErrorCode();
    }

    private LocalDateTime resolveCompletedAt(AgentModelParamProposal proposal) {
        if (proposal.getStatus() == AgentModelParamProposalStatus.CONFIRMED) {
            return proposal.getConfirmedAt();
        }
        return proposal.getProposalCompletedAt();
    }

    private String buildProposalStatusEndpoint(Long scenarioId, Long modelParamProposalId) {
        return "/api/v1/mi/next-gen/scenarios/%d/model-param-proposals/%d".formatted(scenarioId, modelParamProposalId);
    }

    private String truncateErrorMessage(String message) {
        if (message == null || message.length() <= 1000) {
            return message;
        }
        return message.substring(0, 1000);
    }

}
