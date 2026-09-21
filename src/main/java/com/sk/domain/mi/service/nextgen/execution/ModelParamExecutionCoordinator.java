package com.sk.domain.mi.service.nextgen.execution;

import com.sk.common.enums.ResultCode;
import com.sk.common.exception.AppErrorException;
import com.sk.common.exception.AppHttpStatusException;
import com.sk.domain.mi.dto.request.nextgen.ModelParamProposalExecutionReq;
import com.sk.domain.mi.service.nextgen.ModelParamProposalService;
import com.sk.domain.mi.service.nextgen.paramproposal.ModelParamConfirmationValidator;
import com.sk.domain.mi.service.nextgen.paramproposal.excel.MergedModelInputFiles;
import com.sk.domain.mi.service.nextgen.paramproposal.excel.ModelParamAdjustment;
import com.sk.domain.mi.service.nextgen.paramproposal.excel.ModelParamExcelMergeService;
import com.sk.domain.shared.entity.master.tmd.ModelExecutionPreparation;
import com.sk.domain.shared.entity.master.tmm.AgentModelParamProposal;
import com.sk.domain.shared.entity.master.tmm.MiScenario;
import com.sk.domain.shared.enums.fundamentalmodel.FundamentalModelCaseStatus;
import com.sk.domain.shared.enums.fundamentalmodel.FundamentalModelExecType;
import com.sk.domain.shared.enums.nextgen.AgentModelParamProposalStatus;
import com.sk.domain.shared.enums.nextgen.ModelExecutionPreparationStatus;
import com.sk.domain.shared.repository.master.tmd.ModelExecutionPreparationRepository;
import com.sk.domain.shared.repository.master.tmd.ScenarioCaseRepository;
import com.sk.domain.shared.repository.master.tmm.AgentModelParamProposalRepository;
import com.sk.domain.shared.service.permissions.AuthorizationService;
import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionOperations;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Component
@RequiredArgsConstructor
public class ModelParamExecutionCoordinator {

    private static final int PREPARATION_ERROR_MAX_LENGTH = 1000;
    private static final List<ModelExecutionPreparationStatus> IN_FLIGHT_PREPARATION_STATUSES = List.of(
        ModelExecutionPreparationStatus.REQUESTING,
        ModelExecutionPreparationStatus.PROPOSAL_PENDING,
        ModelExecutionPreparationStatus.CONFIRMING,
        ModelExecutionPreparationStatus.PREPARING_EXECUTION
    );
    private static final List<ModelExecutionPreparationStatus> PRE_EXECUTION_FAILURE_STATUSES = List.of(
        ModelExecutionPreparationStatus.CONFIRMING,
        ModelExecutionPreparationStatus.PREPARING_EXECUTION
    );

    private final ModelExecutionPreparationRepository modelExecutionPreparationRepository;
    private final ScenarioCaseRepository scenarioCaseRepository;
    private final AgentModelParamProposalRepository agentModelParamProposalRepository;
    private final ModelParamProposalService modelParamProposalService;
    private final ModelParamConfirmationValidator confirmationValidator;
    private final ModelParamExcelMergeService modelParamExcelMergeService;
    private final AuthorizationService authorizationService;
    private final TransactionOperations transactionOperations;
    private final EntityManager entityManager;

