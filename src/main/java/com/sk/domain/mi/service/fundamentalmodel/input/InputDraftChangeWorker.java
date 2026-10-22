package com.sk.domain.mi.service.fundamentalmodel.input;

import com.fasterxml.jackson.databind.JsonNode;
import com.sk.domain.mi.dto.response.fundamentalmodel.ModelInputDraftResponses.Problem;
import com.sk.domain.shared.repository.master.tmm.ModelInputDraftRepository;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.PosixFilePermissions;
import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/** 전용 worker JVM에서만 생성한다. 웹 scheduler/메모리 queue를 사용하지 않는다. */
final class InputDraftChangeWorker implements AutoCloseable {
    private static final String UUID_PATTERN="[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}";
    private static final Pattern PENDING_MARKER=Pattern.compile("\\.pending-"+UUID_PATTERN+"-"+UUID_PATTERN+"\\.json");
    private final ModelInputDraftRepository store;
    private final ModelInputDraftService service;
    private final InputWorkbookReader reader;
    private final InputTempFiles scratch;
    private final InputDraftJson json;
    private final TransactionTemplate tx;
    private final Path markers;
    private final ScheduledExecutorService watchdog=Executors.newSingleThreadScheduledExecutor(task->{
        Thread thread=new Thread(task,"input-draft-deadline");thread.setDaemon(true);return thread;
    });
    private final Runnable terminate;
    private final InputDraftFinalizationService finalizations;
    private final boolean finalizationEnabled;
    private InputUploadService uploads;
    void uploads(InputUploadService uploads){this.uploads=uploads;}

