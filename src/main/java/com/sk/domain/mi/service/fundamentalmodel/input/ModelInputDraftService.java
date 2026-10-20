package com.sk.domain.mi.service.fundamentalmodel.input;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sk.common.model.BaseResponseFactory;
import com.sk.domain.mi.dto.response.fundamentalmodel.ModelInputDraftResponses.*;
import com.sk.domain.mi.service.fundamentalmodel.input.InputWorkbookReader.Cell;
import com.sk.domain.mi.service.fundamentalmodel.input.InputWorkbookReader.Window;
import com.sk.domain.shared.repository.master.tmm.ModelInputDraftRepository;
import com.sk.domain.shared.repository.master.tmm.ModelInputDraftRepository.*;
import com.sk.domain.shared.service.permissions.AuthorizationService;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.util.CellReference;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly=true)
public class ModelInputDraftService {
    private static final List<String> ROLES=List.of("DEMAND","SUPPLY","TRADE","OUTPUT");
    private final ModelInputDraftRepository store;
    @org.springframework.lang.Nullable private final AuthorizationService authorization;
    private final InputWorkbookReader reader;
    private final InputDraftJson json;
    private InputUploadRepository uploads;
    @org.springframework.beans.factory.annotation.Autowired
    void uploads(InputUploadRepository uploads){this.uploads=uploads;}
    // JpaConfig의 primary manager이며 JDBC도 같은 masterDataSource를 사용한다.
    private final PlatformTransactionManager transactionManager;
    @org.springframework.beans.factory.annotation.Value("${mi.model-input.change-worker.enabled:false}")
    private boolean changesEnabled;
    @org.springframework.beans.factory.annotation.Value("${mi.model-input.finalization.enabled:false}")
    private boolean finalizationEnabled;
    private Supplier<InputDraftFinalizationService> finalizations=()->null;
    @org.springframework.beans.factory.annotation.Autowired
    void finalizations(org.springframework.beans.factory.ObjectProvider<InputDraftFinalizationService> provider) {
        finalizations=provider::getIfAvailable;
    }
    // 수동 조립하는 로컬 transaction fixture도 같은 경계를 사용한다.
    void finalizations(InputDraftFinalizationService service){finalizations=()->service;}
    private InputDraftFinalizationService finalizer() {
        var service=finalizations.get();if(service==null)throw InputDraftException.internal();return service;
    }
    Source verifyFinalizationBasis(Draft draft,ModelInputDraftRepository.Operation operation) {
        Actor actor=workerActor(draft,operation);Source original=source(actor,draft);
        if(!java.util.Objects.equals(draft.sourceVersion(),original.version())||draft.templateId()!=original.catalog().templateId())
            throw InputDraftException.conflict("SOURCE_CHANGED",draft.revision());
        fixed(draft,original,draft.sourceVersion());
        InputWorkbookEditor.validateHistory(original,store.revisions(draft.id()),json);
        return original;
    }
    @Transactional(propagation=Propagation.NOT_SUPPORTED)
    public FinalizationView submitFinalization(String id,InputDraftJson.FinalizationRequest request) {
        Actor actor=actor();recoverExpired(actor,id);
        tx(true,()->{
            Draft draft=store.owned(id,actor.company,actor.user);if(!draft.uploaded())store.caseAccess(draft.caseId(),false);
            if(!finalizationEnabled&&store.request(id,request.requestId())==null&&!draft.status().equals("FINALIZED"))
                throw InputDraftException.conflict("POLICY_UNSUPPORTED",draft.revision());
            return null;
        });
        var role=authorization.getCurrentRole();
        if(role!=com.sk.domain.shared.enums.Role.USER&&role!=com.sk.domain.shared.enums.Role.ADMIN)throw InputDraftException.missing();
        var operation=finalizer().accept(id,actor.company,actor.user,actor.login,role.name(),
            new InputDraftFinalizationService.Request(request.requestId(),request.baseRevision(),request.title(),request.description()));
        return finalizationView(operation);
    }
    @Transactional(propagation=Propagation.NOT_SUPPORTED)
    public FinalizationView finalizationOperation(String id,String operationId) {
        Actor actor=actor();recoverExpired(actor,id);
        return tx(true,()->{
            Draft draft=store.owned(id,actor.company,actor.user);if(!draft.uploaded())store.caseAccess(draft.caseId(),false);
            var op=store.operation(id,operationId,false);
            if(!op.type().equals("FINALIZE"))throw InputDraftException.missing();
            return finalizationView(op);
        });
    }
    private FinalizationView finalizationView(ModelInputDraftRepository.Operation op) {
        if(!op.type().equals("FINALIZE")||op.schemaVersion()!=2)throw InputDraftException.internal();
        String label=switch(op.status()){case "QUEUED"->"대기 중";case "RUNNING"->"처리 중";case "SUCCEEDED"->"최종저장 완료";case "FAILED"->"최종저장 실패";default->throw InputDraftException.internal();};
        Long caseId=null;List<FinalizedFile> files=List.of();Problem failure=null;FinalizationExecution execution=null;
        if(op.status().equals("SUCCEEDED")) {
            JsonNode result=json.payload(op.result());
            if(!result.path("finalizedCaseId").isIntegralNumber()||!result.path("finalizedCaseId").canConvertToLong()
                ||result.path("finalizedCaseId").longValue()<=0||!result.path("files").isArray()||result.path("files").size()!=4)throw InputDraftException.internal();
            caseId=result.path("finalizedCaseId").longValue();var saved=new ArrayList<FinalizedFile>();
            for(int i=0;i<4;i++) {
                JsonNode file=result.path("files").get(i);
                if(!InputDraftFinalizationService.ROLES.get(i).equals(file.path("role").asText())||!file.path("fileNo").isIntegralNumber()
                    ||!file.path("fileNo").canConvertToLong()||file.path("fileNo").longValue()<=0)throw InputDraftException.internal();
                saved.add(new FinalizedFile(file.path("role").asText(),file.path("fileNo").longValue()));
            }
            if(saved.stream().map(FinalizedFile::fileNo).distinct().count()!=4)throw InputDraftException.internal();files=List.copyOf(saved);
            execution=finalizationExecution(result,caseId);
        } else if(op.status().equals("FAILED")) {
            JsonNode problem=json.payload(op.failure());
            if(!Set.of("VALIDATION_FAILED","CALCULATION_FAILED","STRUCTURE_VALIDATION_FAILED","PRESERVATION_FAILED","PERSISTENCE_FAILED","TIMEOUT","SOURCE_CHANGED","POLICY_INCOMPLETE","POLICY_UNSUPPORTED").contains(problem.path("reason").asText())
                ||!problem.path("currentRevision").isIntegralNumber()||!problem.path("errors").isArray())throw InputDraftException.internal();
            List<Object> issues=new ArrayList<>();problem.path("errors").forEach(issues::add);
            failure=new Problem(problem.path("reason").asText(),problem.path("currentRevision").longValue(),List.copyOf(issues));
        }
        return new FinalizationView(op.id(),op.requestId(),op.draftId(),op.status(),label,op.baseRevision(),caseId,files,
            caseId==null?null:"/api/v1/mi/fundamental-model/cases/"+caseId,failure,
            "/api/v1/mi/fundamental-model/input-drafts/"+op.draftId()+"/finalizations/"+op.id(),execution);
    }
    private FinalizationExecution finalizationExecution(JsonNode result,long caseId) {
        if(!result.has("executionStatus"))return null; // 배포 전 완료한 저장은 자동 실행하지 않는다.
        String status=result.path("executionStatus").asText();
        String runId=result.path("executionRunId").asText(null),reason=result.path("executionFailureReason").asText(null);
        if(runId!=null) {
            var run=store.finalizationRun(caseId,runId);status=run.status();
            reason=status.equals("FAILED")?("JOB_SUBMIT_FAILED".equals(run.failureReason())?"JOB_SUBMIT_FAILED":"MODEL_EXECUTION_FAILED"):null;
        } else if(!status.equals("QUEUED")&&!status.equals("FAILED"))throw InputDraftException.internal();
        if(status.equals("FAILED")&&runId==null&&!"EXECUTION_REQUEST_FAILED".equals(reason))throw InputDraftException.internal();
        String label=switch(status){case "QUEUED"->"실행 대기";case "RUNNING"->"실행 중";case "COMPLETED"->"실행 완료";case "FAILED"->"실행 실패";default->throw InputDraftException.internal();};
        return new FinalizationExecution(status,label,runId,"/api/v1/mi/fundamental-model/cases/"+caseId+"/status",reason);
    }
    record Actor(int company,int user,String companyCode,String login) {}
    record Source(Long caseId,Catalog catalog,List<FileRef> files,String key,String version,
        Map<String,String> inputIds,Map<Long,InputReadDefinition> definitions) {
        Source(long caseId,Catalog catalog,List<FileRef> files,String key,String version,Map<String,String> inputIds,Map<Long,InputReadDefinition> definitions){this(Long.valueOf(caseId),catalog,files,key,version,inputIds,definitions);}
    }
    private record State(Draft draft,Source source,InputDraftHistory history) {}
    Actor actor() {
        if(authorization==null)throw InputDraftException.missing();
        int company=authorization.getCompanyNoWithPriority(),user=authorization.getUserNo();
        String code=authorization.getCompanyCdWithPriority(),login=authorization.getSubject();
        if(company<=0||user<=0||code==null||code.isBlank()||login==null||login.isBlank()||login.length()>50)throw InputDraftException.missing();
        return new Actor(company,user,code,login);
    }
    private <T>T tx(boolean readOnly,Supplier<T> action) {
        TransactionTemplate tx=new TransactionTemplate(transactionManager);tx.setReadOnly(readOnly);
        tx.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
        return tx.execute(ignored->action.get());
    }
    private Source source(Actor actor,long caseId,boolean lock) {
        if(caseId<1||caseId>Integer.MAX_VALUE)throw InputDraftException.missing();
        store.caseAccess(caseId,lock);Catalog catalog=store.catalog(caseId);
        if(catalog.roles().size()!=4||!new HashSet<>(catalog.roles()).equals(new HashSet<>(ROLES)))throw InputDraftException.conflict("POLICY_INCOMPLETE",null);
        List<FileRef> registered=store.files(caseId);Map<String,FileRef> files=new LinkedHashMap<>();
        for(FileRef file:registered) {
            List<String> matches=ROLES.stream().filter(role->file.name()!=null&&file.name().toUpperCase(Locale.ROOT).contains(role)).toList();
            if(matches.size()!=1||file.fileNo()<=0||file.location()==null||file.location().isBlank()||files.containsKey(matches.get(0)))throw InputDraftException.conflict("POLICY_INCOMPLETE",null);
            String role=matches.get(0);files.put(role,new FileRef(role,file.fileNo(),file.name(),file.location()));
        }
        if(files.size()!=4)throw InputDraftException.conflict("POLICY_INCOMPLETE",null);
        Map<Long,InputReadDefinition> definitions=new LinkedHashMap<>();Set<String> keys=new HashSet<>();
        for(TableRef table:catalog.tables()) {
            if(!keys.add(table.key())||!ROLES.contains(table.role()))throw InputDraftException.conflict("POLICY_INCOMPLETE",null);
            InputReadDefinition def=InputReadDefinition.parse(json.definition(table.definition()));
            if(def.columns().stream().anyMatch(c->c<table.startColumn()||c>table.endColumn())
                ||def.headerRows().stream().anyMatch(r->r<table.startRow()||r>table.endRow())
                ||def.bodyRanges().stream().anyMatch(range->range.startRow()<table.dataStart()||range.endRow()>table.endRow()))throw InputDraftException.conflict("POLICY_INCOMPLETE",null);
            definitions.put(table.id(),def);
        }
        Map<String,JsonNode> controlDefinitions=new HashMap<>();
        for(TableRef table:catalog.tables())for(JsonNode control:definitions.get(table.id()).controls()) {
            TableRef owner=catalog.tables().stream().filter(t->t.key().equals(control.path("sourceTableId").asText())).findFirst().orElseThrow(()->InputDraftException.conflict("POLICY_INCOMPLETE",null));
            int row=control.path("binding").path("rowNumber").asInt(),col=control.path("binding").path("columnIndex").asInt()+1;
            InputReadDefinition def=definitions.get(table.id());
            if(!owner.role().equals(table.role())||!owner.sheet().equals(table.sheet())
                ||row<owner.startRow()||row>owner.endRow()||col<owner.startColumn()||col>owner.endColumn()
                ||(def.columns().contains(col)&&(def.headerRows().contains(row)||def.body(row))))throw InputDraftException.conflict("POLICY_INCOMPLETE",null);
            String controlKey=table.role()+"/"+table.sheet()+"/"+control.path("id").asText();
            JsonNode previous=controlDefinitions.putIfAbsent(controlKey,control);
            if(previous!=null&&!previous.equals(control))throw InputDraftException.conflict("POLICY_INCOMPLETE",null);
        }
        String template="template-v1:"+catalog.templateId()+":"+catalog.version(),publication="catalog-v1:"+catalog.templateId();
        String key=hash("sc1",ordered("ruleVersion","sc1","tenantKey",Integer.toString(actor.company),"caseId",Long.toString(caseId)));
        List<Map<String,String>> refs=new ArrayList<>();Map<String,String> inputIds=new LinkedHashMap<>();
        List<FileRef> ordered=new ArrayList<>();
        for(String role:ROLES) {
            FileRef file=files.get(role);ordered.add(file);
            Map<String,String> ref=ordered("kind",role,"fileId",Long.toString(file.fileNo()),"objectRef",file.location(),"contentRevision","file-reg-v1:"+file.fileNo());refs.add(ref);
            Map<String,String> input=ordered("ruleVersion","in1","sourceKey",key);input.putAll(ref);input.put("templateVersion",template);input.put("catalogRevision",publication);
            inputIds.put(role,hash("in1",input));
        }
        Map<String,Object> version=new LinkedHashMap<>(ordered("ruleVersion","sv1","tenantKey",Integer.toString(actor.company),"caseId",Long.toString(caseId)));
        version.put("files",refs);version.put("templateVersion",template);version.put("catalogRevision",publication);
        return new Source(caseId,catalog,List.copyOf(ordered),key,hash("sv1",version),Map.copyOf(inputIds),Map.copyOf(definitions));
    }
    private Source source(Actor actor,Draft draft) {
        if(!draft.uploaded())return source(actor,draft.caseId(),false);
        if(uploads==null)throw InputDraftException.internal();
        var registered=uploads.files(draft.id());
        if(registered.isEmpty())throw InputDraftException.internal();
        var first=uploads.operation(registered.get(0).operation());
        String key=json.payload(first.request()).path("metadata").path("sourceKey").asText();
        List<FileRef> files=registered.stream().map(u->u.file(json.payload(u.reference()).path("location").asText())).toList();
        return uploadSource(uploads.template(draft.templateId(),null),files,key);
    }
    Source uploadSource(Catalog full,List<FileRef> files,String sourceKey) {
        Set<String> present=files.stream().map(FileRef::role).collect(java.util.stream.Collectors.toSet());
        Catalog catalog=new Catalog(full.templateId(),full.version(),full.roles().stream().filter(present::contains).toList(),full.tables().stream().filter(t->present.contains(t.role())).toList());
        Map<Long,InputReadDefinition> definitions=new LinkedHashMap<>();Map<String,String> ids=new LinkedHashMap<>();
        for(TableRef table:catalog.tables())definitions.put(table.id(),InputReadDefinition.parse(json.definition(table.definition())));
        for(FileRef file:files)ids.put(file.role(),hash("in1",ordered("sourceKey",sourceKey,"kind",file.role(),"template",Long.toString(catalog.templateId()),"objectRef",file.location())));
        return new Source(null,catalog,List.copyOf(files),sourceKey,null,Map.copyOf(ids),Map.copyOf(definitions));
    }
    private Structure structure(State state) {
        Readiness readiness=null;
        if(state.draft.uploaded()) {
            List<String> present=ROLES.stream().filter(r->state.source.inputIds.containsKey(r)).toList();
            readiness=new Readiness(present,ROLES.stream().filter(r->!present.contains(r)).toList(),finalizationEnabled&&present.size()==4&&state.draft.status().equals("DRAFT")&&state.draft.activeOperation()==null);
        }
        return new Structure(summary(state.draft,state.source),sections(state.source),null,readiness);
    }
    private static Map<String,String> ordered(String... pairs) {Map<String,String> map=new LinkedHashMap<>();for(int i=0;i<pairs.length;i+=2)map.put(pairs[i],pairs[i+1]);return map;}
    private String hash(String prefix,Object value) {
        try {return prefix+"_"+HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(json.bytes(value)));}
        catch(java.security.NoSuchAlgorithmException ex){throw InputDraftException.internal();}
    }
    @Transactional(propagation=Propagation.NOT_SUPPORTED)
    public Created create(long caseId,String requestedVersion) {
        Actor actor=actor();Source basis=tx(true,()->source(actor,caseId,false));
        Draft previous=tx(true,()->store.active(actor.company,actor.user,caseId));
        if(previous!=null)return tx(false,()->{
            Source current=source(actor,caseId,true);
            Draft existing=store.active(actor.company,actor.user,caseId);
            if(existing==null)throw InputDraftException.conflict("DRAFT_SOURCE_CONFLICT",previous.revision());
            existing=store.lockOwned(existing.id(),actor.company,actor.user);
            fixed(existing,current,requestedVersion);
            return new Created(summary(existing,restore(existing,current).source),false);
        });
        if(!basis.version.equals(requestedVersion))throw InputDraftException.conflict("SOURCE_CHANGED",null);
        List<RowRef> rows=new ArrayList<>();
        for(FileRef file:basis.files) {
            List<Window> windows=new ArrayList<>();
            List<TableRef> regas=basis.catalog.tables().stream().filter(table->table.role().equals(file.role())
                &&InputWorkbookPolicy.kind(table)==InputWorkbookPolicy.Kind.REGAS_MONTHLY).toList();
            if(regas.size()>1)throw InputDraftException.conflict("POLICY_INCOMPLETE",null);
            for(TableRef table:basis.catalog.tables())if(table.role().equals(file.role())) {
                InputReadDefinition def=basis.definitions.get(table.id());
                if(regas.contains(table))continue;
                windows.add(new Window(table.sheet(),def::body,Set.copyOf(def.identityColumns()),(row,cells)->{
                    boolean active=false;
                    for(int col:def.identityColumns()) {
                        Cell value=cells.get(col);if(value==null)continue;
                        if(value.formula())throw InputDraftException.conflict("POLICY_INCOMPLETE",null);
                        if(!(value.raw() instanceof String text)||!text.isBlank())active=true;
                    }
                    if(active)rows.add(new RowRef(table.id(),row,UUID.randomUUID().toString()));
                }));
            }
            if(regas.isEmpty())reader.read(file,actor.companyCode,windows);
            else reader.withLocal(file,actor.companyCode,path->{
                try {
                    reader.readLocal(path,windows);
                    List<TableRef> masters=basis.catalog.tables().stream().filter(table->table.role().equals("SUPPLY")&&table.sheet().equals("BCRegasification")).toList();
                    TableRef monthly=regas.get(0);
                    if(masters.size()!=1||!basis.definitions.get(monthly.id()).identityColumns().equals(List.of(2,3,4,5,6)))throw InputDraftException.conflict("POLICY_INCOMPLETE",null);
                    TableRef master=masters.get(0);
                    Set<Integer> masterRows=rows.stream().filter(row->row.table()==master.id()).map(RowRef::row).collect(java.util.stream.Collectors.toSet());
                    for(int row:InputWorkbookRegas.linkedSourceRows(path,master,monthly,masterRows,()->{})) {
                        if(!basis.definitions.get(monthly.id()).body(row))throw InputDraftException.conflict("POLICY_INCOMPLETE",null);
                        rows.add(new RowRef(monthly.id(),row,UUID.randomUUID().toString()));
                    }
                    return null;
                } catch(InputDraftException ex){throw ex;}
                catch(InputDraftPreserveChange.Failure ex){throw InputDraftException.conflict("POLICY_INCOMPLETE",null);}
                catch(Exception ex){throw InputDraftException.internal();}
            });
        }
        return tx(false,()->{
            Source current=source(actor,caseId,true);
            if(!basis.version.equals(current.version))throw InputDraftException.conflict("SOURCE_CHANGED",null);
            Draft existing=store.active(actor.company,actor.user,caseId);
            if(existing!=null){existing=store.lockOwned(existing.id(),actor.company,actor.user);fixed(existing,current,requestedVersion);return new Created(summary(existing,restore(existing,current).source),false);}
            Draft created=store.create(actor.company,actor.user,caseId,current.catalog,current.version,current.files,rows,actor.login);
            return new Created(summary(created,current),true);
        });
    }
    private void fixed(Draft draft,Source source,String version) {
        if(draft.uploaded()){if(draft.templateId()!=source.catalog.templateId())throw InputDraftException.internal();return;}
        if(!draft.sourceVersion().equals(version)||!java.util.Objects.equals(draft.sourceVersion(),source.version)||draft.templateId()!=source.catalog.templateId())throw InputDraftException.conflict("DRAFT_SOURCE_CONFLICT",draft.revision());
        Map<String,Long> expected=new LinkedHashMap<>();source.files.forEach(f->expected.put(f.role(),f.fileNo()));
        if(!expected.equals(tx(true,()->store.fixedFiles(draft.id()))))throw InputDraftException.conflict("SOURCE_CHANGED",draft.revision());
    }
    private State state(Actor actor,String id) {
        return state(actor,store.lockOwned(id,actor.company,actor.user));
    }
    private State state(Actor actor,Draft draft) {
        Source source=source(actor,draft);
        if(!java.util.Objects.equals(draft.sourceVersion(),source.version)) throw InputDraftException.conflict("SOURCE_CHANGED",draft.revision());
        fixed(draft,source,draft.sourceVersion());
        return restore(draft,source);
    }
    private State restore(Draft draft,Source source) {
        InputDraftHistory history=InputDraftHistory.restore(draft,source.catalog,source.definitions,
            store.revisions(draft.id()),store.rowIdentities(draft.id()),store.rowPositions(draft.id()),json);
        Source current=new Source(source.caseId,history.catalog(),source.files,source.key,source.version,source.inputIds,history.definitions());
        return new State(draft,current,history);
    }
    private Summary summary(Draft draft,Source source) {
        String label=switch(draft.status()){case "DRAFT"->"편집 중";case "FINALIZED"->"최종저장 완료";case "DISCARDED"->"폐기";default->throw InputDraftException.internal();};
        String endpoint=null;
        if(draft.activeOperation()!=null) {
            var op=store.operation(draft.id(),draft.activeOperation(),false);
            if(op.terminal())throw InputDraftException.internal();
            endpoint=op.type().equals("UPLOAD_FILE")?"/api/v1/mi/fundamental-model/input-upload-requests/"+op.id()
                :"/api/v1/mi/fundamental-model/input-drafts/"+draft.id()+(op.type().equals("CHANGE")?"/change-requests/":"/finalizations/")+op.id();
        }
        return new Summary(draft.id(),draft.caseId(),source.key,draft.sourceVersion(),draft.status(),label,draft.revision(),draft.activeOperation(),endpoint,draft.finalizedCase(),"/api/v1/mi/fundamental-model/input-drafts/"+draft.id());
    }
    @Transactional(propagation=Propagation.NOT_SUPPORTED)
    public OperationView submitChange(String id,InputDraftJson.ChangeRequest request) {
        Actor actor=actor();recoverExpired(actor,id);
        return tx(false,()->{
            Draft draft=store.lockOwned(id,actor.company,actor.user);if(!draft.uploaded())store.caseAccess(draft.caseId(),false);
            var previous=store.request(id,request.requestId());
            if(previous!=null) {
                if(previous.schemaVersion()!=2)throw InputDraftException.internal();
                if(!previous.type().equals("CHANGE")||!json.sameRequest(previous.request(),request))throw InputDraftException.conflict("IDEMPOTENCY_CONFLICT",draft.revision());
                return operationView(previous);
            }
            Source current=source(actor,draft);
            if(!java.util.Objects.equals(draft.sourceVersion(),current.version))throw InputDraftException.conflict("SOURCE_CHANGED",draft.revision());
            fixed(draft,current,draft.sourceVersion());
            if(!draft.status().equals("DRAFT"))throw InputDraftException.conflict("DRAFT_NOT_EDITABLE",draft.revision());
            if(draft.activeOperation()!=null)throw InputDraftException.conflict("DRAFT_BUSY",draft.revision());
            if(draft.revision()!=request.baseRevision())throw InputDraftException.conflict("REVISION_CONFLICT",draft.revision());
            State state=restore(draft,current);
            validateChange(state.source,request,draft);
            var role=authorization.getCurrentRole();
            if(role!=com.sk.domain.shared.enums.Role.USER&&role!=com.sk.domain.shared.enums.Role.ADMIN)throw InputDraftException.missing();
            var op=store.enqueueChange(draft,request.requestId(),request.baseRevision(),new String(json.changeBytes(request),StandardCharsets.UTF_8),role.name(),actor.login);
            return operationView(op);
        });
    }
    void validateChange(Source source,InputDraftJson.ChangeRequest request,Draft draft) {
        // 모든 표 접근을 먼저 확인해 필드 검증이 다른 표의 존재를 드러내지 않게 한다.
        Map<String,TableRef> requested=new HashMap<>();
        for(JsonNode command:request.operations()) {
            String key=command.path("tableId").asText();requested.put(key,source.catalog.tables().stream().filter(t->t.key().equals(key)).findFirst().orElseThrow(InputDraftException::missing));
        }
        Map<String,JsonNode> newColumns=new HashMap<>();Set<String> deletedFields=new HashSet<>();
        for(JsonNode command:request.operations()) {
            TableRef table=requested.get(command.path("tableId").asText());InputReadDefinition definition=source.definitions.get(table.id());
            String type=command.path("type").asText();JsonNode field=null;
            if(command.has("fieldKey")) {
                String key=command.path("fieldKey").asText();
                for(JsonNode candidate:definition.fields())if(key.equals(candidate.path("fieldKey").asText()))field=candidate;
                if(field==null||deletedFields.contains(table.key()+"/"+key))throw InputDraftException.invalid();
            }
            if(type.equals("ADD_ROW")) {
                var values=command.path("values").fields();while(values.hasNext()) {
                    var entry=values.next();JsonNode target=null;for(JsonNode candidate:definition.fields())if(entry.getKey().equals(candidate.path("fieldKey").asText()))target=candidate;
                    if(target==null||deletedFields.contains(table.key()+"/"+entry.getKey())||!inputType(target.path("dataType").asText(),entry.getValue()))throw InputDraftException.invalid();
                }
            }
            policy(definition.capabilities().path(type).asText(),draft,command,request,"ACTION_NOT_ALLOWED");
            if(field!=null)policy(field.path("policyStatus").path(command.has("headerKey")?"SET_HEADER":type).asText(),draft,command,request,type.equals("CLEAR")?"CLEAR_NOT_ALLOWED":"ACTION_NOT_ALLOWED");
            if(type.equals("ADD_COLUMN")) {
                JsonNode rules=definition.columnCreation().path("headerValues"),values=command.path("headerValues");
                var names=values.fieldNames();while(names.hasNext())if(!rules.has(names.next()))throw InputDraftException.invalid();
                var headers=rules.fields();while(headers.hasNext()) {
                    var entry=headers.next();JsonNode rule=entry.getValue();String key=entry.getKey();
                    if(!values.has(key)) {
                        if(rule.path("create").path("required").asBoolean())throw InputDraftException.validation(draft.revision(),List.of(Map.of(
                            "commandId",command.path("commandId").asText(),"tableId",table.key(),"path","/operations/"+request.operations().indexOf(command)+"/headerValues/"+key,"reason","REQUIRED")));
                    } else typed(rule.path("dataType").asText(),values.path(key).decimalValue());
                }
                newColumns.put(command.path("clientColumnKey").asText(),definition.columnCreation().path("body"));
            } else if(type.equals("DELETE_COLUMN")&&command.has("fieldKey"))deletedFields.add(table.key()+"/"+command.path("fieldKey").asText());
            if(type.equals("SET")&&!command.has("controlId")) {
                JsonNode target=field==null?newColumns.get(command.path("clientColumnKey").asText()):field;
                if(target==null)throw InputDraftException.invalid();JsonNode value=command.path("value");String dataType=command.has("headerKey")?"INTEGER":target.path("dataType").asText();
                if(!inputType(dataType,value))throw InputDraftException.invalid();
            }
        }
        if(!changesEnabled)throw InputDraftException.conflict("POLICY_UNSUPPORTED",draft.revision());
        Map<Long,Map<Integer,String>> bindings=new HashMap<>();
        for(var row:store.rowPositions(draft.id()))if(row.end()==null&&row.row()!=null)
            bindings.computeIfAbsent(row.table(),ignored->new HashMap<>()).put(row.row(),row.id());
        InputWorkbookEditor.validate(source,bindings,request,draft.revision());
        InputWorkbookEditor.validateHistory(source(actor(),draft),store.revisions(draft.id()),json);
    }
    private boolean inputType(String type,JsonNode value) {
        if(type.equals("TEXT"))return value.isTextual();if(!value.isNumber())return false;
        if(type.equals("INTEGER"))try{value.decimalValue().toBigIntegerExact();}catch(ArithmeticException ex){return false;}return true;
    }
    private void policy(String status,Draft draft,JsonNode command,InputDraftJson.ChangeRequest request,String reason) {
        if(status.equals("INCOMPLETE")||status.isEmpty())throw InputDraftException.conflict("POLICY_INCOMPLETE",draft.revision());
        if(status.equals("NOT_ALLOWED"))throw InputDraftException.validation(draft.revision(),List.of(Map.of(
            "commandId",command.path("commandId").asText(),"tableId",command.path("tableId").asText(),
            "path","/operations/"+request.operations().indexOf(command),"reason",reason)));
        if(!status.equals("READY"))throw InputDraftException.conflict("POLICY_UNSUPPORTED",draft.revision());
    }
    record PreparedChange(String draftId,String operationId,String token,String company,FileRef file,
            long templateId,TableRef table,String rowId,InputDraftJson.ChangeRequest request,BigDecimal previous,InputDraftEditingBasis basis) {
        PreparedChange(String draftId,String operationId,String token,String company,FileRef file,long templateId,TableRef table,
                String rowId,InputDraftJson.ChangeRequest request,BigDecimal previous) {
            this(draftId,operationId,token,company,file,templateId,table,rowId,request,previous,null);
        }
    }
    private Actor workerActor(Draft draft,ModelInputDraftRepository.Operation op) {
        var owner=store.workerOwner(draft,op);
        if(owner.companyCode()==null||owner.companyCode().isBlank()||owner.login()==null||owner.login().isBlank())throw InputDraftException.missing();
        return new Actor(owner.company(),owner.user(),owner.companyCode(),owner.login());
    }
    @Transactional(propagation=Propagation.NOT_SUPPORTED)
    PreparedChange prepareWorkerChange(String id,String operationId,String token) {
        return tx(false,()->{
            Draft draft=store.lockForRecovery(id);var op=store.lockRunningChange(id,operationId,token);
            if(op==null)return null;
            Actor actor=workerActor(draft,op);State current=state(actor,draft);
            var request=json.changeRequest(new java.io.ByteArrayInputStream(op.request().getBytes(StandardCharsets.UTF_8)));
            InputWorkbookEditor.validate(current.source,current.history,request,draft.revision());
            Source original=source(actor,draft);
            InputWorkbookEditor.validateHistory(original,store.revisions(id),json);
            var basis=new InputDraftEditingBasis(draft,original,store.revisions(id),store.rowIdentities(id),store.rowPositions(id));
            TableRef table=current.source.catalog.tables().stream().filter(t->t.key().equals(request.operations().get(0).path("tableId").asText())).findFirst().orElseThrow(InputDraftException::internal);
            return new PreparedChange(id,op.id(),token,actor.companyCode,current.source.files.stream().filter(f->f.role().equals(table.role())).findFirst().orElseThrow(),
                current.source.catalog.templateId(),table,request.operations().get(0).path("rowRef").path("rowId").asText(),request,null,basis);
        });
    }
    @Transactional(propagation=Propagation.NOT_SUPPORTED)
    public OperationView changeOperation(String id,String operationId) {
        Actor actor=actor();recoverExpired(actor,id);
        return tx(true,()->{
            Draft draft=store.owned(id,actor.company,actor.user);if(!draft.uploaded())store.caseAccess(draft.caseId(),false);
            var op=store.operation(id,operationId,false);
            if(!op.type().equals("CHANGE"))throw InputDraftException.missing();
            return operationView(op);
        });
    }
    private void recoverExpired(Actor actor,String id) {
        tx(false,()->{
            Draft draft=store.lockOwned(id,actor.company,actor.user);if(!draft.uploaded())store.caseAccess(draft.caseId(),false);
            if(draft.activeOperation()!=null) {
                var operation=store.operation(id,draft.activeOperation(),true);
                if(operation.type().equals("FINALIZE"))finalizer().expireQueuedJoined(id,operation.id());
                else store.expireChange(id,operation.id(),failureJson("TIMEOUT",draft.revision()));
            }
            return null;
        });
    }
    private String failureJson(String reason,long revision) {
        return new String(json.bytes(new Problem(reason,revision,List.of())),StandardCharsets.UTF_8);
    }
    /** 내부 복구 경계: 호출자는 이 token의 worker 종료를 확인해야 한다. HTTP로 노출하지 않는다. */
    @Transactional(propagation=Propagation.NOT_SUPPORTED)
    public void recoverStoppedChange(String id,String operationId,String stoppedToken) {
        tx(false,()->{
            Draft draft=store.lockForRecovery(id);
            // timeout을 먼저 확정하고, 성공/실패가 이미 commit됐다면 종료 상태를 보존한다.
            var op=store.expireChange(id,operationId,failureJson("TIMEOUT",draft.revision()));
            if(!op.terminal())store.failStoppedChange(id,operationId,stoppedToken,failureJson("PERSISTENCE_FAILED",draft.revision()));
            return null;
        });
    }
    /** 후속 처리기가 검증한 결과의 내부 확정 경계. HTTP/dispatcher에는 연결하지 않는다. */
    @Transactional(propagation=Propagation.NOT_SUPPORTED)
    public boolean completeChange(String id,String operationId,String token,JsonNode verifiedDelta) {
        Actor actor=actor();
        var role=authorization.getCurrentRole();
        if(role!=com.sk.domain.shared.enums.Role.USER&&role!=com.sk.domain.shared.enums.Role.ADMIN)throw InputDraftException.missing();
        return completeChange(actor,id,operationId,token,verifiedDelta);
    }
    @Transactional(propagation=Propagation.NOT_SUPPORTED)
    boolean completeWorkerChange(String id,String operationId,String token,JsonNode verifiedDelta) {
        return completeChange(null,id,operationId,token,verifiedDelta);
    }
    private boolean completeChange(Actor requestedActor,String id,String operationId,String token,JsonNode verifiedDelta) {
        try {
            return tx(false,()->{
                Draft draft=requestedActor==null?store.lockForRecovery(id):store.lockOwned(id,requestedActor.company,requestedActor.user);
                if(!draft.uploaded())store.caseAccess(draft.caseId(),false);
                var op=store.lockRunningChange(id,operationId,token);
                if(op==null){store.expireChange(id,operationId,failureJson("TIMEOUT",draft.revision()));return false;}
                Actor actor=requestedActor==null?workerActor(draft,op):requestedActor;
                State state=state(actor,draft);
                if(requestedActor==null) {
                    var request=json.changeRequest(new java.io.ByteArrayInputStream(op.request().getBytes(StandardCharsets.UTF_8)));
                    InputWorkbookEditor.validate(state.source,state.history,request,draft.revision());
                    InputWorkbookEditor.validateHistory(source(actor,draft),store.revisions(id),json);
                }
                var applied=state.history.apply(json.delta(json.deltaText(verifiedDelta)),op.request(),op.id(),op.baseRevision()+1);
                boolean completed=store.completeChange(id,operationId,token,applied.delta(),applied.result(),applied.rows());
                if(!completed)store.expireChange(id,operationId,failureJson("TIMEOUT",draft.revision()));
                return completed;
            });
        } catch(RuntimeException ex) {
            if(requestedActor!=null&&ex instanceof InputDraftException error&&error.getStatus().value()==404)throw error;
            String reason=ex instanceof InputDraftException error&&"SOURCE_CHANGED".equals(error.getReason())?"SOURCE_CHANGED":"PERSISTENCE_FAILED";
            // commit 응답 유실도 여기로 올 수 있다. 쓰기 DB 잠금으로 성공 여부를 먼저 재확인한다.
            return tx(false,()->{
                Draft draft=requestedActor==null?store.lockForRecovery(id):store.lockOwned(id,requestedActor.company,requestedActor.user);
                if(requestedActor!=null)if(!draft.uploaded())store.caseAccess(draft.caseId(),false);
                var op=store.operation(id,operationId,true);
                if(op.status().equals("SUCCEEDED"))return token!=null&&token.equals(op.token());
                if(op.terminal()||!op.status().equals("RUNNING")||token==null||!token.equals(op.token()))return false;
                op=store.expireChange(id,operationId,failureJson("TIMEOUT",draft.revision()));
                if(!op.terminal())store.failStoppedChange(id,operationId,token,failureJson(reason,draft.revision()));
                return false;
            });
        }
    }
    private OperationView operationView(ModelInputDraftRepository.Operation op) {
        if(op.schemaVersion()!=2)throw InputDraftException.internal();
        String label=switch(op.status()){case "QUEUED"->"대기 중";case "RUNNING"->"처리 중";case "SUCCEEDED"->"저장 완료";case "FAILED"->"저장 실패";default->throw InputDraftException.internal();};
        Long resultRevision=null;List<JsonNode> mappings=List.of(),columnMappings=List.of();List<String> affected=List.of();Problem failure=null;
        if(op.status().equals("SUCCEEDED")) {
            JsonNode result=json.payload(op.result());
            if(!result.isObject()||result.size()!=4||!result.path("resultRevision").isIntegralNumber()||!result.path("resultRevision").canConvertToLong()||result.path("resultRevision").longValue()!=op.baseRevision()+1
                ||!result.path("columnMappings").isArray()||!result.path("rowMappings").isArray()||!result.path("affectedTableIds").isArray())throw InputDraftException.internal();
            validateMappings(result.path("rowMappings"),false);validateMappings(result.path("columnMappings"),true);
            resultRevision=result.path("resultRevision").longValue();
            List<JsonNode> rowList=new ArrayList<>();result.path("rowMappings").forEach(rowList::add);mappings=List.copyOf(rowList);
            List<JsonNode> columnList=new ArrayList<>();result.path("columnMappings").forEach(columnList::add);columnMappings=List.copyOf(columnList);
            List<String> tableList=new ArrayList<>();for(JsonNode table:result.path("affectedTableIds")){if(!table.isTextual())throw InputDraftException.internal();tableList.add(table.asText());}affected=List.copyOf(tableList);
        } else if(op.status().equals("FAILED")) {
            JsonNode problem=json.payload(op.failure());
            if(!Set.of("VALIDATION_FAILED","CALCULATION_FAILED","STRUCTURE_VALIDATION_FAILED","PRESERVATION_FAILED","PERSISTENCE_FAILED","TIMEOUT","SOURCE_CHANGED","POLICY_INCOMPLETE","POLICY_UNSUPPORTED","REVISION_CONFLICT").contains(problem.path("reason").asText())
                ||!problem.has("currentRevision")||!problem.path("issues").isArray())throw InputDraftException.internal();
            List<Object> issues=new ArrayList<>();problem.path("issues").forEach(issues::add);
            failure=new Problem(problem.path("reason").asText(),problem.path("currentRevision").isNull()?null:problem.path("currentRevision").longValue(),List.copyOf(issues));
        }
        return new OperationView(op.id(),op.requestId(),op.draftId(),op.status(),label,op.baseRevision(),resultRevision,mappings,columnMappings,affected,failure,
            "/api/v1/mi/fundamental-model/input-drafts/"+op.draftId()+"/change-requests/"+op.id());
    }
    private void validateMappings(JsonNode mappings,boolean column) {
        Set<String> clients=new HashSet<>(),identities=new HashSet<>(),commands=new HashSet<>();
        String clientKey=column?"clientColumnKey":"clientRowKey",identityKey=column?"fieldKey":"rowId",stateKey=column?"columnState":"rowState";
        for(JsonNode mapping:mappings) {
            if(!mapping.isObject()||mapping.size()!=5||!mapping.path("tableId").isTextual()||mapping.path("tableId").asText().isBlank()
                ||!mapping.path("commandId").asText().matches("[A-Za-z][A-Za-z0-9_-]{0,63}")
                ||!mapping.path(clientKey).asText().matches("[A-Za-z][A-Za-z0-9_-]{0,63}")
                ||!mapping.path(identityKey).asText().matches(column?"[A-Za-z][A-Za-z0-9_-]{0,63}":"[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")
                ||!Set.of("ACTIVE","DELETED").contains(mapping.path(stateKey).asText())
                ||!clients.add(mapping.path(clientKey).asText())||!commands.add(mapping.path("commandId").asText())
                ||!identities.add(mapping.path("tableId").asText()+"/"+mapping.path(identityKey).asText()))throw InputDraftException.internal();
        }
    }
    private List<Section> sections(Source source) {
        List<Section> result=new ArrayList<>();
        for(String role:ROLES) {
            if(!source.inputIds.containsKey(role))continue;
            Map<String,List<TableInfo>> sheets=new LinkedHashMap<>();
            for(TableRef table:source.catalog.tables())if(table.role().equals(role)) {
                InputReadDefinition def=source.definitions.get(table.id());List<String> controls=new ArrayList<>();def.controls().forEach(c->controls.add(c.path("id").asText()));
                sheets.computeIfAbsent(table.sheet(),k->new ArrayList<>()).add(new TableInfo(table.key(),role,table.sheet(),table.name(),table.description(),
                    cell(table.startRow(),table.startColumn())+":"+cell(table.endRow(),table.endColumn()),table.rowAdd()?"추가 가능":"고정",table.columnAdd()?"추가 가능":"고정",
                    new Coordinates(def.headerRows().isEmpty()?0:def.headerRows().get(0),def.bodyRanges().isEmpty()?table.dataStart():def.bodyRanges().get(0).startRow()),List.copyOf(controls)));
            }
            result.add(new Section(role,source.inputIds.get(role),sheets.entrySet().stream().map(e->new Sheet(e.getKey(),List.copyOf(e.getValue()))).toList()));
        }
        return List.copyOf(result);
    }
    private static String cell(int row,int col){return CellReference.convertNumToColString(col-1)+row;}
    @Transactional(propagation=Propagation.NOT_SUPPORTED)
    public SourceStructure sourceStructure(long caseId) {
        Actor actor=actor();Source source=tx(true,()->source(actor,caseId,false));
        // sourceVersion 진입점도 네 객체의 존재/표 소속을 확인한다.
        for(FileRef file:source.files) {
            List<Window> windows=source.catalog.tables().stream().filter(t->t.role().equals(file.role())).map(t->new Window(t.sheet(),row->false,Set.<Integer>of(),(row,cells)->{})).toList();
            reader.read(file,actor.companyCode,windows);
        }
        Source finalSource=tx(true,()->source(actor,caseId,false));if(!source.version.equals(finalSource.version))throw InputDraftException.conflict("SOURCE_CHANGED",null);
        return new SourceStructure(source.key,source.version,sections(source),null);
    }
    @Transactional(propagation=Propagation.NOT_SUPPORTED)
    public Structure structure(String id) {
        Actor actor=actor();
        return tx(false,()->{
            // 포인터와 작업을 읽는 동안 timeout/종료 복구가 같은 Draft를 바꾸지 못하게 한다.
            Draft draft=store.lockOwned(id,actor.company,actor.user);
            State state=state(actor,draft);
            return structure(state);
        });
    }
    @Transactional(propagation=Propagation.NOT_SUPPORTED)
    public Table table(String id,long revision,String key,Integer first) {
        Actor actor=actor();State state=tx(false,()->state(actor,id));
        if(revision!=state.draft.revision())throw InputDraftException.conflict("REVISION_CONFLICT",state.draft.revision());
        Table result=renderTable(actor,state,key,first);
        State current=tx(false,()->state(actor,id));
        if(current.draft.revision()!=revision)throw InputDraftException.conflict("REVISION_CONFLICT",current.draft.revision());
        if(!java.util.Objects.equals(state.source.version,current.source.version))throw InputDraftException.conflict("SOURCE_CHANGED",revision);
        return result;
    }
    private Table renderTable(Actor actor,State state,String key,Integer first) {return renderTable(actor,state,key,first,InputDraftJson.RESPONSE_LIMIT);}
    private Table renderTable(Actor actor,State state,String key,Integer first,int limit) {
        TableRef table=state.source.catalog.tables().stream().filter(t->t.key().equals(key)).findFirst().orElseThrow(InputDraftException::missing);
        InputReadDefinition def=state.source.definitions.get(table.id());List<Integer> candidate=def.page(first,2000),headers=def.headerRows();
        Map<Integer,Map<Integer,Cell>> rows=new HashMap<>();Set<Integer> wanted=new HashSet<>(candidate);wanted.addAll(headers);
        long[] budget={0};int[] cutoff={Integer.MAX_VALUE};
        Set<Integer> invalidRows=new HashSet<>(),invalidControls=new HashSet<>();
        FileRef file=state.source.files.stream().filter(f->f.role().equals(table.role())).findFirst().orElseThrow(InputDraftException::internal);
        List<Window> windows=new ArrayList<>();
        Map<Integer,List<Integer>> sourceRows=new HashMap<>();
        for(int row:wanted){Integer original=state.history.sourceRow(table.id(),row);if(original!=null)sourceRows.computeIfAbsent(original,k->new ArrayList<>()).add(row);}
        windows.add(new Window(table.sheet(),row->sourceRows.containsKey(row)&&sourceRows.get(row).stream().anyMatch(current->headers.contains(current)||current<=cutoff[0]),state.history.sourceColumns(table.id(),def.columns()),(original,sourceValues)->{
            Map<Integer,Cell> values=state.history.projectSource(table.id(),sourceValues);
            for(int row:sourceRows.get(original)) {
                if(!headers.contains(row)&&row>cutoff[0])continue;
                budget[0]+=json.bytes(values).length;
                if(budget[0]>InputDraftJson.RESPONSE_LIMIT&&!headers.contains(row)&&candidate.contains(row)&&!candidate.isEmpty()&&row!=candidate.get(0)){cutoff[0]=Math.min(cutoff[0],row-1);continue;}
                rows.computeIfAbsent(row,k->new HashMap<>()).putAll(values);
            }
        },original->invalidRows.addAll(sourceRows.get(original))));
        for(JsonNode control:def.controls()) {
            int boundRow=control.path("binding").path("rowNumber").asInt(),boundColumn=control.path("binding").path("columnIndex").asInt()+1;
            JsonNode original=state.history.originalControl(table.id(),control.path("id").asText());
            int sourceRow=original.path("binding").path("rowNumber").asInt(),sourceCol=original.path("binding").path("columnIndex").asInt()+1;
            windows.add(new Window(table.sheet(),row->row==sourceRow,Set.of(sourceCol),
                (row,values)->{if(values.containsKey(sourceCol))rows.computeIfAbsent(boundRow,k->new HashMap<>()).put(boundColumn,values.get(sourceCol));},invalidControls::add));
        }
        reader.read(file,actor.companyCode,windows);
        for(int row:wanted)state.history.overlay(table.id(),row,rows.computeIfAbsent(row,k->new HashMap<>()));
        for(JsonNode control:def.controls()) {
            int row=control.path("binding").path("rowNumber").asInt(),col=control.path("binding").path("columnIndex").asInt()+1;
            Cell value=state.history.control(control.path("sourceTableId").asText(),row,col);
            if(value!=null)rows.computeIfAbsent(row,k->new HashMap<>()).put(col,value);
        }
        List<Integer> page=new ArrayList<>(candidate.stream().filter(row->row<=cutoff[0]).toList());
        Map<Integer,String> identities=state.history.bindings(table.id());
        Table result;
        while(true) {
            result=project(state,table,def,page,rows,identities,invalidRows);
            if(json.bytes(BaseResponseFactory.create(result)).length<=limit)break;
            if(page.size()<=1)throw InputDraftException.responseLarge();page=new ArrayList<>(page.subList(0,Math.max(1,page.size()/2)));
        }
        // 크기로 정한 범위만 검사한다. 오류 때문에 행을 건너뛰거나 페이지를 줄이지 않는다.
        if(!invalidControls.isEmpty()||invalidRows.stream().anyMatch(headers::contains)
            ||page.stream().anyMatch(invalidRows::contains))throw InputDraftException.internal();
        return result;
    }
    @io.swagger.v3.oas.annotations.media.Schema(requiredProperties={"kind","draftId","revision","draftEndpoint","structure","selection"})
    public record UploadResult(String kind,String draftId,long revision,String draftEndpoint,Structure structure,
        @io.swagger.v3.oas.annotations.media.Schema(nullable=true,types={"object","null"},schemaResolution=io.swagger.v3.oas.annotations.media.Schema.SchemaResolution.INLINE) Table selection) {}
    UploadResult previewUpload(Actor actor,Draft draft,Catalog catalog,List<FileRef> files,String sourceKey,
            List<Revision> chain,List<RowIdentity> identities,List<RowPosition> positions,String kind) {
        Source source=uploadSource(catalog,files,sourceKey);
        InputDraftHistory history=InputDraftHistory.restore(draft,source.catalog,source.definitions,chain,identities,positions,json);
        State state=new State(draft,new Source(null,history.catalog(),source.files,source.key,null,source.inputIds,history.definitions()),history);
        var first=state.source.catalog.tables().stream().filter(t->t.role().equals(kind)).findFirst().orElse(null);
        Structure structure=structure(state);
        int budget=InputDraftJson.BODY_LIMIT-json.bytes(structure).length-4096;
        if(budget<1)throw InputDraftException.responseLarge();
        Table selection=first==null?null:renderTable(actor,state,first.key(),null,budget);
        return new UploadResult(kind,draft.id(),draft.revision(),"/api/v1/mi/fundamental-model/input-drafts/"+draft.id(),structure,selection);
    }
    private Table project(State state,TableRef table,InputReadDefinition def,List<Integer> page,Map<Integer,Map<Integer,Cell>> values,Map<Integer,String> ids,Set<Integer> invalidRows) {
        List<Column> columns=def.columns().stream().map(col->new Column("column"+(col-1),CellReference.convertNumToColString(col-1))).toList();
        List<Map<String,Object>> grid=new ArrayList<>();List<Long> inputs=new ArrayList<>();List<RowBinding> bindings=new ArrayList<>();List<EditValue> edits=new ArrayList<>();
        Set<Integer> returned=new java.util.TreeSet<>(def.headerRows());returned.addAll(page);
        for(int row:returned) {
            Map<Integer,Cell> cells=values.getOrDefault(row,Map.of());Map<String,Object> record=new LinkedHashMap<>();record.put("id",row-1);record.put("rowNumber",row);
            for(int col:def.columns()){Cell value=cells.get(col);if(value!=null&&value.display()!=null)record.put("column"+(col-1),value.display());}
            grid.add(record);
            if(!page.contains(row))continue;
            String rowId=ids.get(row);
            if(rowId==null&&def.identityColumns().stream().map(cells::get).anyMatch(value->value!=null&&value.raw()!=null
                &&(!(value.raw() instanceof String text)||!text.isBlank())))invalidRows.add(row);
            bindings.add(new RowBinding(row,rowId,rowId==null?"EMPTY_SLOT":state.history.origin(rowId)));Map<String,Object> typed=new LinkedHashMap<>();
            for(JsonNode field:def.fields()) {
                int col=field.path("columnNumber").asInt();Cell value=cells.get(col);
                boolean editable="READY".equals(field.path("policyStatus").path("SET").asText())&&table.scopes().stream().anyMatch(scope->scope.contains(row,col));
                if(editable&&(value==null||!value.formula()))inputs.add((long)row*16384+(col-1));
                if(editable&&rowId!=null&&value!=null&&value.raw()!=null&&!value.formula()) {
                    try {typed.put(field.path("fieldKey").asText(),typed(field.path("dataType").asText(),value.raw()));}
                    catch(InputDraftException ex){invalidRows.add(row);}
                }
            }
            if(rowId!=null)edits.add(new EditValue(rowId,Map.copyOf(typed)));
        }
        List<JsonNode> fields=new ArrayList<>();for(JsonNode original:def.fields()){ObjectNode field=original.deepCopy();int col=field.remove("columnNumber").asInt();field.put("columnField","column"+(col-1));if("READY".equals(field.path("policyStatus").path("SET_HEADER").asText())&&!(changesEnabled&&InputWorkbookPolicy.supportsHeader(table,col)))((ObjectNode)field.path("policyStatus")).put("SET_HEADER","UNSUPPORTED");fields.add(field);}
        Map<String,String> caps=new LinkedHashMap<>();def.capabilities().fields().forEachRemaining(e->caps.put(e.getKey(),"READY".equals(e.getValue().asText())
            &&!(changesEnabled&&InputWorkbookEditor.supports(table,def,e.getKey()))?"UNSUPPORTED":e.getValue().asText()));
        List<JsonNode> controls=new ArrayList<>();for(JsonNode original:def.controls()){ObjectNode control=original.deepCopy();int row=control.path("binding").path("rowNumber").asInt(),col=control.path("binding").path("columnIndex").asInt()+1;Cell cell=values.getOrDefault(row,Map.of()).get(col);control.set("value",json.tree(cell==null?null:cell.raw()));control.put("editable",original.path("editable").asBoolean()&&"READY".equals(caps.get("SET"))&&InputWorkbookPolicy.supportsControl(table,original));controls.add(control);}
        Integer start=page.isEmpty()?null:page.get(0),end=page.isEmpty()?null:page.get(page.size()-1),next=null;
        if(end!=null) {List<Integer> after=def.page(end,2);if(after.size()>1)next=after.get(1);}
        return new Table(state.draft.id(),state.draft.revision(),state.source.key,state.source.version,
            new TableData(table.key(),columns,grid,inputs,def.inputGuideRules(),controls,def.displayUpdateRules()),
            new Page(def.totalRows(),page.size(),start,end,next!=null,next),new Editing(caps,fields,bindings,edits,caps.get("ADD_COLUMN").equals("READY")?def.columnCreation():null));
    }
    private Object typed(String type,Object value) {
        if("TEXT".equals(type)&&value instanceof String)return value;
        if(value instanceof BigDecimal number){if("DECIMAL".equals(type))return number;if("INTEGER".equals(type))try{return number.toBigIntegerExact();}catch(ArithmeticException ex){throw InputDraftException.internal();}}
        throw InputDraftException.internal();
    }
}
