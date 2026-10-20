package com.sk.domain.mi.service.fundamentalmodel.input;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sk.domain.mi.dto.response.fundamentalmodel.ModelInputDraftResponses.*;
import com.sk.domain.shared.repository.master.tmm.ModelInputDraftRepository;
import com.sk.domain.shared.repository.master.tmm.ModelInputDraftRepository.*;
import com.sk.domain.shared.service.permissions.AuthorizationService;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;

/** 파일 수신은 web, 검증/파싱은 공통 durable worker에서 수행한다. 파일 성공 전 Draft를 만들지 않는다. */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(propagation=Propagation.NOT_SUPPORTED)
public class InputUploadService {
    public static final long FILE_LIMIT=50L*1024*1024;
    private final InputUploadRepository uploads;
    private final ModelInputDraftRepository common;
    private final ModelInputDraftService drafts;
    private final InputUploadValidation validation;
    private final InputUploadObjects objects;
    private final InputTempFiles scratch;
    private final InputDraftJson json;
    private final PlatformTransactionManager transactionManager;
    @Nullable private final AuthorizationService authorization;
    @Value("${mi.model-input.upload.enabled:false}") private boolean enabled;
    @Value("${mi.model-input.upload.source-key:}") private String sourceKey;
    @Value("${mi.model-input.upload.model-code:}") private String modelCode;
    @io.swagger.v3.oas.annotations.media.Schema(requiredProperties={"operationId","requestId","status","statusEndpoint","result","failure"})
    public record View(@io.swagger.v3.oas.annotations.media.Schema(format="uuid") String operationId,
        @io.swagger.v3.oas.annotations.media.Schema(format="uuid") String requestId,
        @io.swagger.v3.oas.annotations.media.Schema(allowableValues={"QUEUED","RUNNING","SUCCEEDED","FAILED"}) String status,String statusEndpoint,
        JsonNode result,Problem failure) {}
    /** 저장된 JSON 결과를 wire 변환 없이 유지하고 공개 schema만 구체 타입으로 설명한다. */
    @io.swagger.v3.oas.annotations.media.Schema(name="InputUploadOperation",requiredProperties={"operationId","requestId","status","statusEndpoint","result","failure"})
    public record UploadOperationSchema(String operationId,String requestId,
        @io.swagger.v3.oas.annotations.media.Schema(allowableValues={"QUEUED","RUNNING","SUCCEEDED","FAILED"}) String status,String statusEndpoint,
        @io.swagger.v3.oas.annotations.media.Schema(nullable=true,types={"object","null"},schemaResolution=io.swagger.v3.oas.annotations.media.Schema.SchemaResolution.INLINE) ModelInputDraftService.UploadResult result,
        @io.swagger.v3.oas.annotations.media.Schema(nullable=true,types={"object","null"},schemaResolution=io.swagger.v3.oas.annotations.media.Schema.SchemaResolution.INLINE) UploadFailure failure) {}
    @io.swagger.v3.oas.annotations.media.Schema(requiredProperties={"reason","currentRevision","issues"})
    public record UploadFailure(String reason,@io.swagger.v3.oas.annotations.media.Schema(nullable=true,types={"integer","null"}) Long currentRevision,List<UploadIssue> issues) {}
    @io.swagger.v3.oas.annotations.media.Schema(requiredProperties={"scope","kind","sheetName","tableId","cellRef","reason"})
    public record UploadIssue(@io.swagger.v3.oas.annotations.media.Schema(allowableValues={"FILE","SHEET","TABLE","HEADER"}) String scope,
        @io.swagger.v3.oas.annotations.media.Schema(nullable=true,types={"string","null"},allowableValues={"DEMAND","SUPPLY","TRADE","OUTPUT"}) String kind,
        @io.swagger.v3.oas.annotations.media.Schema(nullable=true,types={"string","null"}) String sheetName,
        @io.swagger.v3.oas.annotations.media.Schema(nullable=true,types={"string","null"}) String tableId,
        @io.swagger.v3.oas.annotations.media.Schema(nullable=true,types={"string","null"}) String cellRef,String reason) {}
    record Metadata(String sourceKey,String requestId,Long baseRevision,boolean replaceConfirmed) {}
    private <T>T tx(Supplier<T> work) {
        TransactionTemplate tx=new TransactionTemplate(transactionManager);tx.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
        return tx.execute(ignored->work.get());
    }
    private void configured(){if(!enabled)throw InputDraftException.conflict("POLICY_UNSUPPORTED",null);if(sourceKey.isBlank()||modelCode.isBlank())throw InputDraftException.conflict("POLICY_INCOMPLETE",null);}
    Metadata metadata(String text,boolean followup) {
        JsonNode value=json.uploadMetadata(text);requestKeys(value,Set.of("sourceKey","requestId","baseRevision","replaceConfirmed"));
        if(!value.path("sourceKey").isTextual()||!value.path("requestId").isTextual())throw InputDraftException.invalid();
        String id=value.path("requestId").asText();uuid(id);
        if(!value.path("sourceKey").asText().equals(sourceKey))throw InputDraftException.invalid();
        Long revision=null;
        if(followup) {
            JsonNode n=value.path("baseRevision");if(!n.isIntegralNumber()||!n.canConvertToLong()||n.longValue()<0||n.longValue()>9007199254740991L)throw InputDraftException.invalid();revision=n.longValue();
            if(value.has("replaceConfirmed")&&!value.path("replaceConfirmed").isBoolean())throw InputDraftException.invalid();
        } else if(value.has("baseRevision")||value.has("replaceConfirmed"))throw InputDraftException.invalid();
        return new Metadata(sourceKey,id,revision,value.path("replaceConfirmed").asBoolean(false));
    }
    public View accept(String draft,MultipartFile file,String metadata) {
        configured();var actor=drafts.actor();Metadata request=metadata(metadata,draft!=null);
        if(draft!=null){uuid(draft);common.owned(draft,actor.company(),actor.user());}
        if(file==null||file.isEmpty())throw InputDraftException.invalid();if(file.getSize()>FILE_LIMIT)throw InputDraftException.large();
        String name=filename(file.getOriginalFilename()),extension=name.substring(name.lastIndexOf('.')+1).toLowerCase(Locale.ROOT);
        if(!Set.of("xlsx","xlsm").contains(extension))throw InputDraftException.invalid();
        String mime=Objects.requireNonNullElse(file.getContentType(),"application/octet-stream").toLowerCase(Locale.ROOT);
        if(mime.length()>100)throw InputDraftException.invalid();
        if(authorization==null||!Set.of("USER","ADMIN").contains(authorization.getCurrentRole().name()))throw InputDraftException.missing();
        String role=authorization.getCurrentRole().name(),id=UUID.randomUUID().toString();Path local=null,intake=null;
        try {
            local=scratch.create();MessageDigest digest=MessageDigest.getInstance("SHA-256");long size=0;
            try(var input=file.getInputStream();var output=Files.newOutputStream(local)) {
                byte[] buffer=new byte[65536];int read;
                while((read=input.read(buffer))!=-1){size+=read;if(size>FILE_LIMIT)throw InputDraftException.large();digest.update(buffer,0,read);output.write(buffer,0,read);}
            }
            if(size==0)throw InputDraftException.invalid();
            intake=scratch.registerUpload(id);
            var ref=objects.plan(actor.companyCode(),role,id);
            tx(()->{uploads.prepare(id,actor.company(),actor.user(),actor.login(),text(ref),name,extension,mime);return null;});
            objects.put(ref,actor.companyCode(),local,mime);
            final long bytes=size;final String hash=HexFormat.of().formatHex(digest.digest());
            tx(()->{uploads.received(id,bytes,hash);return null;});
            ObjectNode payload=(ObjectNode)json.tree(java.util.Map.of("metadata",request,"file",java.util.Map.of("name",name,"mime",mime,"size",bytes,"sha256",hash)));
            Operation accepted=tx(()->{
                var slot=uploads.lockSlot(actor.company(),actor.user(),actor.login());
                Operation previous=uploads.previous(actor.company(),actor.user(),draft,request.requestId());
                if(previous!=null) {
                    if(!Set.of("UPLOAD_CREATE","UPLOAD_FILE").contains(previous.type())||!json.sameValue(json.payload(previous.request()),payload))throw InputDraftException.conflict("IDEMPOTENCY_CONFLICT",draft==null?null:common.owned(draft,actor.company(),actor.user()).revision());
                    return previous;
                }
                if(draft==null) {if(slot.operation()!=null||slot.draft()!=null)throw InputDraftException.conflict("UPLOAD_IN_PROGRESS",null);}
                else {
                    Draft current=common.lockOwned(draft,actor.company(),actor.user());
                    if(!current.uploaded()||!draft.equals(slot.draft()))throw InputDraftException.missing();
                    if(!current.status().equals("DRAFT"))throw InputDraftException.conflict("DRAFT_NOT_EDITABLE",current.revision());
                    if(current.activeOperation()!=null)throw InputDraftException.conflict("DRAFT_BUSY",current.revision());
                    if(current.revision()!=request.baseRevision())throw InputDraftException.conflict("REVISION_CONFLICT",current.revision());
                    String kind=null;try{kind=InputUploadValidation.kind(name);}catch(InputUploadValidation.Failure ignored){/* 판별 실패는 비동기 FILE issue로 확정한다. */}
                    final String detected=kind;
                    if(detected!=null&&uploads.files(draft).stream().anyMatch(u->detected.equals(u.kind()))&&!request.replaceConfirmed())throw InputDraftException.conflict("REPLACEMENT_NOT_CONFIRMED",current.revision());
                }
                return uploads.accept(uploads.file(id),draft,request.requestId(),request.baseRevision(),role,actor.login(),text(payload));
            });
            return view(accepted);
        } catch(InputDraftException ex){throw ex;}
        catch(Exception ex){throw InputDraftException.internal();}
        finally {
            if(intake!=null)try{tx(()->{uploads.stoppedIntake(id);return null;});scratch.acknowledgedUpload(id);}
            catch(Exception ex){log.warn("Input upload intake recovery remains pending");}
            if(local!=null)try{Files.deleteIfExists(local);}catch(Exception ex){throw InputDraftException.internal();}
        }
    }
    public View operation(String id) {
        uuid(id);var actor=drafts.actor();return view(uploads.ownedOperation(id,actor.company(),actor.user()));
    }
    private Long revision(Operation op){return op.draftId()==null?null:common.lockForRecovery(op.draftId()).revision();}
    private View view(Operation op) {
        return new View(op.id(),op.requestId(),op.status(),"/api/v1/mi/fundamental-model/input-upload-requests/"+op.id(),
            op.status().equals("SUCCEEDED")?json.payload(op.result()):null,op.status().equals("FAILED")?problem(json.payload(op.failure())):null);
    }
    private Problem problem(JsonNode value) {
        List<Object> issues=new ArrayList<>();value.path("issues").forEach(issues::add);
        return new Problem(value.path("reason").asText(),value.path("currentRevision").isNull()?null:value.path("currentRevision").longValue(),List.copyOf(issues));
    }
    public Structure discard(String draft,String body) {
        uuid(draft);var actor=drafts.actor();JsonNode value=json.uploadMetadata(body);
        requestKeys(value,Set.of("baseRevision","discardConfirmed"));
        if(!value.path("discardConfirmed").isBoolean()||!value.path("discardConfirmed").booleanValue()||!value.path("baseRevision").isIntegralNumber()||!value.path("baseRevision").canConvertToLong()||value.path("baseRevision").longValue()<0||value.path("baseRevision").longValue()>9007199254740991L)throw InputDraftException.invalid();
        tx(()->{
            uploads.lockSlot(actor.company(),actor.user(),actor.login());Draft current=common.lockOwned(draft,actor.company(),actor.user());
            if(!current.uploaded())throw InputDraftException.missing();if(current.status().equals("DISCARDED"))return null;
            if(!current.status().equals("DRAFT"))throw InputDraftException.conflict("DRAFT_NOT_EDITABLE",current.revision());
            if(current.activeOperation()!=null)throw InputDraftException.conflict("DRAFT_BUSY",current.revision());
            if(current.revision()!=value.path("baseRevision").longValue())throw InputDraftException.conflict("REVISION_CONFLICT",current.revision());
            uploads.discard(current);return null;
        });return drafts.structure(draft);
    }
    List<Operation> pending(){return enabled?uploads.pending():List.of();}
    void expireQueued(Operation observed) {
        tx(()->{Operation op=uploads.lock(observed);if(op.status().equals("QUEUED")&&!common.databaseTime().isBefore(op.expires()))uploads.fail(op,text(new Problem("TIMEOUT",revision(op),List.of())));return null;});
    }
    boolean terminal(String id){return uploads.operation(id).terminal();}
    private static void requestKeys(JsonNode value,Set<String> allowed){value.fieldNames().forEachRemaining(key->{if(!allowed.contains(key))throw InputDraftException.invalid();});}
    void process(Operation observed,String token) {
        Operation op=tx(()->uploads.claim(observed,token));if(op==null)return;
        try {
            var owner=uploads.owner(op);var actor=new ModelInputDraftService.Actor(owner.company(),owner.user(),owner.companyCode(),owner.login());
            var u=uploads.operationFile(op.id());String kind=InputUploadValidation.kind(u.name());
            Draft old=op.draftId()==null?null:common.owned(op.draftId(),owner.company(),owner.user());
            Catalog catalog=uploads.template(old==null?u.template():Long.valueOf(old.templateId()),modelCode);
            tx(()->{uploads.validation(u,kind,catalog.templateId());return null;});
            String location=json.payload(u.reference()).path("location").asText();
            long next=old==null?0:op.baseRevision()+1;String draft=old==null?UUID.randomUUID().toString():old.id();
            FileRef file=new FileRef(kind,null,u.name(),location,next,u.hash(),u.size());
            List<RowRef> newRows=validation.validate(file,owner.companyCode(),catalog,u.extension(),u.mime());
            List<FileRef> files=new ArrayList<>();if(old!=null)for(var item:uploads.files(old.id()))if(!item.kind().equals(kind))files.add(item.file(json.payload(item.reference()).path("location").asText()));files.add(file);
            List<Revision> chain=new ArrayList<>(old==null?List.of():common.revisions(draft));
            String delta=text(java.util.Map.of("policyTemplateId",catalog.templateId(),"uploadFileId",u.id(),"kind",kind));
            if(old==null)chain.add(new Revision(0,null,null,2,null,null,null,0,null,null,null));
            else chain.add(new Revision(next,next-1,op.id(),2,delta,"UPLOAD_FILE","SUCCEEDED",2,op.baseRevision(),op.request(),"{}"));
            List<RowIdentity> identities=new ArrayList<>(old==null?List.of():common.rowIdentities(draft));
            List<RowPosition> positions=new ArrayList<>(old==null?List.of():common.rowPositions(draft));
            Set<Long> affected=catalog.tables().stream().filter(t->t.role().equals(kind)).map(TableRef::id).collect(java.util.stream.Collectors.toSet());
            List<RowPosition> closed=new ArrayList<>();
            for(int i=0;i<positions.size();i++){var p=positions.get(i);if(affected.contains(p.table())&&p.end()==null&&p.row()!=null){positions.set(i,new RowPosition(p.table(),p.id(),p.begin(),next,p.row()));closed.add(new RowPosition(p.table(),p.id(),next,null,null));}}
            positions.addAll(closed);
            for(RowRef row:newRows){identities.add(new RowIdentity(row.table(),row.id(),row.row(),null,null,next));positions.add(new RowPosition(row.table(),row.id(),next,null,row.row()));}
            Draft candidate=new Draft(draft,owner.company(),owner.user(),(Long)null,catalog.templateId(),null,"DRAFT",next,null,null);
            String key=json.payload(op.request()).path("metadata").path("sourceKey").asText();
            var result=drafts.previewUpload(actor,candidate,catalog,files,key,chain,identities,positions,kind);
            String encoded=text(result);if(encoded.getBytes(StandardCharsets.UTF_8).length>InputDraftJson.BODY_LIMIT)throw InputDraftException.responseLarge();
            tx(()->{
                Operation current=uploads.lock(op);var slot=uploads.lockSlot(owner.company(),owner.user(),op.actor());uploads.owner(current);
                if(!current.status().equals("RUNNING")||!token.equals(current.token())||!common.databaseTime().isBefore(current.expires()))throw InputDraftException.conflict("TIMEOUT",old==null?null:old.revision());
                if(old==null) {if(!op.id().equals(slot.operation())||slot.draft()!=null)throw InputDraftException.internal();uploads.createDraft(draft,u,catalog,op.actor());}
                else {var actual=common.lockForRecovery(draft);if(!draft.equals(slot.draft())||!actual.status().equals("DRAFT")||actual.revision()!=op.baseRevision()||!op.id().equals(actual.activeOperation()))throw InputDraftException.conflict("REVISION_CONFLICT",actual.revision());}
                uploads.complete(current,draft,kind,next,encoded,delta,newRows,catalog);return null;
            });
        } catch(InputUploadValidation.Failure ex){fail(op,token,ex.problem.reason(),ex.problem.issues());}
        catch(InputDraftException ex){fail(op,token,ex.getReason()==null?"PERSISTENCE_FAILED":ex.getReason(),List.of());}
    }
    void fail(Operation observed,String token,String reason,List<Object> issues) {
        tx(()->{Operation current=uploads.lock(observed);if(current.terminal()||!Objects.equals(token,current.token()))return null;
            String safe=!common.databaseTime().isBefore(current.expires())?"TIMEOUT":reason;
            uploads.fail(current,text(new Problem(safe,revision(current),issues)));return null;});
    }
    boolean recoverStopped(String operation,String token) {
        var op=uploads.operation(operation);
        if(op.status().equals("QUEUED")){expireQueued(op);return true;} // marker 공개 후 claim 전 종료. 아직 실행하지 않았음.
        if(op.status().equals("RUNNING")&&token.equals(op.token())){fail(op,token,"PERSISTENCE_FAILED",List.of());return uploads.operation(operation).terminal();}
        return op.terminal()||!token.equals(op.token());
    }
    void cleanupCandidates() {
        for(var file:uploads.cleanupFiles())try {
            if(file.operation()!=null&&!uploads.operation(file.operation()).status().equals("FAILED"))continue;
            JsonNode saved=json.payload(file.reference());String company=uploads.company(file);
            var ref=new InputUploadObjects.Reference(saved.path("bucket").asText(),saved.path("key").asText(),saved.path("location").asText());
            boolean absent=objects.remove(ref,company);var now=common.databaseTime();
            String previous=saved.path("absentSince").asText(null);
            if(absent&&previous!=null&&java.time.LocalDateTime.parse(previous).plusSeconds(20).isBefore(now)) {
                tx(()->{uploads.cleaned(file.id());return null;});
            } else {
                ObjectNode next=(ObjectNode)saved.deepCopy();if(absent){if(previous==null)next.put("absentSince",now.toString());}else next.remove("absentSince");
                tx(()->{uploads.cleanupReference(file.id(),text(next));return null;});
            }
        } catch(Exception ex){log.warn("Input upload candidate cleanup remains pending");}
    }
    @org.springframework.context.event.EventListener(org.springframework.boot.context.event.ApplicationReadyEvent.class)
    public void recoverIntakes() {
        if(!enabled)return;
        scratch.recover();
        for(String id:scratch.stoppedUploads())try{tx(()->{uploads.stoppedIntake(id);return null;});scratch.acknowledgedUpload(id);}
        catch(Exception ex){log.warn("Input upload intake recovery remains pending");}
    }
    private String text(Object value){return new String(json.bytes(value),StandardCharsets.UTF_8);}
    static String filename(String value) {
        if(value==null||value.isBlank()||value.length()>300||value.indexOf('/')>=0||value.indexOf('\\')>=0||value.contains("..")||value.chars().anyMatch(c->Character.isISOControl(c)))throw InputDraftException.invalid();
        return Normalizer.normalize(value,Normalizer.Form.NFC);
    }
    static void uuid(String value){try{if(value==null||!UUID.fromString(value).toString().equals(value))throw InputDraftException.invalid();}catch(IllegalArgumentException ex){throw InputDraftException.invalid();}}
}