    InputDraftChangeWorker(ModelInputDraftRepository store,ModelInputDraftService service,InputWorkbookReader reader,
            InputTempFiles scratch,InputDraftJson json,PlatformTransactionManager manager,Path root,Runnable terminate)throws Exception {
        this(store,service,reader,scratch,json,manager,root,terminate,null,false);
    }
    InputDraftChangeWorker(ModelInputDraftRepository store,ModelInputDraftService service,InputWorkbookReader reader,
            InputTempFiles scratch,InputDraftJson json,PlatformTransactionManager manager,Path root,Runnable terminate,
            InputDraftFinalizationService finalizations,boolean finalizationEnabled)throws Exception {
        this.finalizations=finalizations;this.finalizationEnabled=finalizationEnabled;
        if(finalizationEnabled&&finalizations==null)throw new IllegalStateException("finalization wiring required");
        this.store=store;this.service=service;this.reader=reader;this.scratch=scratch;this.json=json;this.terminate=terminate;
        tx=new TransactionTemplate(manager);tx.setIsolationLevel(org.springframework.transaction.TransactionDefinition.ISOLATION_READ_COMMITTED);
        markers=root.resolve("markers");
        if(!Files.exists(markers))Files.createDirectory(markers,PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rwx------")));
        if(!Files.isDirectory(markers,LinkOption.NOFOLLOW_LINKS)||Files.isSymbolicLink(markers))throw new IllegalStateException("worker markers");
    }

    /** supervisor가 이전 JVM 종료/평문 정리를 확인한 뒤 복구하며, 남은 object 정리는 다음 idle cycle에도 확인한다. */
    void recoverStopped()throws Exception {
        if(uploads!=null)uploads.cleanupCandidates();
        try(var files=Files.list(markers)) {
            var paths=files.toList();
            // 최종 marker 공개/claim 전의 임시 파일에는 DB 복구 권한이 없다.
            for(Path path:paths)if(PENDING_MARKER.matcher(path.getFileName().toString()).matches()) {
                if(!Files.isRegularFile(path,LinkOption.NOFOLLOW_LINKS))throw new IllegalStateException("worker marker");
                Files.delete(path);
            }
            for(Path path:paths) {
                if(PENDING_MARKER.matcher(path.getFileName().toString()).matches())continue;
                if(!Files.isRegularFile(path,LinkOption.NOFOLLOW_LINKS)||Files.size(path)>1024)throw new IllegalStateException("worker marker");
                JsonNode marker=json.payload(Files.readString(path));
                String operation=uuid(marker.path("operationId").asText()),token=uuid(marker.path("token").asText());
                if(!path.getFileName().toString().equals(operation+"-"+token+".json"))throw new IllegalStateException("worker marker");
                if("UPLOAD".equals(marker.path("draftId").asText())) {
                    if(uploads==null)throw new IllegalStateException("upload recovery unavailable");
                    if(!uploads.recoverStopped(operation,token))throw new IllegalStateException("upload recovery unconfirmed");
                    Files.delete(path);continue;
                }
                String draft=uuid(marker.path("draftId").asText());
                var current=tx.execute(ignored->store.operation(draft,operation,true));
                if(current.type().equals("FINALIZE")) {
                    if(finalizations==null)throw new IllegalStateException("finalization recovery unavailable");
                    InputDraftFinalizationService.StoppedAttempt stopped=(op,attempt)->operation.equals(op)&&token.equals(attempt);
                    if(current.status().equals("RUNNING")&&token.equals(current.token())) {
                        if(marker.has("failureReason"))finalizations.failStopped(draft,operation,token,finalReason(marker.path("failureReason").asText()),stopped);
                        else finalizations.recoverStopped(draft,operation,token,stopped);
                    } else if(current.status().equals("QUEUED"))finalizations.expireQueued(draft,operation);
                    current=tx.execute(ignored->store.operation(draft,operation,true));
                    if(current.status().equals("FAILED")&&token.equals(current.token())) {
                        finalizations.cleanup(draft,operation,token,stopped);
                        if(!finalizations.cleanupComplete(draft,operation,token,stopped))continue;
                    }
                } else {
                    service.recoverStoppedChange(draft,operation,token);
                    current=tx.execute(ignored->store.operation(draft,operation,true));
                }
                if(current.terminal()||!token.equals(current.token()))Files.delete(path);
                else throw new IllegalStateException("worker recovery unconfirmed");
            }
        }
    }

    boolean runOne()throws Exception {
        if(uploads!=null)for(var queued:uploads.pending()) {
            if(!queued.status().equals("QUEUED"))continue;
            long remaining=Duration.between(store.databaseTime(),queued.expires()).toNanos();
            if(remaining<=0){uploads.expireQueued(queued);continue;}
            String token=UUID.randomUUID().toString();Path marker=writeMarker("UPLOAD",queued.id(),token);
            remaining=Duration.between(store.databaseTime(),queued.expires()).toNanos();
            if(remaining<=0){uploads.expireQueued(queued);Files.delete(marker);continue;}
            var deadline=watchdog.schedule(terminate,remaining,TimeUnit.NANOSECONDS);boolean acknowledged=false;
            try {
                uploads.process(queued,token);
                if(scratch.plaintextRemains()||!uploads.terminal(queued.id()))throw new IllegalStateException("upload result unconfirmed");
                Files.delete(marker);acknowledged=true;return true;
            } finally {if(acknowledged)deadline.cancel(false);}
        }
        for(var queued:finalizationEnabled?store.queuedOperations(true):store.queuedChanges()) {
            long remaining=Duration.between(store.databaseTime(),queued.expires()).toNanos();
            if(remaining<=0) {
                expire(queued);
                continue;
            }
            String token=UUID.randomUUID().toString();Path marker=writeMarker(queued.draftId(),queued.id(),token);
            // marker fsync·DB 응답 시간도 접수 시각부터의 기한에 포함한다.
            long clockBegin=System.nanoTime();
            remaining=Duration.between(store.databaseTime(),queued.expires()).toNanos()-(System.nanoTime()-clockBegin);
            if(remaining<=0) {
                expire(queued);
                Files.delete(marker);continue;
            }
            var deadline=watchdog.schedule(terminate,remaining,TimeUnit.NANOSECONDS);
            boolean acknowledged=false;
            try {
                if(queued.type().equals("FINALIZE")) {
                    finalizeOne(queued,token,marker);acknowledged=true;return true;
                }
                var claimed=tx.execute(ignored->store.claimChange(queued.draftId(),queued.id(),token));
                if(claimed==null){Files.delete(marker);acknowledged=true;return true;}
                try {
                    var job=service.prepareWorkerChange(claimed.draftId(),claimed.id(),token);
                    if(job==null)fail(claimed.draftId(),claimed.id(),token,"TIMEOUT");
                    else {
                        JsonNode delta=job.basis()==null?InputDraftPreserveChange.execute(job,reader,scratch,json)
                            :new InputWorkbookEditor(reader,scratch,json,()->{if(Thread.currentThread().isInterrupted())throw new InputDraftPreserveChange.Failure("TIMEOUT");})
                                .execute(job.basis(),job.company(),job.request());
                        if(scratch.plaintextRemains())throw new IllegalStateException("worker cleanup unconfirmed");
                        service.completeWorkerChange(claimed.draftId(),claimed.id(),token,delta);
                    }
                } catch(InputDraftPreserveChange.Failure ex) {
                    if(scratch.plaintextRemains())throw new IllegalStateException("worker cleanup unconfirmed");
                    fail(claimed.draftId(),claimed.id(),token,ex.reason);
                } catch(InputDraftException ex) {
                    if(scratch.plaintextRemains())throw new IllegalStateException("worker cleanup unconfirmed");
                    fail(claimed.draftId(),claimed.id(),token,allowed(ex.getReason()),ex.getIssues());
                }
                // 예상하지 못한 DB/commit 오류는 자식 JVM을 끝내고 실제 DB+marker로 복구한다.
                var current=tx.execute(ignored->store.operation(claimed.draftId(),claimed.id(),true));
                if(!current.terminal())throw new IllegalStateException("worker result unconfirmed");
                Files.delete(marker);acknowledged=true;return true;
            } finally {if(acknowledged)deadline.cancel(false);}
        }
        return false;
    }
    private void expire(ModelInputDraftRepository.Operation queued) {
        if(queued.type().equals("FINALIZE"))finalizations.expireQueued(queued.draftId(),queued.id());
        else tx.execute(ignored->store.expireChange(queued.draftId(),queued.id(),failure("TIMEOUT",queued.baseRevision())));
    }
    private void finalizeOne(ModelInputDraftRepository.Operation queued,String token,Path marker)throws Exception {
        try {
            var job=finalizations.claim(queued.draftId(),queued.id(),token);
            if(job!=null)finalizations.execute(job);
            if(scratch.plaintextRemains())throw new IllegalStateException("worker cleanup unconfirmed");
            var current=tx.execute(ignored->store.operation(queued.draftId(),queued.id(),true));
            // claim이 commit된 뒤 prepare 기한 경과로 null이어도 RUNNING/token은 남을 수 있다.
            if(current.status().equals("RUNNING")&&token.equals(current.token()))throw new IllegalStateException("finalization attempt requires stopped recovery");
            if(current.status().equals("FAILED")&&token.equals(current.token()))throw new IllegalStateException("finalization cleanup requires stopped recovery");
            if(current.status().equals("QUEUED"))finalizations.expireQueued(queued.draftId(),queued.id());
            Files.delete(marker);
        } catch(InputDraftPreserveChange.Failure ex) {
            writeMarker(queued.draftId(),queued.id(),token,finalReason(ex.reason));
            throw new IllegalStateException("finalization stopped recovery required");
        } catch(InputDraftException ex) {
            writeMarker(queued.draftId(),queued.id(),token,finalReason(ex.getReason()));
            throw new IllegalStateException("finalization stopped recovery required");
        }
    }
    private static String finalReason(String reason) {
        String safe=allowed(reason);return safe.equals("REVISION_CONFLICT")?"PERSISTENCE_FAILED":safe;
    }
    private void fail(String draft,String operation,String token,String reason) {
        fail(draft,operation,token,reason,List.of());
    }
    private void fail(String draft,String operation,String token,String reason,List<Object> issues) {
        tx.execute(ignored->{
            var current=store.lockForRecovery(draft);
            var op=store.expireChange(draft,operation,failure("TIMEOUT",current.revision()));
            if(!op.terminal())store.failStoppedChange(draft,operation,token,new String(json.bytes(new Problem(allowed(reason),current.revision(),issues)),java.nio.charset.StandardCharsets.UTF_8));
            return null;
        });
    }
    private String failure(String reason,long revision){return new String(json.bytes(new Problem(reason,revision,List.of())),java.nio.charset.StandardCharsets.UTF_8);}
    private static String allowed(String reason) {
        return reason!=null&&Set.of("VALIDATION_FAILED","CALCULATION_FAILED","PRESERVATION_FAILED","STRUCTURE_VALIDATION_FAILED","SOURCE_CHANGED","POLICY_INCOMPLETE","POLICY_UNSUPPORTED","REVISION_CONFLICT","TIMEOUT").contains(reason)?reason:"PERSISTENCE_FAILED";
    }
    private Path writeMarker(String draft,String operation,String token)throws Exception {return writeMarker(draft,operation,token,null);}
    private Path writeMarker(String draft,String operation,String token,String reason)throws Exception {
        Path path=markers.resolve(uuid(operation)+"-"+uuid(token)+".json");
        Path pending=markers.resolve(".pending-"+path.getFileName());
        var record=new java.util.HashMap<String,String>();record.put("draftId",draft.equals("UPLOAD")?draft:uuid(draft));record.put("operationId",operation);record.put("token",token);
        if(reason!=null)record.put("failureReason",finalReason(reason));
        byte[] bytes=json.bytes(record);
        try(FileChannel file=FileChannel.open(pending,Set.of(StandardOpenOption.CREATE_NEW,StandardOpenOption.WRITE),
                PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rw-------")))) {
            ByteBuffer buffer=ByteBuffer.wrap(bytes);while(buffer.hasRemaining())file.write(buffer);file.force(true);
        }
        // 같은 filesystem에서 완성된 내용만 한 번에 공개한다. 실패하면 claim하지 않는다.
        Files.move(pending,path,StandardCopyOption.ATOMIC_MOVE);
        try(FileChannel directory=FileChannel.open(markers,StandardOpenOption.READ)){directory.force(true);}
        return path;
    }
    private static String uuid(String value){if(!UUID.fromString(value).toString().equals(value))throw new IllegalStateException("worker identifier");return value;}
    @Override public void close(){watchdog.shutdownNow();}
}
