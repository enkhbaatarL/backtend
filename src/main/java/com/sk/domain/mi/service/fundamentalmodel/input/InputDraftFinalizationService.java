package com.sk.domain.mi.service.fundamentalmodel.input;

import com.sk.domain.shared.repository.master.tmm.ModelInputDraftRepository;
import com.sk.domain.shared.repository.master.tmm.ModelInputDraftRepository.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

/** A의 공통 접수/격리 worker에 명시적으로 연결한다. 웹 bean/queue/자동 재실행을 등록하지 않는다. */
final class InputDraftFinalizationService {
    static final List<String> ROLES = List.of("SUPPLY", "DEMAND", "TRADE", "OUTPUT");
    static final String MIME = "application/vnd.ms-excel.sheet.macroEnabled.12";

    record Request(String requestId, long baseRevision, String title, String description) {
        Request {
            uuid(requestId);
            if (baseRevision < 0 || baseRevision > 9007199254740991L || title == null) {
                throw InputDraftException.invalid();
            }
            title = title.trim();
            if (title.isEmpty() || title.length() > 200
                    || (description != null && description.getBytes(StandardCharsets.UTF_8).length > 65535)) {
                throw InputDraftException.invalid();
            }
        }
    }

    record Job(Draft draft, Operation operation, WorkerOwner owner, List<FileRef> files,
            List<Revision> revisions, InputDraftEditingBasis basis) {
        Job(Draft draft,Operation operation,WorkerOwner owner,List<FileRef> files,List<Revision> revisions) {
            this(draft,operation,owner,files,revisions,null);
        }
    }
    record SavedFile(String role, long fileNo) {}
    record Result(long finalizedCaseId, List<SavedFile> files, String executionStatus,
            String executionRunId, String executionFailureReason) {
        Result(long finalizedCaseId, List<SavedFile> files) {
            this(finalizedCaseId, files, "QUEUED", null, null);
        }
    }

    /** A가 기존 source/fixed/restore 경계로 원본 접근·sourceVersion·고정 발행을 재검사한다. */
    interface BasisVerifier {
        ModelInputDraftService.Source verify(Draft draft, Operation operation);
    }

    /** A supervisor의 동일 operation/token 종료 증거. 이전 JVM과 전송이 모두 종료된 뒤만 true다. */
    interface StoppedAttempt {
        boolean confirmed(String operationId, String token);
    }

    /** 전용 private 참조만 사용한다. 구현은 저장소 정책/암복호화 primitive를 재사용한다. */
    interface ObjectsStore {
        Reference plan(WorkerOwner owner, Operation operation, String role);
        default Reference plan(WorkerOwner owner,Operation operation,String role,String extension) {
            Reference original=plan(owner,operation,role);
            if(extension.equals("xlsm"))return original;
            if(!extension.equals("xlsx"))throw InputDraftException.internal();
            String key=original.key().replaceAll("\\.xlsm$",".xlsx");
            return new Reference(original.bucket(),key,original.location().replaceAll("\\.xlsm$",".xlsx"),role+".xlsx",InputUploadValidation.XLSX,original.company(),null,null,null);
        }
        void upload(Reference ref, Path generated) throws Exception;
        void verifyReadable(Reference ref) throws Exception;
        void delete(Reference ref) throws Exception;
        boolean absent(Reference ref) throws Exception;
    }

    record Reference(String bucket, String key, String location, String name, String mime,
            String company, Long size, String sha256, String absentSince) {
        Reference content(long bytes, String hash) {
            return new Reference(bucket, key, location, name, mime, company, bytes, hash, null);
        }
        Reference observed(String when) {
            return new Reference(bucket, key, location, name, mime, company, size, sha256, when);
        }
        void validate(Operation op, String role, WorkerOwner owner) {
            String extension=(role+".xlsx").equals(name)?"xlsx":"xlsm";
            String expected = "SCNRCASE/INPUTDRAFT/" + uuid(op.id()) + "/" + uuid(op.token()) + "/" + role + "."+extension;
            if (bucket == null || bucket.isBlank() || !expected.equals(key) || !Objects.equals(company, owner.companyCode())
                    || !(role + "."+extension).equals(name) || !(extension.equals("xlsx")?InputUploadValidation.XLSX:MIME).equals(mime) || location == null || location.length() > 255
                    || !location.endsWith("/" + bucket + "/" + key) || absentSince != null
                    || size != null || sha256 != null) {
                throw InputDraftException.internal();
            }
        }
    }

    private final InputDraftFinalizationRepository store;
    private final InputDraftFinalizationFiles files;
    private final ObjectsStore objects;
    private final TransactionTemplate transaction;

    InputDraftFinalizationService(InputDraftFinalizationRepository store, InputDraftFinalizationFiles files,
            ObjectsStore objects, PlatformTransactionManager masterManager) {
        this.store = Objects.requireNonNull(store);
        this.files = Objects.requireNonNull(files);
        this.objects = Objects.requireNonNull(objects);
        transaction = new TransactionTemplate(masterManager);
        transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        transaction.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
    }