    @Transactional(propagation = Propagation.MANDATORY)
    public ExecutionConfirmationContext begin(
        MiScenario scenario,
        ModelParamProposalExecutionReq request
    ) {
        Long scenarioId = scenario.getScenarioId();
        if (request == null || request.modelParamProposalId() == null) {
            throw AppErrorException.of(ResultCode.Error.REQ_INVALID_PARAMETER, "modelParamProposalId is required.");
        }

        AgentModelParamProposal agentProposal = findAgentProposal(scenarioId, request.modelParamProposalId());
        validateAgentProposalSnapshot(scenario, agentProposal);
        confirmationValidator.validateAgainstProposal(
            agentProposal.getProposalParameters(),
            request.confirmationParameters()
        );
        rejectIfActiveExecutionExists(scenarioId);

        boolean confirmationRequired = switch (agentProposal.getStatus()) {
            case PROPOSED -> true;
            case CONFIRMED -> !confirmationValidator.matchesConfirmation(
                agentProposal.getConfirmationParameters(),
                request.confirmationParameters()
            );
            // ABSTAINED(보류)는 실패와 동일 처리 — 범위 이탈 값이므로 실행을 허용하지 않는다.
            case PROCESSING, FAILED, ABSTAINED -> throw AppHttpStatusException.of(
                HttpStatus.CONFLICT,
                ResultCode.Error.BIZ_RULE_VIOLATION,
                "Agent proposal must be PROPOSED or CONFIRMED for execution. modelParamProposalId: %d"
                    .formatted(request.modelParamProposalId())
            );
        };

        Integer actorUserNo = authorizationService.getUserNo();
        String actorLoginId = authorizationService.getSubject();
        Optional<ModelExecutionPreparation> activePreparation = modelExecutionPreparationRepository
            .findLatestByScenarioIdAndStatuses(scenarioId, IN_FLIGHT_PREPARATION_STATUSES);

        ModelExecutionPreparation preparation;
        if (activePreparation.isPresent()) {
            preparation = activePreparation.orElseThrow();
            // 26.07.29 취약점 진단 검토, '표준 라이브러리의 널 반환 값 역참조-102250,102252', 오탐 -> 위 isPresent() 검사로 present가 보장되며 orElseThrow()는 null을 반환하지 않는 계약.
            validateReusablePendingPreparation(preparation, request.modelParamProposalId());
            int transitioned = modelExecutionPreparationRepository.transitionStatus(
                preparation.getExecPrepId(),
                List.of(ModelExecutionPreparationStatus.PROPOSAL_PENDING),
                ModelExecutionPreparationStatus.CONFIRMING,
                null,
                actorLoginId,
                LocalDateTime.now()
            );
            if (transitioned != 1) {
                throw activePreparationConflict(scenarioId);
            }
        } else {
            ModelExecutionPreparation latest = modelExecutionPreparationRepository
                .findFirstByScenarioIdAndAgentProposalIdAndIsDeletedFalseOrderByExecPrepIdDesc(
                    scenarioId,
                    request.modelParamProposalId()
                )
                .orElseThrow(() -> proposalPreparationNotFound(scenarioId, request.modelParamProposalId()));
            validateRepeatExecutionSource(latest, scenarioId);
            try {
                preparation = modelExecutionPreparationRepository.saveAndFlush(
                    ModelExecutionPreparation.createConfirmingExecution(
                        scenarioId,
                        actorUserNo,
                        request.modelParamProposalId()
                    )
                );
            } catch (DataIntegrityViolationException e) {
                log.info("Active execution preparation unique guard rejected JSON execution. scenarioId={}", scenarioId, e);
                throw activePreparationConflict(scenarioId);
            }
        }

        // 26.07.29 취약점 진단 검토, '표준 라이브러리의 널 반환 값 역참조-102251', 오탐 -> preparation은 양 분기에서 각각 orElseThrow()/saveAndFlush()로 할당되어 not null 보장됨.
        return new ExecutionConfirmationContext(preparation.getExecPrepId(), confirmationRequired);
    }

    public List<MultipartFile> confirmAndMerge(
        Long scenarioId,
        ModelParamProposalExecutionReq request,
        ExecutionConfirmationContext context
    ) {
        if (context.confirmationRequired()) {
            modelParamProposalService.confirmProposal(
                request.modelParamProposalId(),
                request.confirmationParameters()
            );
        }

        String confirmationParametersJson = confirmPreparationAndReadFreshAgentSnapshot(
            scenarioId,
            request,
            context.execPrepId()
        );
        MergedModelInputFiles mergedFiles = modelParamExcelMergeService.merge(
            ModelParamAdjustment.listFromJson(confirmationParametersJson)
        );
        return mergedFiles.toMultipartFiles();
    }

