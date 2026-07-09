package com.sk.domain.mi.service.nextgen;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sk.common.enums.ResultCode;
import com.sk.common.exception.AppErrorException;
import com.sk.common.exception.AppHttpStatusException;
import com.sk.domain.mi.dto.response.nextgen.ModelExecutionOptionsRes;
import com.sk.domain.mi.dto.response.nextgen.ModelParamProposalAcceptRes;
import com.sk.domain.mi.dto.response.nextgen.ModelParamProposalRes;
import com.sk.domain.shared.client.ModelParamProposalAgentApi;
import com.sk.domain.shared.client.dto.request.AgentModelParamConfirmationReq;
import com.sk.domain.shared.client.dto.request.ModelParamPayloadItem;
import com.sk.domain.shared.client.dto.response.AgentModelParamConfirmationRes;
import com.sk.domain.shared.client.dto.response.AgentModelParamProposalCreateRes;
import com.sk.domain.shared.entity.master.tmd.ModelExecutionPreparation;
import com.sk.domain.shared.entity.master.tmm.AgentModelParamProposal;
import com.sk.domain.shared.entity.master.tmm.MiScenario;
import com.sk.domain.shared.enums.fundamentalmodel.FundamentalModelCaseStatus;
import com.sk.domain.shared.enums.fundamentalmodel.FundamentalModelExecType;
import com.sk.domain.shared.enums.nextgen.AgentModelParamProposalStatus;
import com.sk.domain.shared.enums.nextgen.ModelExecutionPreparationStatus;
import com.sk.domain.shared.enums.nextgen.ModelInputMode;
import com.sk.domain.shared.enums.nextgen.ScenarioStatus;
import com.sk.domain.shared.repository.master.tmd.ModelExecutionPreparationRepository;
import com.sk.domain.shared.repository.master.tmd.ScenarioCaseRepository;
import com.sk.domain.shared.repository.master.tmm.AgentModelParamProposalRepository;
import com.sk.domain.shared.repository.master.tmm.MiScenarioRepository;
import com.sk.domain.shared.service.permissions.AuthorizationService;
import java.net.SocketTimeoutException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
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

    private final MiScenarioRepository miScenarioRepository;
    private final ScenarioCaseRepository scenarioCaseRepository;
    private final ModelExecutionPreparationRepository modelExecutionPreparationRepository;
    private final AgentModelParamProposalRepository agentModelParamProposalRepository;
    private final ModelParamProposalAgentApi modelParamProposalAgentApi;
    private final AuthorizationService authorizationService;
    private final TransactionOperations transactionOperations;
    private final ObjectMapper objectMapper;

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
    public ModelParamProposalRes getProposal(Long scenarioId, Long modelParamProposalId) {
        ModelExecutionPreparation preparation = modelExecutionPreparationRepository
            .findByScenarioIdAndAgentProposalIdAndIsDeletedFalse(scenarioId, modelParamProposalId)
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

        return new ModelParamProposalRes(
            proposal.getModelParamProposalId(),
            proposal.getScenarioId(),
            proposal.getScenarioGroupId(),
            proposal.getEventId(),
            resolveDisplayStatus(preparation, proposal),
            resolveParameters(proposal),
            resolveFailureReason(proposal),
            preparation.getCreateDt(),
            resolveCompletedAt(proposal)
        );
    }

    @Override
    public AgentModelParamConfirmationRes confirmProposal(Long modelParamProposalId, List<ModelParamPayloadItem> confirmationParameters) {
        if (confirmationParameters == null || confirmationParameters.isEmpty()) {
            throw AppErrorException.of(ResultCode.Error.REQ_INVALID_PARAMETER, "confirmationParameters is required.");
        }

        AgentModelParamConfirmationRes response = callAgent(
            () -> modelParamProposalAgentApi.confirmProposal(
                modelParamProposalId,
                AgentModelParamConfirmationReq.of(confirmationParameters)
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

    private ModelInputMode resolveInputMode(MiScenario scenario) {
        Long eventTypeL2Id = scenario.getScenarioGroup().getEventTypeL2().getEventTypeL2Id();
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
        if (preparation.getStatus() == ModelExecutionPreparationStatus.CONFIRMING
            || preparation.getStatus() == ModelExecutionPreparationStatus.PREPARING_EXECUTION
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
        return parseArrayOrEmpty(json);
    }

    private JsonNode parseArrayOrEmpty(String json) {
        if (json == null || json.isBlank()) {
            return objectMapper.createArrayNode();
        }
        try {
            JsonNode node = objectMapper.readTree(json);
            return node != null && node.isArray() ? node : objectMapper.createArrayNode();
        } catch (JsonProcessingException e) {
            throw AppErrorException.of(ResultCode.Error.API_RESPONSE_ERROR, "Agent proposal parameter JSON is invalid.");
        }
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
