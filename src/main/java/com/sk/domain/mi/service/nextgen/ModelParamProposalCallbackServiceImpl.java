package com.sk.domain.mi.service.nextgen;

import com.sk.common.enums.ResultCode;
import com.sk.common.exception.AppErrorException;
import com.sk.common.exception.AppHttpStatusException;
import com.sk.domain.mi.dto.request.nextgen.ModelParamProposalCallbackReq;
import com.sk.domain.mi.dto.response.nextgen.ModelParamProposalSseEvent;
import com.sk.domain.mi.service.nextgen.sse.SseEventPublisher;
import com.sk.domain.shared.entity.master.tmd.ModelExecutionPreparation;
import com.sk.domain.shared.entity.master.tmm.AgentModelParamProposal;
import com.sk.domain.shared.enums.nextgen.AgentCallbackResult;
import com.sk.domain.shared.enums.nextgen.AgentModelParamProposalStatus;
import com.sk.domain.shared.enums.nextgen.ModelExecutionPreparationStatus;
import com.sk.domain.shared.repository.master.tmd.ModelExecutionPreparationRepository;
import com.sk.domain.shared.repository.master.tmm.AgentModelParamProposalRepository;
import com.sk.domain.shared.service.permissions.AuthorizationService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ModelParamProposalCallbackServiceImpl implements ModelParamProposalCallbackService {

    private static final List<ModelExecutionPreparationStatus> CALLBACK_CANDIDATE_STATUSES = List.of(
        ModelExecutionPreparationStatus.REQUESTING,
        ModelExecutionPreparationStatus.PROPOSAL_PENDING
    );

    private final ModelExecutionPreparationRepository modelExecutionPreparationRepository;
    private final AgentModelParamProposalRepository agentModelParamProposalRepository;
    private final AuthorizationService authorizationService;
    private final SseEventPublisher sseEventPublisher;

    @Override
    @Transactional
    public void handleCallback(Long scenarioId, ModelParamProposalCallbackReq request) {
        validateEnvelope(scenarioId, request);

        Long callbackProposalId = request.data() == null ? null : request.data().modelParamProposalId();
        List<ModelExecutionPreparation> candidates =
            modelExecutionPreparationRepository.findCallbackCandidates(scenarioId, CALLBACK_CANDIDATE_STATUSES);

        if (candidates.isEmpty()) {
            log.info("No in-flight model parameter proposal preparation for callback. scenarioId={}, modelParamProposalId={}",
                scenarioId, callbackProposalId);
            return;
        }
        if (candidates.size() > 1) {
            throw AppHttpStatusException.of(
                HttpStatus.CONFLICT,
                ResultCode.Error.BIZ_RULE_VIOLATION,
                "multiple in-flight model parameter proposal preparations found. scenarioId: %d".formatted(scenarioId)
            );
        }

        ModelExecutionPreparation preparation = candidates.get(0);
        Long modelParamProposalId = callbackProposalId != null ? callbackProposalId : preparation.getAgentProposalId();
        AgentModelParamProposalStatus signalStatus = resolveSignalStatus(request);

        if (modelParamProposalId == null) {
            handleCallbackWithoutProposalId(preparation, signalStatus, request.message());
            return;
        }

        AgentModelParamProposal proposal = agentModelParamProposalRepository
            .findByModelParamProposalIdAndScenarioIdAndIsDeletedFalse(modelParamProposalId, scenarioId)
            .orElseThrow(() -> AppErrorException.of(
                ResultCode.Error.RSC_NOT_FOUND,
                "Agent model parameter proposal not found for callback. scenarioId: %d, modelParamProposalId: %d"
                    .formatted(scenarioId, modelParamProposalId)
            ));

        validateCallbackSnapshot(request, proposal);
        attachAgentProposalIdIfNecessary(preparation, modelParamProposalId);
        applyCanonicalStatus(preparation.getExecPrepId(), scenarioId, modelParamProposalId, proposal.getStatus(), request.message());
    }

    private void validateEnvelope(Long pathScenarioId, ModelParamProposalCallbackReq request) {
        if (request == null || request.status() == null) {
            throw AppErrorException.of(ResultCode.Error.REQ_INVALID_PARAMETER, "callback status is required.");
        }
        if (request.message() == null || request.message().isBlank()) {
            throw AppErrorException.of(ResultCode.Error.REQ_INVALID_PARAMETER, "callback message is required.");
        }
        ModelParamProposalCallbackReq.Data data = request.data();
        if (data == null) {
            if (request.status().isSuccess()) {
                throw AppErrorException.of(ResultCode.Error.REQ_INVALID_PARAMETER, "callback data is required for success.");
            }
            return;
        }
        if (data.scenarioId() != null && !pathScenarioId.equals(data.scenarioId())) {
            throw AppHttpStatusException.of(
                HttpStatus.CONFLICT,
                ResultCode.Error.BIZ_RULE_VIOLATION,
                "callback scenarioId does not match path. pathScenarioId: %d".formatted(pathScenarioId)
            );
        }
        if (data.status() != null
            && data.status() != AgentModelParamProposalStatus.PROPOSED
            && data.status() != AgentModelParamProposalStatus.FAILED) {
            throw AppErrorException.of(ResultCode.Error.REQ_INVALID_PARAMETER, "callback data.status must be PROPOSED or FAILED.");
        }
        if (data.status() == AgentModelParamProposalStatus.PROPOSED && request.status() != AgentCallbackResult.SUCCESS) {
            throw AppErrorException.of(ResultCode.Error.REQ_INVALID_PARAMETER, "PROPOSED callback must use success status.");
        }
        if (data.status() == AgentModelParamProposalStatus.FAILED && request.status() != AgentCallbackResult.FAILURE) {
            throw AppErrorException.of(ResultCode.Error.REQ_INVALID_PARAMETER, "FAILED callback must use error/failure status.");
        }
    }

    private AgentModelParamProposalStatus resolveSignalStatus(ModelParamProposalCallbackReq request) {
        if (request.data() != null && request.data().status() != null) {
            return request.data().status();
        }
        return request.status().isSuccess()
            ? AgentModelParamProposalStatus.PROPOSED
            : AgentModelParamProposalStatus.FAILED;
    }

    private void handleCallbackWithoutProposalId(
        ModelExecutionPreparation preparation,
        AgentModelParamProposalStatus signalStatus,
        String message
    ) {
        if (signalStatus != AgentModelParamProposalStatus.FAILED) {
            throw AppErrorException.of(ResultCode.Error.REQ_INVALID_PARAMETER, "modelParamProposalId is required for success callback.");
        }

        int updated = transition(
            preparation.getExecPrepId(),
            ModelExecutionPreparationStatus.REQUEST_FAILED,
            message
        );
        if (updated > 0) {
            publishAfterCommit(ModelParamProposalSseEvent.of(
                preparation.getScenarioId(),
                null,
                AgentModelParamProposalStatus.FAILED.name()
            ));
        }
    }

    private void validateCallbackSnapshot(ModelParamProposalCallbackReq request, AgentModelParamProposal proposal) {
        ModelParamProposalCallbackReq.Data data = request.data();
        if (data == null) {
            return;
        }
        if (data.scenarioGroupId() != null && !Objects.equals(data.scenarioGroupId(), proposal.getScenarioGroupId())) {
            throw AppHttpStatusException.of(
                HttpStatus.CONFLICT,
                ResultCode.Error.BIZ_RULE_VIOLATION,
                "callback scenarioGroupId does not match Agent proposal."
            );
        }
        if (data.eventId() != null && !Objects.equals(data.eventId(), proposal.getEventId())) {
            throw AppHttpStatusException.of(
                HttpStatus.CONFLICT,
                ResultCode.Error.BIZ_RULE_VIOLATION,
                "callback eventId does not match Agent proposal."
            );
        }
    }

    private void attachAgentProposalIdIfNecessary(ModelExecutionPreparation preparation, Long modelParamProposalId) {
        int writeCount = modelExecutionPreparationRepository.writeAgentProposalIdIfAbsent(
            preparation.getExecPrepId(),
            modelParamProposalId,
            authorizationService.getSubject(),
            LocalDateTime.now()
        );
        if (writeCount > 0) {
            return;
        }

        ModelExecutionPreparation current = modelExecutionPreparationRepository.findById(preparation.getExecPrepId())
            .orElseThrow(() -> AppErrorException.of(
                ResultCode.Error.RSC_NOT_FOUND,
                "model execution preparation not found after Agent proposal correlation update. execPrepId: %d"
                    .formatted(preparation.getExecPrepId())
            ));
        if (!modelParamProposalId.equals(current.getAgentProposalId())) {
            throw AppHttpStatusException.of(
                HttpStatus.CONFLICT,
                ResultCode.Error.BIZ_RULE_VIOLATION,
                "Agent proposal correlation conflict. execPrepId: %d".formatted(preparation.getExecPrepId())
            );
        }
    }

    private void applyCanonicalStatus(
        Long execPrepId,
        Long scenarioId,
        Long modelParamProposalId,
        AgentModelParamProposalStatus agentStatus,
        String message
    ) {
        switch (agentStatus) {
            case PROPOSED -> {
                int updated = transition(execPrepId, ModelExecutionPreparationStatus.PROPOSAL_PENDING, null);
                if (updated > 0) {
                    publishAfterCommit(ModelParamProposalSseEvent.of(scenarioId, modelParamProposalId, agentStatus.name()));
                }
            }
            case FAILED -> {
                int updated = transition(execPrepId, ModelExecutionPreparationStatus.REQUEST_FAILED, message);
                if (updated > 0) {
                    publishAfterCommit(ModelParamProposalSseEvent.of(scenarioId, modelParamProposalId, agentStatus.name()));
                }
            }
            case PROCESSING -> transition(execPrepId, ModelExecutionPreparationStatus.PROPOSAL_PENDING, null);
            case CONFIRMED -> log.info(
                "Skip model parameter proposal callback for post-proposal Agent status. execPrepId={}, modelParamProposalId={}, status={}",
                execPrepId, modelParamProposalId, agentStatus
            );
        }
    }

    private int transition(Long execPrepId, ModelExecutionPreparationStatus nextStatus, String errorMessage) {
        return modelExecutionPreparationRepository.transitionStatus(
            execPrepId,
            CALLBACK_CANDIDATE_STATUSES,
            nextStatus,
            truncateErrorMessage(errorMessage),
            authorizationService.getSubject(),
            LocalDateTime.now()
        );
    }

    private void publishAfterCommit(ModelParamProposalSseEvent event) {
        Runnable publish = () -> sseEventPublisher.publish(event);
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    publish.run();
                }
            });
            return;
        }
        publish.run();
    }

    private String truncateErrorMessage(String message) {
        if (message == null || message.length() <= 1000) {
            return message;
        }
        return message.substring(0, 1000);
    }
}