    /** HTTP 인증/소유권 확인 후 호출한다. CHANGE와 같은 요청 이력/슬롯을 사용한다. */
    Operation accept(String draft, int company, int user, String actor, String role, Request request) {
        return tx(() -> store.accept(draft, company, user, actor, role, request));
    }

    /** A가 fsync한 marker의 token을 전달한다. 이미 RUNNING인 요청을 다시 claim하지 않는다. */
    Job claim(String draft, String operation, String token) {
        uuid(token);
        Operation claimed = tx(() -> store.claim(draft, operation, token));
        // 원본/권한 검증 실패도 durable RUNNING/token으로 복구한다. claim을 함께 rollback하지 않는다.
        return claimed == null ? null : tx(() -> store.prepareClaimed(draft, operation, token));
    }

    /** 같은 격리 worker에서만 호출. 파일 IO 중 transaction/row lock을 유지하지 않는다. */
    Result execute(Job job) throws Exception {
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            throw new IllegalStateException("finalization file IO requires no transaction");
        }
        List<Reference> refs = ROLES.stream().map(role -> {
            String name=job.files().stream().filter(f->f.role().equals(role)).findFirst().orElseThrow(InputDraftException::internal).name();
            Reference ref = objects.plan(job.owner(), job.operation(), role,name.toLowerCase(java.util.Locale.ROOT).endsWith(".xlsx")?"xlsx":"xlsm");
            ref.validate(job.operation(), role, job.owner());
            return ref;
        }).toList();
        tx(() -> { store.prepare(job, refs); return null; });
        // 원본4역할을 순차 처리한다. 임시 workbook/화면 snapshot을 승격하지 않는다.
        for (int index = 0; index < ROLES.size(); index++) {
            InputTempFiles.checkpoint();
            String role = ROLES.get(index);
            Reference planned = refs.get(index);
            files.withGenerated(job, role, generated -> {
                Reference ready = planned.content(java.nio.file.Files.size(generated), InputDraftFinalizationFiles.digest(generated));
                tx(() -> { store.content(job, role, ready); return null; }); // PUT보다 먼저 durable 기록
                objects.upload(ready, generated);
                InputTempFiles.checkpoint();
                objects.verifyReadable(ready);
                tx(() -> { store.uploaded(job, role); return null; });
                return null;
            });
        }
        // 예외/응답 유실은 여기서 object 삭제나 FAILED 추정으로 바꾸지 않는다.
        // A가 worker/전송 종료를 확인한 뒤 recoverStopped에서 같은 쓰기 DB를 잠가 확정한다.
        InputTempFiles.checkpoint();
        return tx(() -> store.commit(job));
    }

    /** 공통 HTTP 복구의 이미 열린 같은 master transaction/잠금에 참여한다. */
    Operation expireQueuedJoined(String draft,String operation) { return store.expireQueued(draft,operation); }

    boolean cleanupComplete(String draft,String operation,String token,StoppedAttempt stopped) {
        requireStopped(operation,token,stopped);
        return tx(()->store.cleanupCandidates(draft,operation,token).isEmpty());
    }

    Operation expireQueued(String draft, String operation) {
        return tx(() -> store.expireQueued(draft, operation));
    }

    Operation failStopped(String draft, String operation, String token, String reason, StoppedAttempt stopped) {
        requireStopped(operation, token, stopped);
        return tx(() -> store.failStopped(draft, operation, token, reason));
    }

    Operation recoverStopped(String draft, String operation, String token, StoppedAttempt stopped) {
        requireStopped(operation, token, stopped);
        return tx(() -> store.recoverStopped(draft, operation, token));
    }

    /** 반복 호출 가능한 운영 정리 경계. 스케줄/자동 purge는 등록하지 않는다. */
    void cleanup(String draft, String operation, String token, StoppedAttempt stopped) throws Exception {
        requireStopped(operation, token, stopped);
        for (var pending : tx(() -> store.cleanupCandidates(draft, operation, token))) {
            Reference ref = pending.ref();
            if (!objects.absent(ref)) {
                objects.delete(ref);
                tx(() -> { store.observeAbsent(draft, operation, token, pending.role(), false); return null; });
            } else {
                // 삭제 호출과 분리된 두 번의 실제 읽기 부재 확인을 영속 기록한다.
                tx(() -> { store.observeAbsent(draft, operation, token, pending.role(), true); return null; });
            }
        }
    }

    private static void requireStopped(String operation, String token, StoppedAttempt stopped) {
        uuid(operation); uuid(token);
        if (stopped == null || !stopped.confirmed(operation, token)) {
            throw new IllegalStateException("worker and transfers must be confirmed stopped");
        }
    }

    private <T> T tx(Supplier<T> work) {
        return transaction.execute(ignored -> work.get());
    }

    static String uuid(String value) {
        try {
            if (value == null || !UUID.fromString(value).toString().equals(value)) throw InputDraftException.invalid();
            return value;
        } catch (IllegalArgumentException ex) { throw InputDraftException.invalid(); }
    }
}