    public void markFailed(Long execPrepId, RuntimeException failure) {
        try {
            transactionOperations.executeWithoutResult(status -> {
                int transitioned = modelExecutionPreparationRepository.transitionStatus(
                    execPrepId,
                    PRE_EXECUTION_FAILURE_STATUSES,
                    ModelExecutionPreparationStatus.EXECUTION_FAILED,
                    preparationFailureMessage(failure),
                    authorizationService.getSubject(),
                    LocalDateTime.now()
                );
                if (transitioned == 0) {
                    log.info("Skip marking non-preparing execution preparation as failed. execPrepId={}", execPrepId);
                }
            });
        } catch (RuntimeException updateFailure) {
            log.error("Failed to mark execution preparation as failed. execPrepId={}", execPrepId, updateFailure);
        }
    }

    public void rejectIfActiveExecutionExists(Long scenarioId) {
        boolean exists = scenarioCaseRepository.existsByScenarioIdAndExecTypeAndStatusAndIsDeletedFalse(
            scenarioId,
            FundamentalModelExecType.SCENARIO,
            FundamentalModelCaseStatus.RUNNING
        );
        if (exists) {
            throw AppHttpStatusException.of(
                HttpStatus.CONFLICT,
                ResultCode.Error.RSC_ALREADY_EXISTS,
                "해당 scenario에는 이미 RUNNING 상태의 model execution이 존재합니다. scenarioId: %d"
                    .formatted(scenarioId)
            );
        }
    }

    private String confirmPreparationAndReadFreshAgentSnapshot(
        Long scenarioId,
        ModelParamProposalExecutionReq request,
        Long execPrepId
    ) {
        String confirmationParametersJson = transactionOperations.execute(status -> {
            entityManager.clear();
            AgentModelParamProposal freshProposal = findAgentProposal(scenarioId, request.modelParamProposalId());
            if (freshProposal.getStatus() != AgentModelParamProposalStatus.CONFIRMED
                || freshProposal.getConfirmationParameters() == null
                || freshProposal.getConfirmationParameters().isBlank()
                || !confirmationValidator.matchesConfirmation(
                    freshProposal.getConfirmationParameters(),
                    request.confirmationParameters()
                )) {
                throw AppErrorException.of(
                    ResultCode.Error.API_RESPONSE_ERROR,
                    "Agent confirmation snapshot is not ready or does not match the request. modelParamProposalId: %d"
                        .formatted(request.modelParamProposalId())
                );
            }

            int transitioned = modelExecutionPreparationRepository.transitionStatus(
                execPrepId,
                List.of(ModelExecutionPreparationStatus.CONFIRMING),
                ModelExecutionPreparationStatus.PREPARING_EXECUTION,
                null,
                authorizationService.getSubject(),
                LocalDateTime.now()
            );
            if (transitioned != 1) {
                throw AppHttpStatusException.of(
                    HttpStatus.CONFLICT,
                    ResultCode.Error.BIZ_RULE_VIOLATION,
                    "model execution preparation cannot enter PREPARING_EXECUTION. execPrepId: %d"
                        .formatted(execPrepId)
                );
            }
            return freshProposal.getConfirmationParameters();
        });
        if (confirmationParametersJson == null || confirmationParametersJson.isBlank()) {
            throw AppErrorException.of(
                ResultCode.Error.API_RESPONSE_ERROR,
                "Agent confirmation snapshot is empty. modelParamProposalId: %d"
                    .formatted(request.modelParamProposalId())
            );
        }
        return confirmationParametersJson;
    }

