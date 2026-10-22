package com.sk.domain.mi.service.fundamentalmodel.input;

import com.sk.domain.mi.dto.response.fundamentalmodel.ModelInputDraftResponses.Problem;
import com.sk.domain.shared.repository.master.tmm.ModelInputDraftRepository;
import com.sk.domain.shared.repository.master.tmm.ModelInputDraftRepository.Operation;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

/** 실행 위치만 바꾼다. 기존 접수/권한/claim/완료/종료 확인 후 복구의 계약을 재사용한다. */
final class InputDraftWebTasks {
    private final ModelInputDraftRepository common;
    private final ModelInputDraftService drafts;
    private final InputUploadService uploads;
    private final InputDraftFinalizationService finalizations;
    private final InputWorkbookReader reader;
    private final InputTempFiles scratch;
    private final InputDraftJson json;
    private final TransactionTemplate transaction;
    private final boolean changesEnabled;
    private final boolean finalizationEnabled;

    InputDraftWebTasks(ModelInputDraftRepository common, ModelInputDraftService drafts, InputUploadService uploads,
            InputDraftFinalizationService finalizations, InputWorkbookReader reader, InputTempFiles scratch,
            InputDraftJson json, PlatformTransactionManager manager, boolean changesEnabled, boolean finalizationEnabled) {
        this.common = common; this.drafts = drafts; this.uploads = uploads; this.finalizations = finalizations;
        this.reader = reader; this.scratch = scratch; this.json = json;
        this.changesEnabled = changesEnabled; this.finalizationEnabled = finalizationEnabled;
        transaction = new TransactionTemplate(manager);
        transaction.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
    }

    LocalDateTime databaseTime() { return common.databaseTime(); }
    String enabledOperations() {
        return "UPLOAD" + (changesEnabled ? ",CHANGE" : "") + (finalizationEnabled ? ",FINALIZE" : "");
    }
    Operation operation(String operation) { return common.workerOperation(operation); }
    List<Operation> stoppedOperations() { return common.stoppedWorkerOperations(); }

    Operation next() {
        for (var upload : uploads.pending()) if (upload.status().equals("QUEUED")) return operation(upload.id());
        return common.queuedOperations(finalizationEnabled).stream()
                .filter(op -> !op.type().equals("CHANGE") || changesEnabled).findFirst().orElse(null);
    }

    void process(Operation queued, String token) throws Exception {
        InputTempFiles.checkpoint();
        if (!databaseTime().isBefore(queued.expires())) { expire(queued); return; }
        switch (queued.type()) {
            case "UPLOAD_CREATE", "UPLOAD_FILE" -> uploads.process(queued.id(), token);
            case "FINALIZE" -> {
                var job = finalizations.claim(queued.draftId(), queued.id(), token);
                if (job != null) finalizations.execute(job);
            }
            case "CHANGE" -> {
                var claimed = transaction.execute(ignored -> common.claimChange(queued.draftId(), queued.id(), token));
                if (claimed == null) return;
                var job = drafts.prepareWorkerChange(claimed.draftId(), claimed.id(), token);
                if (job == null) return;
                var delta = job.basis() == null ? InputDraftPreserveChange.execute(job, reader, scratch, json)
                        : new InputWorkbookEditor(reader, scratch, json, InputTempFiles::checkpoint)
                                .execute(job.basis(), job.company(), job.request());
                InputTempFiles.checkpoint();
                drafts.completeWorkerChange(claimed.draftId(), claimed.id(), token, delta);
            }
            default -> throw new IllegalStateException("unsupported runner operation");
        }
    }

    /** 본문/전송/finally 종료 뒤만 호출한다. DB 응답 유실은 재조회하며 성공을 실패로 덮지 않는다. */
    boolean settle(String operation, String token, String reason) throws Exception {
        return settle(operation, token, reason, List.of());
    }

    boolean settle(String operation, String token, String reason, List<Object> issues) throws Exception {
        Operation observed = operation(operation);
        if (observed.status().equals("QUEUED")) expire(observed);
        Operation current = observed.status().equals("QUEUED") ? operation(operation) : observed;
        if (current.status().equals("QUEUED") && current.token() == null) return true;
        // 요청 ck07: FAILED + token NULL은 시작 시각도 NULL인 미실행 종료다.
        // 실행기 bind의 예상 token은 실제 claim token으로 오인하지 않는다.
        if (current.status().equals("FAILED") && current.token() == null) {
            return !current.type().startsWith("UPLOAD") || cleanupUploads();
        }
        if (!token.equals(current.token())) throw new IllegalStateException("operation attempt changed");
        if (current.status().equals("SUCCEEDED")) return true;
        var stopped = (InputDraftFinalizationService.StoppedAttempt) (op, attempt) -> operation.equals(op) && token.equals(attempt);
        if (current.status().equals("RUNNING")) {
            String safe = safeReason(reason);
            switch (current.type()) {
                case "UPLOAD_CREATE", "UPLOAD_FILE" -> uploads.failStopped(operation, token, safe);
                case "FINALIZE" -> finalizations.failStopped(current.draftId(), operation, token,
                        safe.equals("REVISION_CONFLICT") ? "PERSISTENCE_FAILED" : safe, stopped);
                case "CHANGE" -> transaction.execute(ignored -> {
                    var draft = common.lockForRecovery(current.draftId());
                    var op = common.expireChange(current.draftId(), operation, failure("TIMEOUT", draft.revision()));
                    if (!op.terminal()) common.failStoppedChange(current.draftId(), operation, token, failure(safe, draft.revision(), issues));
                    return null;
                });
                default -> throw new IllegalStateException("unsupported runner recovery");
            }
        }
        // 실패 확정의 실제 쓰기 DB 결과를 확인한 뒤에만 삭제한다.
        Operation saved = operation(operation);
        if (saved.status().equals("SUCCEEDED")) return true;
        if (!saved.status().equals("FAILED") || !token.equals(saved.token())) throw new IllegalStateException("operation result unconfirmed");
        if (saved.type().equals("FINALIZE")) {
            finalizations.cleanup(saved.draftId(), operation, token, stopped);
            return finalizations.cleanupComplete(saved.draftId(), operation, token, stopped);
        }
        if (saved.type().startsWith("UPLOAD")) return cleanupUploads();
        return true;
    }

    boolean cleanupUploads() {
        uploads.cleanupCandidates();
        return uploads.cleanupComplete();
    }

    private void expire(Operation queued) {
        if (queued.type().startsWith("UPLOAD")) uploads.expireQueued(queued.id());
        else if (queued.type().equals("FINALIZE")) finalizations.expireQueued(queued.draftId(), queued.id());
        else transaction.execute(ignored -> common.expireChange(queued.draftId(), queued.id(), failure("TIMEOUT", queued.baseRevision())));
    }

    private String failure(String reason, long revision) { return failure(reason, revision, List.of()); }
    private String failure(String reason, long revision, List<Object> issues) {
        return new String(json.bytes(new Problem(reason, revision, issues)), StandardCharsets.UTF_8);
    }

    static String safeReason(String reason) {
        return reason != null && Set.of("VALIDATION_FAILED", "CALCULATION_FAILED", "PRESERVATION_FAILED", "STRUCTURE_VALIDATION_FAILED",
                "SOURCE_CHANGED", "POLICY_INCOMPLETE", "POLICY_UNSUPPORTED", "REVISION_CONFLICT", "TIMEOUT").contains(reason)
                ? reason : "PERSISTENCE_FAILED";
    }
}
