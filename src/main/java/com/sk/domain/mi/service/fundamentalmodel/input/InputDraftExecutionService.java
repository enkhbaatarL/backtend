package com.sk.domain.mi.service.fundamentalmodel.input;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sk.domain.mi.service.fundamentalmodel.FundamentalModelCaseServiceImpl;
import com.sk.domain.shared.repository.master.tmm.ModelInputDraftRepository;
import com.sk.domain.shared.repository.master.tmm.ModelInputDraftRepository.Operation;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

/** 웹 BE의 실행 접수기. 격리 파일 worker에는 등록하지 않으며 영속 저장 성공 이후에만 동작한다. */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "mi.model-input.finalization.enabled", havingValue = "true")
public class InputDraftExecutionService {
    private final ModelInputDraftRepository store;
    private final InputDraftJson json;
    private final FundamentalModelCaseServiceImpl cases;
    private final PlatformTransactionManager transactionManager;

    @Scheduled(fixedDelay = 2_000)
    public void dispatchPending() {
        for (Operation queued : store.queuedFinalizationExecutions()) {
            dispatch(queued);
        }
    }

    void dispatch(Operation queued) {
        try {
            transaction().executeWithoutResult(ignored -> {
                var draft = store.lockForRecovery(queued.draftId());
                Operation current = store.operation(queued.draftId(), queued.id(), true);
                ObjectNode result = queuedResult(current);
                if (result == null) return;
                if (draft.finalizedCase() == null || result.path("finalizedCaseId").asLong() != draft.finalizedCase()) {
                    throw InputDraftException.internal();
                }
                var owner = store.workerOwner(draft, current);
                var target = store.lockFinalizationCase(draft);
                String runId = target.runId();
                if (runId == null) {
                    // 이 메서드는 같은 master transaction에 참여한다. 실행 이력과 아래 연결은 함께 commit된다.
                    runId = cases.runFinalizedDraftCase(target.caseId(), owner.user(), owner.login()).runId();
                } else {
                    // 수동 실행이 먼저 접수됐다면 완료/실패 여부와 무관하게 그 실행을 연결한다.
                    store.finalizationRun(target.caseId(), runId);
                }
                result.put("executionStatus", "RUNNING");
                result.put("executionRunId", runId);
                result.putNull("executionFailureReason");
                complete(current, result);
            });
        } catch (RuntimeException failure) {
            // 응답/commit 결과가 불명확해도 같은 쓰기 DB를 다시 잠가 QUEUED인 경우에만 실패로 확정한다.
            log.warn("Input Draft execution acceptance failed. operationId={}, failureType={}",
                queued.id(), failure.getClass().getSimpleName());
            try {
                transaction().executeWithoutResult(ignored -> {
                    store.lockForRecovery(queued.draftId());
                    Operation current = store.operation(queued.draftId(), queued.id(), true);
                    ObjectNode result = queuedResult(current);
                    if (result == null) return;
                    result.put("executionStatus", "FAILED");
                    result.putNull("executionRunId");
                    result.put("executionFailureReason", "EXECUTION_REQUEST_FAILED");
                    complete(current, result);
                });
            } catch (RuntimeException unavailable) {
                // DB 장애 중에는 실패로 추정하지 않는다. 남은 영속 대기는 다음 cycle/재시작에서 확인한다.
                log.error("Input Draft execution outcome unavailable. operationId={}", queued.id());
            }
        }
    }

    private ObjectNode queuedResult(Operation operation) {
        if (!operation.type().equals("FINALIZE") || !operation.status().equals("SUCCEEDED")) return null;
        JsonNode result = json.payload(operation.result());
        if (!"QUEUED".equals(result.path("executionStatus").asText())) return null;
        if (!(result instanceof ObjectNode object) || !result.path("finalizedCaseId").isIntegralNumber()
            || !result.path("finalizedCaseId").canConvertToLong() || result.path("finalizedCaseId").asLong() <= 0) {
            throw InputDraftException.internal();
        }
        return object;
    }

    private void complete(Operation operation, ObjectNode result) {
        store.completeFinalizationExecution(operation, new String(json.bytes(result), StandardCharsets.UTF_8));
    }

    private TransactionTemplate transaction() {
        var tx = new TransactionTemplate(transactionManager);
        tx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        tx.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
        return tx;
    }
}