    private AgentModelParamProposal findAgentProposal(Long scenarioId, Long modelParamProposalId) {
        return agentModelParamProposalRepository
            .findByModelParamProposalIdAndScenarioIdAndIsDeletedFalse(modelParamProposalId, scenarioId)
            .orElseThrow(() -> AppErrorException.of(
                ResultCode.Error.RSC_NOT_FOUND,
                "Agent model parameter proposal not found. scenarioId: %d, modelParamProposalId: %d"
                    .formatted(scenarioId, modelParamProposalId)
            ));
    }

    /**
     * 제안 스냅샷이 실행 대상 시나리오의 것인지 대조한다.
     * scenarioId + scenarioGroupId 로 결속이 고정된다. 과거의 eventId 대조는 그룹의 직접 이벤트 컬럼 제거
     * (멀티 이벤트, migration 0023)로 삭제 — 다중 이벤트 그룹은 proposal.mi_event_id 가 null 이기도 하다.
     */
    private void validateAgentProposalSnapshot(MiScenario scenario, AgentModelParamProposal agentProposal) {
        Long scenarioGroupId = scenario.getScenarioGroup().getScenarioGroupId();
        if (!Objects.equals(agentProposal.getScenarioId(), scenario.getScenarioId())
            || !Objects.equals(agentProposal.getScenarioGroupId(), scenarioGroupId)) {
            throw AppHttpStatusException.of(
                HttpStatus.CONFLICT,
                ResultCode.Error.BIZ_RULE_VIOLATION,
                "Agent proposal snapshot does not match the scenario. modelParamProposalId: %d"
                    .formatted(agentProposal.getModelParamProposalId())
            );
        }
    }

    private void validateReusablePendingPreparation(
        ModelExecutionPreparation preparation,
        Long modelParamProposalId
    ) {
        if (preparation.getStatus() != ModelExecutionPreparationStatus.PROPOSAL_PENDING
            || preparation.getCaseId() != null
            || !Objects.equals(preparation.getAgentProposalId(), modelParamProposalId)) {
            throw activePreparationConflict(preparation.getScenarioId());
        }
    }

    private void validateRepeatExecutionSource(
        ModelExecutionPreparation preparation,
        Long scenarioId
    ) {
        if (preparation.getStatus() != ModelExecutionPreparationStatus.EXECUTING
            && preparation.getStatus() != ModelExecutionPreparationStatus.EXECUTION_FAILED) {
            throw AppHttpStatusException.of(
                HttpStatus.CONFLICT,
                ResultCode.Error.BIZ_RULE_VIOLATION,
                "model parameter proposal cannot be reused from current preparation status. scenarioId: %d"
                    .formatted(scenarioId)
            );
        }
    }

    private AppErrorException proposalPreparationNotFound(Long scenarioId, Long modelParamProposalId) {
        return AppErrorException.of(
            ResultCode.Error.RSC_NOT_FOUND,
            "model parameter proposal preparation not found. scenarioId: %d, modelParamProposalId: %d"
                .formatted(scenarioId, modelParamProposalId)
        );
    }

    private AppHttpStatusException activePreparationConflict(Long scenarioId) {
        return AppHttpStatusException.of(
            HttpStatus.CONFLICT,
            ResultCode.Error.RSC_ALREADY_EXISTS,
            "해당 scenario에는 이미 진행 중인 model execution preparation이 존재합니다. scenarioId: %d"
                .formatted(scenarioId)
        );
    }

    private String preparationFailureMessage(RuntimeException failure) {
        if (failure instanceof AppErrorException) {
            return truncatePreparationError(failure.getMessage());
        }
        return "JSON model execution preparation failed.";
    }

    private String truncatePreparationError(String errorMessage) {
        if (errorMessage == null || errorMessage.length() <= PREPARATION_ERROR_MAX_LENGTH) {
            return errorMessage;
        }
        return errorMessage.substring(0, PREPARATION_ERROR_MAX_LENGTH);
    }

    public record ExecutionConfirmationContext(Long execPrepId, boolean confirmationRequired) {
    }
}
