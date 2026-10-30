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
    @org.springframework.beans.factory.annotation.Value("${mi.model-input.restore.cursor-secret:}")
    private String changeCursorSecret="";
    @org.springframework.beans.factory.annotation.Value("${mi.model-input.restore-preview.enabled:false}")
    private boolean restorePreviewEnabled;
    private InputTempFiles restoreScratch;
    @org.springframework.beans.factory.annotation.Autowired
    void restoreScratch(InputTempFiles scratch){this.restoreScratch=scratch;}
    private java.util.concurrent.Semaphore restorePreviewSlot=new java.util.concurrent.Semaphore(1);
    private int previewTimeoutMillis=30_000;
    private long previewScratchBytes=256L*1024*1024;
    @org.springframework.beans.factory.annotation.Autowired
    void previewLimits(@org.springframework.beans.factory.annotation.Value("${mi.model-input.restore-preview.max-concurrent:1}") int concurrent,
            @org.springframework.beans.factory.annotation.Value("${mi.model-input.restore-preview.timeout-ms:30000}") int timeout,
            @org.springframework.beans.factory.annotation.Value("${mi.model-input.restore-preview.scratch-bytes:268435456}") long bytes) {
        if(concurrent<=0||timeout<=0||bytes<=0)throw new IllegalArgumentException("positive preview limits required");
        restorePreviewSlot=new java.util.concurrent.Semaphore(concurrent);previewTimeoutMillis=timeout;previewScratchBytes=bytes;
    }
    @Transactional(propagation=Propagation.NOT_SUPPORTED)
    public RestorePreview restorePreview(String id,long revision,String tableId,String kind,String target,Integer first) {
        long started=System.nanoTime();Actor actor=actor();var basis=comparisonBasis(actor,id,revision,tableId);
        if(kind==null||!Set.of("ROW","COLUMN").contains(kind)||target==null||!(kind.equals("ROW")?target.matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"):target.matches("[A-Za-z][A-Za-z0-9_-]{0,63}")))throw InputDraftException.invalid();
        if(!restorePreviewEnabled||!restoresEnabled||!changesEnabled||restoreScratch==null)throw InputDraftException.conflict("POLICY_UNSUPPORTED",revision);
        if(!restorePreviewSlot.tryAcquire())throw InputDraftException.previewBusy(revision);
        try(var scope=restoreScratch.beginTask()) {
            long deadline=started+java.util.concurrent.TimeUnit.MILLISECONDS.toNanos(previewTimeoutMillis);
            Runnable check=readBudget(scope,deadline,revision,true);
            java.util.function.IntSupplier remaining=()->{check.run();return (int)Math.max(1,java.util.concurrent.TimeUnit.NANOSECONDS.toMillis(deadline-System.nanoTime()));};
            scope.checkpoint(check,remaining);check.run();
            ObjectNode command=(ObjectNode)json.tree(Map.of("type","RESTORE_"+kind,"commandId","preview","tableId",tableId));command.put(kind.equals("ROW")?"rowId":"fieldKey",target);
            var request=new InputDraftJson.ChangeRequest(UUID.randomUUID().toString(),revision,List.of(command));
            JsonNode delta=new InputWorkbookEditor(reader,restoreScratch,json,check::run,remaining).execute(basis,actor.companyCode,request);
            InputDraftHistory history=basis.history(revision,json);history.apply(delta,new String(json.changeBytes(request),StandardCharsets.UTF_8),UUID.randomUUID().toString(),revision+1);
            Source old=basis.source();Source virtual=new Source(old.caseId,history.catalog(),old.files,old.key,old.version,old.inputIds,history.definitions());
            Table result=renderTable(actor,new State(basis.draft().atRevision(revision+1),virtual,history),tableId,first,InputDraftJson.RESPONSE_LIMIT-256);check.run();verifyComparison(actor,basis);
            return new RestorePreview(id,tableId,revision,kind,target,true,result.tableData(),result.page(),result.editing());
        }catch(InputDraftException ex){throw ex;}
        catch(InputDraftPreserveChange.Failure ex){throw InputDraftException.previewUnavailable(revision);}
        catch(Exception ex){throw InputDraftException.internal();}
        finally{restorePreviewSlot.release();}
    }


    private InputDraftEditingBasis comparisonBasis(Actor actor,String id,long revision,String table) {
        return tx(false,()->{
            Draft draft=lockSnapshot(actor,id);Source source=checkedSource(actor,draft);
            if(draft.revision()!=revision)throw InputDraftException.conflict("REVISION_CONFLICT",draft.revision());
            TableRef selected=source.catalog.tables().stream().filter(t->t.key().equals(table)).findFirst().orElseThrow(InputDraftException::missing);
            if(!source.definitions.get(selected.id()).navigationVisible())throw InputDraftException.missing();
            if(source.files.stream().noneMatch(file->file.role().equals(selected.role())))throw InputDraftException.conflict("FILES_INCOMPLETE",draft.revision());
            return loadBasis(draft,source);
        });
    }
    private void verifyComparison(Actor actor,InputDraftEditingBasis basis) {
        tx(false,()->{Draft current=lockSnapshot(actor,basis.draft().id());Source source=checkedSource(actor,current);
            if(current.revision()!=basis.draft().revision())throw InputDraftException.conflict("REVISION_CONFLICT",current.revision());
            if(!source.files.equals(basis.source().files)||!java.util.Objects.equals(source.version,basis.source().version))throw InputDraftException.conflict("SOURCE_CHANGED",current.revision());return null;});
    }
    private record ComparisonValues(Map<String,Map<String,Object>> rows,Map<String,JsonNode> fields,Map<String,JsonNode> controls,Map<String,Object> headers) {}
    private ComparisonValues comparisonValues(State state,TableRef selected,Map<Integer,Map<Integer,Cell>> sourceCells) {
        Map<String,Map<String,Object>> rows=new HashMap<>();Map<String,JsonNode> fields=new LinkedHashMap<>(),controls=new LinkedHashMap<>();
        var definition=state.history.definitions().get(selected.id());definition.fields().forEach(f->fields.put(f.path("fieldKey").asText(),f));
        for(var binding:state.history.rowStates(selected.id()))if(binding.position()!=null) {
            Integer source=state.history.sourceRow(selected.id(),binding.position());Map<Integer,Cell> cells=state.history.projectSource(selected.id(),source==null?Map.of():sourceCells.getOrDefault(source,Map.of()));state.history.overlay(selected.id(),binding.position(),cells);
            Map<String,Object> values=new LinkedHashMap<>();
            for(JsonNode field:definition.fields()) {
                int col=field.path("columnNumber").asInt();Cell cell=cells.get(col);
                if("READY".equals(field.path("policyStatus").path("SET").asText())&&selected.scopes().stream().anyMatch(scope->scope.contains(binding.position(),col))&&cell!=null&&!cell.formula()&&cell.raw()!=null)values.put(field.path("fieldKey").asText(),typed(field.path("dataType").asText(),cell.raw()));
            }
            rows.put(binding.id(),values);
        }
        for(JsonNode control:definition.controls())if(control.path("editable").asBoolean()&&InputWorkbookPolicy.supportsControl(selected,control)) {
            var original=state.history.originalControl(selected.id(),control.path("id").asText());int col=control.path("binding").path("columnIndex").asInt()+1,row=control.path("binding").path("rowNumber").asInt();
            Cell cell=state.history.control(control.path("sourceTableId").asText(),row,col);
            if(cell==null)cell=sourceCells.getOrDefault(original.path("binding").path("rowNumber").asInt(),Map.of()).get(original.path("binding").path("columnIndex").asInt()+1);
            ObjectNode value=control.deepCopy();value.set("value",json.tree(cell==null?null:cell.raw()));controls.put(control.path("id").asText(),value);
        }
        Map<String,Object> headers=new HashMap<>();Map<Integer,Cell> current=state.history.projectSource(selected.id(),sourceCells.getOrDefault(3,Map.of()));state.history.overlay(selected.id(),3,current);
        for(JsonNode field:definition.fields())if(field.path("policyStatus").path("SET_HEADER").asText().equals("READY")) {
            Cell cell=current.get(field.path("columnNumber").asInt());headers.put(field.path("fieldKey").asText(),cell==null?null:cell.raw());
        }
        return new ComparisonValues(rows,fields,controls,headers);
    }
    @Transactional(propagation=Propagation.NOT_SUPPORTED)
    public Changes changes(String id,long revision,String tableId,String cursor) {
        actor();if(restoreScratch==null)throw InputDraftException.conflict("POLICY_UNSUPPORTED",revision);
        if(!restorePreviewSlot.tryAcquire())throw InputDraftException.comparisonBusy(revision);
        try(var scope=restoreScratch.beginTask()) {
            long deadline=System.nanoTime()+java.util.concurrent.TimeUnit.MILLISECONDS.toNanos(previewTimeoutMillis);
            Runnable check=readBudget(scope,deadline,revision,false);scope.checkpoint(check,()->{check.run();return (int)Math.max(1,java.util.concurrent.TimeUnit.NANOSECONDS.toMillis(deadline-System.nanoTime()));});
            return changesCurrent(id,revision,tableId,cursor);
        }catch(InputDraftException ex){throw ex;}catch(InputDraftPreserveChange.Failure ex){throw InputDraftException.comparisonUnavailable(revision);}catch(Exception ex){throw InputDraftException.internal();}finally{restorePreviewSlot.release();}
    }
    private Runnable readBudget(InputTempFiles.Scope scope,long deadline,long revision,boolean preview) {
        long[] diskCheck={0};return ()->{long now=System.nanoTime();if(Thread.currentThread().isInterrupted()||now>=deadline)throw preview?InputDraftException.previewUnavailable(revision):InputDraftException.comparisonUnavailable(revision);
            if(now>=diskCheck[0])try {diskCheck[0]=now+10_000_000;if(scope.sizeBytes()>previewScratchBytes)throw preview?InputDraftException.previewUnavailable(revision):InputDraftException.comparisonUnavailable(revision);}catch(java.io.IOException ex){throw InputDraftException.internal();}};
    }
    private Changes changesCurrent(String id,long revision,String tableId,String cursor) {
        Actor actor=actor();var basis=comparisonBasis(actor,id,revision,tableId);var tokens=new InputDraftChangeCursor(changeCursorSecret);State current=restore(basis);
        TableRef selected=current.source.catalog.tables().stream().filter(t->t.key().equals(tableId)).findFirst().orElseThrow(InputDraftException::missing);
        FileRef file=basis.source().files.stream().filter(f->f.role().equals(selected.role())).findFirst().orElseThrow(InputDraftException::internal);
        String scope=id+"\n"+tableId+"\n"+revision+"\n"+file.baseline()+"\n"+hash("cf_",file);int offset=tokens.decode(cursor,scope);
        long baseline=file.baseline();InputDraftHistory original=basis.history(baseline,json);
        Source source=new Source(basis.source().caseId,original.catalog(),basis.source().files,basis.source().key,basis.source().version,basis.source().inputIds,original.definitions());
        Map<Integer,Map<Integer,Cell>> sourceCells=new HashMap<>();var originalDefinition=original.definitions().get(selected.id());Set<Integer> sourceColumns=new HashSet<>(originalDefinition.columns()),controlRows=new HashSet<>();
        for(JsonNode control:originalDefinition.controls()){controlRows.add(control.path("binding").path("rowNumber").asInt());sourceColumns.add(control.path("binding").path("columnIndex").asInt()+1);}
        int last=Math.max(original.catalog().tables().stream().filter(t->t.id()==selected.id()).findFirst().orElseThrow().endRow(),controlRows.stream().mapToInt(Integer::intValue).max().orElse(1));
        long[] comparedCells={0};
        reader.read(file,actor.companyCode,List.of(new Window(selected.sheet(),row->originalDefinition.body(row)||originalDefinition.headerRows().contains(row)||controlRows.contains(row),sourceColumns,(row,cells)->{InputTempFiles.checkpoint();sourceCells.put(row,cells);if((comparedCells[0]+=cells.size())>250000)throw InputDraftException.comparisonUnavailable(revision);},row->{throw InputDraftException.internal();},last)));
        ComparisonValues before=comparisonValues(new State(basis.draft().atRevision(baseline),source,original),original.catalog().tables().stream().filter(t->t.id()==selected.id()).findFirst().orElseThrow(),sourceCells);
        ComparisonValues after=comparisonValues(current,selected,sourceCells);List<JsonNode> items=new ArrayList<>();
        Map<String,Map<String,String>> references=new HashMap<>();
        boolean structural=current.history.rowStates(selected.id()).stream().anyMatch(r->r.original()!=null&&r.position()==null)||current.history.columnStates(selected.id()).stream().anyMatch(c->c.source()!=null&&c.position()==null&&c.field()!=null);
        if(restoresEnabled&&restorePreviewEnabled&&structural)reader.withLocal(file,actor.companyCode,path->{
            try {Set<String> sheets=new HashSet<>();for(TableRef related:current.history.catalog().tables())if(related.role().equals(file.role())&&(related.id()==selected.id()||InputDraftPmiRows.user(selected)&&InputDraftPmiRows.child(related)||InputWorkbookPolicy.kind(selected)==InputWorkbookPolicy.Kind.REGAS&&related.sheet().equals("RegasAdjustment")))sheets.add(related.sheet());
                for(String sheet:sheets)references.put(sheet,InputWorkbookStructure.formulas(path,sheet,a->true,InputTempFiles::checkpoint));return null;
            }catch(InputDraftException ex){throw ex;}catch(Exception ex){throw InputDraftException.internal();}});

        for(var row:current.history.rowStates(selected.id())) {
            String change=row.original()==null?(row.position()==null?null:"ADDED"):(row.position()==null?"DELETED":null);if(change==null)continue;
            ObjectNode item=changeItem("ROW",change);item.put("rowId",row.id()).put("origin",row.original()==null?"ADDED":"SOURCE");item.set("originalRowNumber",json.tree(row.original()));
            JsonNode command=row.original()==null?json.tree(Map.of("type","DELETE_ROW","commandId","revert","tableId",tableId,"rowRef",Map.of("rowId",row.id()))):json.tree(Map.of("type","RESTORE_ROW","commandId","revert","tableId",tableId,"rowId",row.id()));
            structureRestorationStatus(item,current,command,before,references,basis,file);items.add(item);
        }
        for(var column:current.history.columnStates(selected.id()))if(column.field()!=null) {
            String change=column.source()==null?(column.position()==null?null:"ADDED"):(column.position()==null?"DELETED":null);if(change==null)continue;
            ObjectNode item=changeItem("COLUMN",change);item.put("fieldKey",column.field()).put("origin",column.source()==null?"ADDED":"SOURCE");item.set("originalColumnNumber",json.tree(column.source()));
            JsonNode command=json.tree(Map.of("type",column.source()==null?"DELETE_COLUMN":"RESTORE_COLUMN","commandId","revert","tableId",tableId,"fieldKey",column.field()));
            structureRestorationStatus(item,current,command,before,references,basis,file);items.add(item);
        }
        Set<String> sourceRows=new HashSet<>();current.history.rowStates(selected.id()).stream().filter(r->r.original()!=null&&r.position()!=null).forEach(r->sourceRows.add(r.id()));
        Set<String> sourceFields=new HashSet<>();current.history.columnStates(selected.id()).stream().filter(c->c.source()!=null&&c.position()!=null&&c.field()!=null).forEach(c->sourceFields.add(c.field()));
        for(String row:sourceRows)for(String field:sourceFields) {
            InputTempFiles.checkpoint();
            if(!after.fields.containsKey(field)||!before.fields.containsKey(field))continue;
            Object old=before.rows.getOrDefault(row,Map.of()).get(field),now=after.rows.getOrDefault(row,Map.of()).get(field);
            if(json.sameValue(json.tree(old),json.tree(now)))continue;
            ObjectNode item=changeItem("CELL","MODIFIED");item.put("rowId",row).put("fieldKey",field);item.set("baseline",comparisonValue(old));item.set("current",comparisonValue(now));
            ObjectNode command=(ObjectNode)json.tree(Map.of("type",old==null?"CLEAR":"SET","commandId","revert","tableId",tableId,"rowRef",Map.of("rowId",row),"fieldKey",field));if(old!=null)command.set("value",json.tree(old));
            restorationStatus(item,current,command);items.add(item);
        }
        for(String field:sourceFields)if(after.headers.containsKey(field)||before.headers.containsKey(field)) {
            Object old=before.headers.get(field),now=after.headers.get(field);if(json.sameValue(json.tree(old),json.tree(now)))continue;
            ObjectNode item=changeItem("HEADER","MODIFIED");item.put("fieldKey",field).put("headerKey","year");item.set("baseline",comparisonValue(old));item.set("current",comparisonValue(now));
            if(old==null)item.put("restoreStatus","NOT_ALLOWED").put("restoreReason","BASELINE_VALUE_NOT_ALLOWED");else restorationStatus(item,current,json.tree(Map.of("type","SET","commandId","revert","tableId",tableId,"fieldKey",field,"headerKey","year","value",old)));items.add(item);
        }
        for(var entry:after.controls.entrySet()) {
            JsonNode old=before.controls.get(entry.getKey());if(old==null)throw InputDraftException.internal();
            JsonNode now=entry.getValue().path("value");if(json.sameValue(old.path("value"),now))continue;
            ObjectNode item=changeItem("CONTROL","MODIFIED");item.put("controlId",entry.getKey()).put("sourceTableId",entry.getValue().path("sourceTableId").asText()).put("sourceControlId",entry.getKey());
            item.set("baseline",comparisonValue(json.scalarValue(old.path("value"))));item.set("current",comparisonValue(json.scalarValue(now)));
            if(old.path("value").isNull())item.put("restoreStatus","NOT_ALLOWED").put("restoreReason","BASELINE_VALUE_NOT_ALLOWED");else restorationStatus(item,current,json.tree(Map.of("type","SET","commandId","revert","tableId",tableId,"controlId",entry.getKey(),"value",json.scalarValue(old.path("value")))));items.add(item);
        }
        List<String> order=List.of("ROW","COLUMN","CELL","HEADER","CONTROL");items.sort(java.util.Comparator.comparingInt((JsonNode item)->order.indexOf(item.path("kind").asText())).thenComparing(item->item.path("rowId").asText()).thenComparing(item->item.path("fieldKey").asText()).thenComparing(item->item.path("headerKey").asText()).thenComparing(item->item.path("controlId").asText()));
        if(offset>items.size())throw InputDraftException.invalid();
        List<JsonNode> page=new ArrayList<>();int next=offset;long bytes=json.bytes(BaseResponseFactory.create(new Changes(id,tableId,revision,List.of(),null))).length;
        while(next<items.size()&&page.size()<2000) {InputTempFiles.checkpoint();JsonNode item=items.get(next);String tail=next+1<items.size()?tokens.encode(scope,next+1):null;
            long added=json.bytes(item).length+(page.isEmpty()?0:1),cursorBytes=tail==null?0:json.bytes(tail).length-4;
            if(bytes+added+cursorBytes>InputDraftJson.RESPONSE_LIMIT){if(page.isEmpty())throw InputDraftException.responseLarge();break;}bytes+=added;page.add(item);next++;}
        Changes result=new Changes(id,tableId,revision,List.copyOf(page),next<items.size()?tokens.encode(scope,next):null);verifyComparison(actor,basis);return result;
    }
    private ObjectNode changeItem(String kind,String change){return ((ObjectNode)json.tree(Map.of("kind",kind,"change",change))).put("restoreStatus","UNSUPPORTED").put("restoreReason","POLICY_UNSUPPORTED");}
    private JsonNode comparisonValue(Object value){ObjectNode result=(ObjectNode)json.tree(Map.of("kind",value==null?"EMPTY":"VALUE"));if(value!=null)result.set("value",json.tree(value));return result;}
    private void structureRestorationStatus(ObjectNode item,State state,JsonNode command,ComparisonValues baseline,Map<String,Map<String,String>> references,InputDraftEditingBasis basis,FileRef file) {
        restorationStatus(item,state,command);if(!item.path("restoreStatus").asText().equals("READY")||!command.path("type").asText().startsWith("RESTORE_"))return;
        try {
            if(!references.containsKey(state.source.catalog.tables().stream().filter(t->t.key().equals(command.path("tableId").asText())).findFirst().orElseThrow(InputDraftException::internal).sheet()))throw new InputDraftPreserveChange.Failure("POLICY_UNSUPPORTED");
            InputWorkbookRestoration.verifyReferences(state.history,command,references,json);
            InputWorkbookRestoration.verifyCrossings(state.history,command,table->{
                boolean row=command.path("type").asText().equals("RESTORE_ROW");String target=row?command.path("rowId").asText():command.path("fieldKey").asText();
                if(row&&!table.equals(command.path("tableId").asText())) {
                    long tableId=state.history.catalog().tables().stream().filter(t->t.key().equals(table)).findFirst().orElseThrow(InputDraftException::internal).id();
                    target=state.history.rowStates(tableId).stream().filter(r->state.history.restoredRowIds(List.of(command)).contains(r.id())).map(InputDraftHistory.RowState::id).findFirst().orElseThrow(InputDraftException::internal);
                }
                long deleted=InputWorkbookEditor.deletionRevision(basis,file,table,command.path("type").asText(),target,state.draft.revision(),json);return basis.history(deleted-1,json);
            },json,state.draft.revision());
            TableRef table=state.source.catalog.tables().stream().filter(t->t.key().equals(command.path("tableId").asText())).findFirst().orElseThrow(InputDraftException::internal);
            var request=new InputDraftJson.ChangeRequest(UUID.randomUUID().toString(),state.draft.revision(),List.of(command));
            for(var row:state.history.rowStates(table.id()))if(row.original()!=null&&(command.path("type").asText().equals("RESTORE_ROW")?row.id().equals(command.path("rowId").asText()):row.position()!=null))for(JsonNode field:state.history.originalDefinition(table.id()).fields()) {
                String key=field.path("fieldKey").asText();if(!field.path("policyStatus").path("SET").asText().equals("READY"))continue;
                if(command.path("type").asText().equals("RESTORE_COLUMN")&&!key.equals(command.path("fieldKey").asText()))continue;
                if(command.path("type").asText().equals("RESTORE_ROW")&&state.history.columnStates(table.id()).stream().noneMatch(c->key.equals(c.field())&&c.position()!=null))continue;
                int col=field.path("columnNumber").asInt();if(state.history.originalTable(table.id()).scopes().stream().noneMatch(scope->scope.contains(row.original(),col)))continue;
                Object value=baseline.rows.getOrDefault(row.id(),Map.of()).get(key);
                if(value==null)InputWorkbookPolicy.policy(field.path("policyStatus").path("CLEAR").asText(),state.draft.revision(),command,request,"CLEAR_NOT_ALLOWED");
                else InputWorkbookPolicy.value(field,json.tree(value),field.path("setConstraints"),command,request,state.draft.revision());
            }
        }catch(InputDraftPreserveChange.Failure ex){item.put("restoreStatus","UNSUPPORTED").put("restoreReason","STRUCTURE_UNSUPPORTED");}
        catch(InputDraftException ex){if(ex.getStatus().is5xxServerError())throw ex;item.put("restoreStatus",ex.getStatus().value()==422?"NOT_ALLOWED":"UNSUPPORTED").put("restoreReason",ex.getStatus().value()==422?"BASELINE_VALUE_NOT_ALLOWED":"STRUCTURE_UNSUPPORTED");}
    }
    private void restorationStatus(ObjectNode item,State state,JsonNode command) {
        restorationStatus(item,state,command,request->InputWorkbookEditor.validate(state.source,state.history,request,state.draft.revision()));
    }
    private void restorationStatus(ObjectNode item,State state,JsonNode command,java.util.function.Consumer<InputDraftJson.ChangeRequest> validate) {
        if(!changesEnabled||command.path("type").asText().startsWith("RESTORE_")&&(!restoresEnabled||!restorePreviewEnabled))return;
        try {validate.accept(new InputDraftJson.ChangeRequest(UUID.randomUUID().toString(),state.draft.revision(),List.of(command)));item.put("restoreStatus","READY").putNull("restoreReason");}
        catch(InputDraftException ex){if(ex.getStatus().is5xxServerError())throw ex;String reason=ex.getReason();item.put("restoreStatus","POLICY_INCOMPLETE".equals(reason)?"INCOMPLETE":ex.getStatus().value()==422?"NOT_ALLOWED":"UNSUPPORTED");item.put("restoreReason",reason==null?"ACTION_NOT_ALLOWED":reason);}
    }

    private static final List<String> ROLES=List.of("DEMAND","SUPPLY","TRADE","OUTPUT");
    private final ModelInputDraftRepository store;
    @org.springframework.lang.Nullable private final AuthorizationService authorization;
    private final InputWorkbookReader reader;
    private final InputDraftJson json;
    private InputTableSnapshotReader tableSnapshots;
    @org.springframework.beans.factory.annotation.Autowired
    void tableSnapshots(InputTableSnapshotReader snapshots){this.tableSnapshots=snapshots;}
    private InputUploadRepository uploads;
    @org.springframework.beans.factory.annotation.Autowired
    void uploads(InputUploadRepository uploads){this.uploads=uploads;}
    // JpaConfig의 primary manager이며 JDBC도 같은 masterDataSource를 사용한다.
    private final PlatformTransactionManager transactionManager;
    @org.springframework.beans.factory.annotation.Value("${mi.model-input.change-worker.enabled:false}")
    private boolean changesEnabled;
    @org.springframework.beans.factory.annotation.Value("${mi.model-input.restore.enabled:false}")
    private boolean restoresEnabled;
    @org.springframework.beans.factory.annotation.Value("${mi.model-input.shared-baseline.enabled:false}")
    private boolean sharedBaselineEnabled;
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
    private record CaseSource(Source source,boolean completedRun) {}
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
    // Only the durable registration job supplies this actor; no public request parameter selects its tenant.
    Source snapshotSource(Actor actor,long caseId) { return tx(true,()->source(actor,caseId,false)); }
    private Source source(Actor actor,long caseId,boolean lock) {
        if(caseId<1||caseId>Integer.MAX_VALUE)throw InputDraftException.missing();
        // 공유 원본 Case는 생성자의 회사와 비교하지 않는다. 개인 Draft는 owned()에서 회사+사용자로 제한한다.
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
        Actor actor=actor();Source basis=InputDraftDiagnostics.during(InputDraftDiagnostics.Stage.CREATE_SOURCE,null,()->tx(true,()->source(actor,caseId,false)));
        Draft previous=InputDraftDiagnostics.during(InputDraftDiagnostics.Stage.CREATE_SOURCE,null,()->tx(true,()->store.active(actor.company,actor.user,caseId)));
        if(previous!=null)return InputDraftDiagnostics.during(InputDraftDiagnostics.Stage.CREATE_RESUME,null,()->tx(false,()->{
            Source current=source(actor,caseId,true);
            Draft existing=store.active(actor.company,actor.user,caseId);
            if(existing==null)throw InputDraftException.conflict("DRAFT_SOURCE_CONFLICT",previous.revision());
            existing=store.lockOwned(existing.id(),actor.company,actor.user);
            fixed(existing,current,requestedVersion);
            return new Created(summary(existing,restore(existing,current).source),false);
        }));
        if(!basis.version.equals(requestedVersion))throw InputDraftException.conflict("SOURCE_CHANGED",null);
        boolean shared=sharedBaselineEnabled;
        Baseline ready=shared?tx(true,()->{
            Baseline found=store.findBaseline(actor.company,caseId,basis.catalog.templateId(),basis.version);
            if(found!=null)store.validateBaseline(found,basis.files,initialRanges(basis));
            return found;
        }):null;
        boolean scanned=ready==null;
        List<RowRef> rows=scanned?scanInitialRows(actor,basis):List.of();
        try {
            return InputDraftDiagnostics.during(InputDraftDiagnostics.Stage.CREATE_STORE,null,()->tx(false,()->
                createCaseDraft(actor,basis,requestedVersion,shared,rows,scanned,null)));
        } catch(org.springframework.dao.DuplicateKeyException race) {
            if(!shared)throw race;
            // 전체 rollback 뒤 쓰기 DB/Case lock에서 확정된 승자를 한 번만 확인한다.
            return InputDraftDiagnostics.during(InputDraftDiagnostics.Stage.CREATE_STORE,null,()->tx(false,()->
                createCaseDraft(actor,basis,requestedVersion,true,rows,scanned,race)));
        }
    }
    private List<RowRef> scanInitialRows(Actor actor,Source basis) {
        List<RowRef> rows=new ArrayList<>();
        for(FileRef file:basis.files) {
            List<Window> windows=new ArrayList<>();
            // B:F 연결 수식을 식별 열로 발행한 정의에만 예외를 적용한다.
            // H:AE 등 비수식 식별 열을 쓰는 기존 카탈로그는 일반 행 판정을 유지한다.
            List<TableRef> regas=basis.catalog.tables().stream().filter(table->table.role().equals(file.role())
                &&InputWorkbookPolicy.kind(table)==InputWorkbookPolicy.Kind.REGAS_MONTHLY
                &&basis.definitions.get(table.id()).identityColumns().equals(List.of(2,3,4,5,6))).toList();
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
            InputDraftDiagnostics.during(InputDraftDiagnostics.Stage.CREATE_READ,file.role(),()->{
            if(regas.isEmpty()){reader.read(file,actor.companyCode,windows);return null;}
            return reader.withLocal(file,actor.companyCode,path->{
                try {
                    reader.readLocal(path,windows);
                    List<TableRef> masters=basis.catalog.tables().stream().filter(table->table.role().equals("SUPPLY")&&table.sheet().equals("BCRegasification")).toList();
                    TableRef monthly=regas.get(0);
                    if(masters.size()!=1)throw InputDraftException.conflict("POLICY_INCOMPLETE",null);
                    TableRef master=masters.get(0);
                    Set<Integer> masterRows=rows.stream().filter(row->row.table()==master.id()).map(RowRef::row).collect(java.util.stream.Collectors.toSet());
                    for(int row:InputWorkbookRegas.linkedSourceRows(path,master,monthly,masterRows,()->{})) {
                        if(!basis.definitions.get(monthly.id()).body(row))throw InputDraftException.conflict("POLICY_INCOMPLETE",null);
                        rows.add(new RowRef(monthly.id(),row,UUID.randomUUID().toString()));
                    }
                    return null;
                } catch(InputDraftException ex){throw ex;}
                catch(InputDraftPreserveChange.Failure ex){throw InputDraftException.conflict("POLICY_INCOMPLETE",null);}
                catch(Exception ex){InputDraftDiagnostics.failure(InputDraftDiagnostics.Stage.WORKBOOK_PARSE,file.role(),ex);throw InputDraftException.internal();}
            });
            });
        }
        return rows;
    }
    private Created createCaseDraft(Actor actor,Source basis,String requestedVersion,boolean shared,List<RowRef> rows,
            boolean scanned,org.springframework.dao.DuplicateKeyException race) {
        long caseId=basis.caseId;
        Source current=source(actor,caseId,true);
        if(!basis.version.equals(current.version))throw InputDraftException.conflict("SOURCE_CHANGED",null);
        Draft existing=store.active(actor.company,actor.user,caseId);
        if(existing!=null){existing=store.lockOwned(existing.id(),actor.company,actor.user);fixed(existing,current,requestedVersion);return new Created(summary(existing,restore(existing,current).source),false);}
        Draft created;
        if(shared) {
            Baseline base=store.findBaseline(actor.company,caseId,current.catalog.templateId(),current.version);
            if(base==null) {
                if(race!=null)throw race;
                if(!scanned)throw InputDraftException.internal();
                base=store.createBaseline(actor.company,caseId,current.catalog,current.version,current.files,rows,initialRanges(current),actor.login);
            } else store.validateBaseline(base,current.files,initialRanges(current));
            created=store.createShared(actor.company,actor.user,caseId,current.catalog,current.version,current.files,base,actor.login);
        } else created=store.create(actor.company,actor.user,caseId,current.catalog,current.version,current.files,rows,actor.login);
        return new Created(summary(created,current),true);
    }
    private void fixed(Draft draft,Source source,String version) {
        if(draft.uploaded()){if(draft.templateId()!=source.catalog.templateId())throw InputDraftException.internal();return;}
        if(!draft.sourceVersion().equals(version)||!java.util.Objects.equals(draft.sourceVersion(),source.version)||draft.templateId()!=source.catalog.templateId())throw InputDraftException.conflict("DRAFT_SOURCE_CONFLICT",draft.revision());
        Map<String,Long> expected=new LinkedHashMap<>();source.files.forEach(f->expected.put(f.role(),f.fileNo()));
        if(!expected.equals(tx(true,()->store.fixedFiles(draft.id()))))throw InputDraftException.conflict("SOURCE_CHANGED",draft.revision());
        store.validateDraftBaseline(draft,source.files,initialRanges(source));
    }
    private State state(Actor actor,String id) {
        return state(actor,lockSnapshot(actor,id));
    }
    private Draft lockSnapshot(Actor actor,String id) {
        return InputDraftDiagnostics.timed(InputDraftDiagnostics.Stage.SNAPSHOT_LOCK,null,()->store.lockOwned(id,actor.company,actor.user));
    }
    private State state(Actor actor,Draft draft) {
        return restore(draft,checkedSource(actor,draft));
    }
    /** 소유/원본 접근과 고정 기준을 확인한다. 개별 행/이력을 JVM으로 가져오지 않는다. */
    private Source checkedSource(Actor actor,Draft draft) {
        Source source=InputDraftDiagnostics.timed(InputDraftDiagnostics.Stage.SOURCE_LOAD,null,()->source(actor,draft));
        if(!java.util.Objects.equals(draft.sourceVersion(),source.version)) throw InputDraftException.conflict("SOURCE_CHANGED",draft.revision());
        InputDraftDiagnostics.timed(InputDraftDiagnostics.Stage.FIXED_FILES,null,()->{
            fixed(draft,source,draft.sourceVersion());return null;
        });
        return source;
    }
    private State restore(Draft draft,Source source) { return restore(loadBasis(draft,source)); }
    private InputDraftEditingBasis loadBasis(Draft draft,Source source) {
        var revisions=InputDraftDiagnostics.timed(InputDraftDiagnostics.Stage.REVISION_LOAD,null,()->store.revisions(draft.id()));
        var rows=InputDraftDiagnostics.timed(InputDraftDiagnostics.Stage.ROW_IDENTITIES_LOAD,null,()->store.logicalRows(draft));
        InputDraftDiagnostics.rows(rows.identities().size(),rows.positions().size());
        return new InputDraftEditingBasis(draft,source,revisions,rows.identities(),rows.positions());
    }
    private State restore(InputDraftEditingBasis basis) {
        Draft draft=basis.draft();Source source=basis.source();
        InputDraftHistory history=InputDraftDiagnostics.timed(InputDraftDiagnostics.Stage.HISTORY_RESTORE,null,
            ()->InputDraftHistory.restore(draft,source.catalog,source.definitions,basis.revisions(),basis.identities(),basis.positions(),json));
        Source current=new Source(source.caseId,history.catalog(),source.files,source.key,source.version,source.inputIds,history.definitions());
        return new State(draft,current,history);
    }
    /** 초기 revision은 전체 행 정합성만 DB에서 확인하고 표시할 행만 JVM으로 가져온다. */
    private State initialRead(Draft draft,Source source,TableRef selected,Integer first) {
        var revisions=InputDraftDiagnostics.timed(InputDraftDiagnostics.Stage.REVISION_LOAD,null,()->store.revisions(draft.id()));
        InputDraftDiagnostics.timed(InputDraftDiagnostics.Stage.INITIAL_ROWS_VALIDATE,null,()->{
            store.validateInitialRows(draft,initialRanges(source));return null;
        });
        List<Integer> page=selected==null?List.of():source.definitions.get(selected.id()).page(first,2000);
        var identities=InputDraftDiagnostics.timed(InputDraftDiagnostics.Stage.ROW_IDENTITIES_LOAD,null,()->
            page.isEmpty()?List.<RowIdentity>of():store.initialPageRows(draft,selected.id(),page.get(0),page.get(page.size()-1)));
        var positions=identities.stream().map(row->new RowPosition(row.table(),row.id(),0,null,row.original())).toList();
        InputDraftHistory history=InputDraftDiagnostics.timed(InputDraftDiagnostics.Stage.HISTORY_RESTORE,null,()->
            InputDraftHistory.restore(draft,source.catalog,source.definitions,revisions,identities,positions,json));
        return new State(draft,source,history);
    }
    private static List<InitialRowRange> initialRanges(Source source) {
        List<InitialRowRange> ranges=new ArrayList<>();
        source.definitions.forEach((table,definition)->definition.bodyRanges().forEach(range->
            ranges.add(new InitialRowRange(table,range.startRow(),range.endRow()))));
        return ranges;
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
        return InputDraftDiagnostics.timed(InputDraftDiagnostics.Stage.CHANGE_SUBMIT,null,()->{
            Actor actor=actor();recoverExpired(actor,id);
            var accepted=tx(false,()->{
                Draft draft=InputDraftDiagnostics.timed(InputDraftDiagnostics.Stage.SNAPSHOT_LOCK,null,()->store.lockOwned(id,actor.company,actor.user));if(!draft.uploaded())store.caseAccess(draft.caseId(),false);
                var previous=store.request(id,request.requestId());
                if(previous!=null) {
                    if(previous.schemaVersion()!=InputDraftJson.version(request))throw InputDraftException.conflict("IDEMPOTENCY_CONFLICT",draft.revision());
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
                validateChange(state.source,state.history,request,draft);
                var role=authorization.getCurrentRole();
                if(role!=com.sk.domain.shared.enums.Role.USER&&role!=com.sk.domain.shared.enums.Role.ADMIN)throw InputDraftException.missing();
                var op=store.enqueueChange(draft,request.requestId(),request.baseRevision(),new String(json.changeBytes(request),StandardCharsets.UTF_8),role.name(),actor.login,InputDraftJson.version(request));
                return operationView(op);
            });
            log.info("Input Draft CHANGE submission completed operationId={}",accepted.operationId());
            return accepted;
        });
    }
    void validateChange(Source source,InputDraftHistory history,InputDraftJson.ChangeRequest request,Draft draft) {
        // 모든 표 접근을 먼저 확인해 필드 검증이 다른 표의 존재를 드러내지 않게 한다.
        Map<String,TableRef> requested=new HashMap<>();
        for(JsonNode command:request.operations()) {
            String key=command.path("tableId").asText();requested.put(key,source.catalog.tables().stream().filter(t->t.key().equals(key)).findFirst().orElseThrow(InputDraftException::missing));
        }
        if(InputDraftJson.restores(request)) {
            if(requested.values().stream().anyMatch(table->!source.definitions().get(table.id()).navigationVisible()))throw InputDraftException.missing();
            if(!changesEnabled||!restoresEnabled||!restorePreviewEnabled)throw InputDraftException.conflict("POLICY_UNSUPPORTED",draft.revision());
            InputWorkbookEditor.validate(source,history,request,draft.revision());
            InputWorkbookEditor.validateHistory(source(actor(),draft),store.revisions(draft.id()),json);
            return;
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
        InputWorkbookEditor.validate(source,history,request,draft.revision());
        InputWorkbookEditor.validateHistory(source(actor(),draft),store.revisions(draft.id()),json);
    }
    private boolean inputType(String type,JsonNode value) {
        if(type.equals("DECIMAL_OR_TEXT"))return value.isNumber()||value.isTextual();
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
        return InputDraftDiagnostics.withOperation(operationId,()->InputDraftDiagnostics.timed(InputDraftDiagnostics.Stage.CHANGE_PREPARE,null,()->tx(false,()->{
            Draft draft=InputDraftDiagnostics.timed(InputDraftDiagnostics.Stage.SNAPSHOT_LOCK,null,()->store.lockForRecovery(id));var op=store.lockRunningChange(id,operationId,token);
            if(op==null)return null;
            Actor actor=workerActor(draft,op);
            var basis=loadBasis(draft,checkedSource(actor,draft));State current=restore(basis);
            var request=json.changeRequest(new java.io.ByteArrayInputStream(op.request().getBytes(StandardCharsets.UTF_8)),op.schemaVersion());
            InputWorkbookEditor.validate(current.source,current.history,request,draft.revision());
            InputWorkbookEditor.validateHistory(basis.source(),basis.revisions(),json);
            TableRef table=current.source.catalog.tables().stream().filter(t->t.key().equals(request.operations().get(0).path("tableId").asText())).findFirst().orElseThrow(InputDraftException::internal);
            return new PreparedChange(id,op.id(),token,actor.companyCode,current.source.files.stream().filter(f->f.role().equals(table.role())).findFirst().orElseThrow(),
                current.source.catalog.templateId(),table,request.operations().get(0).path("rowRef").path("rowId").asText(),request,null,basis);
        })));
    }
    @Transactional(propagation=Propagation.NOT_SUPPORTED)
    public OperationView changeOperation(String id,String operationId) {
        return InputDraftDiagnostics.withOperation(operationId,()->InputDraftDiagnostics.timed(InputDraftDiagnostics.Stage.CHANGE_STATUS,null,()->{
            Actor actor=actor();
            var observed=tx(true,()->{
                Draft draft=store.owned(id,actor.company,actor.user);if(!draft.uploaded())store.caseAccess(draft.caseId(),false);
                var op=store.operation(id,operationId,false);
                if(!op.type().equals("CHANGE"))throw InputDraftException.missing();
                return op;
            });
            if(observed.terminal()||tx(true,store::databaseTime).isBefore(observed.expires()))return operationView(observed);
            // 만료 정리는 보조 작업이다. 저장 중인 행을 기다리지 않고 마지막 확정 상태를 반환한다.
            var current=InputDraftDiagnostics.timed(InputDraftDiagnostics.Stage.CHANGE_STATUS_EXPIRE,null,()->tx(false,()->{
                Draft draft=store.tryLockOwned(id,actor.company,actor.user);
                if(draft==null)return observed;
                if(!draft.uploaded())store.caseAccess(draft.caseId(),false);
                var op=store.tryLockOperation(id,operationId);
                if(op==null)return observed;
                if(!op.type().equals("CHANGE"))throw InputDraftException.missing();
                return store.expireChange(id,operationId,failureJson("TIMEOUT",draft.revision()));
            }));
            return operationView(current);
        }));
    }
    private void recoverExpired(Actor actor,String id) {
        tx(false,()->{
            Draft draft=InputDraftDiagnostics.timed(InputDraftDiagnostics.Stage.SNAPSHOT_LOCK,null,()->store.lockOwned(id,actor.company,actor.user));if(!draft.uploaded())store.caseAccess(draft.caseId(),false);
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
        return InputDraftDiagnostics.withOperation(operationId,()->InputDraftDiagnostics.timed(InputDraftDiagnostics.Stage.CHANGE_COMPLETE,null,()->{
            try {
                return tx(false,()->{
                    Draft draft=InputDraftDiagnostics.timed(InputDraftDiagnostics.Stage.SNAPSHOT_LOCK,null,()->
                        requestedActor==null?store.lockForRecovery(id):store.lockOwned(id,requestedActor.company,requestedActor.user));
                    if(!draft.uploaded())store.caseAccess(draft.caseId(),false);
                    var op=store.lockRunningChange(id,operationId,token);
                    if(op==null){store.expireChange(id,operationId,failureJson("TIMEOUT",draft.revision()));return false;}
                    Actor actor=requestedActor==null?workerActor(draft,op):requestedActor;
                    var basis=loadBasis(draft,checkedSource(actor,draft));State state=restore(basis);
                    if(requestedActor==null) {
                        var request=json.changeRequest(new java.io.ByteArrayInputStream(op.request().getBytes(StandardCharsets.UTF_8)),op.schemaVersion());
                        InputWorkbookEditor.validate(state.source,state.history,request,draft.revision());
                        InputWorkbookEditor.validateHistory(basis.source(),basis.revisions(),json);
                    }
                    var applied=InputDraftDiagnostics.timed(InputDraftDiagnostics.Stage.CHANGE_APPLY,null,()->
                        state.history.apply(json.delta(json.deltaText(verifiedDelta)),op.request(),op.id(),op.baseRevision()+1));
                    boolean completed=store.completeChange(id,operationId,token,applied.delta(),applied.result(),applied.rows(),
                        new com.sk.domain.shared.repository.master.tmm.ModelInputBaselineRows.Snapshot(basis.identities(),basis.positions()));
                    if(!completed)store.expireChange(id,operationId,failureJson("TIMEOUT",draft.revision()));
                    return completed;
                });
            } catch(RuntimeException ex) {
                if(requestedActor!=null&&ex instanceof InputDraftException error&&error.getStatus().value()==404)throw error;
                String reason=ex instanceof InputDraftException error&&"SOURCE_CHANGED".equals(error.getReason())?"SOURCE_CHANGED":"PERSISTENCE_FAILED";
                // commit 응답 유실도 여기로 올 수 있다. 쓰기 DB 잠금으로 성공 여부를 먼저 재확인한다.
                return tx(false,()->{
                    Draft draft=InputDraftDiagnostics.timed(InputDraftDiagnostics.Stage.SNAPSHOT_LOCK,null,()->
                        requestedActor==null?store.lockForRecovery(id):store.lockOwned(id,requestedActor.company,requestedActor.user));
                    if(requestedActor!=null)if(!draft.uploaded())store.caseAccess(draft.caseId(),false);
                    var op=store.operation(id,operationId,true);
                    if(op.status().equals("SUCCEEDED"))return token!=null&&token.equals(op.token());
                    if(op.terminal()||!op.status().equals("RUNNING")||token==null||!token.equals(op.token()))return false;
                    op=store.expireChange(id,operationId,failureJson("TIMEOUT",draft.revision()));
                    if(!op.terminal())store.failStoppedChange(id,operationId,token,failureJson(reason,draft.revision()));
                    return false;
                });
            }
        }));
    }
    private OperationView operationView(ModelInputDraftRepository.Operation op) {
        if(!Set.of(2,3).contains(op.schemaVersion()))throw InputDraftException.internal();
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
                InputReadDefinition def=source.definitions.get(table.id());if(!def.navigationVisible())continue;List<String> controls=new ArrayList<>();def.controls().forEach(c->controls.add(c.path("id").asText()));
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
        Actor actor=actor();CaseSource basis=tx(true,()->new CaseSource(source(actor,caseId,false),store.hasCompletedLatestRun(caseId)));
        Source source=basis.source();
        // 완료된 실행의 등록 파일은 반복 판독하지 않는다. 완료 증거가 없으면 기존 객체·시트 검사를 유지한다.
        if(!basis.completedRun())for(FileRef file:source.files) {
            Set<String> sheets=source.catalog.tables().stream().filter(t->t.role().equals(file.role())).map(TableRef::sheet).collect(java.util.stream.Collectors.toSet());
            reader.verifySheets(file,actor.companyCode,sheets);
        }
        Source finalSource=tx(true,()->source(actor,caseId,false));if(!source.version.equals(finalSource.version))throw InputDraftException.conflict("SOURCE_CHANGED",null);
        return new SourceStructure(source.key,source.version,sections(source),null);
    }
    @Transactional(propagation=Propagation.NOT_SUPPORTED)
    public Structure structure(String id) {
        Actor actor=actor();
        return InputDraftDiagnostics.timed(InputDraftDiagnostics.Stage.STRUCTURE_STATE,null,()->tx(false,()->{
            // 포인터와 작업을 읽는 동안 timeout/종료 복구가 같은 Draft를 바꾸지 못하게 한다.
            Draft draft=lockSnapshot(actor,id);Source source=checkedSource(actor,draft);
            State state=draft.revision()==0?initialRead(draft,source,null,null):restore(draft,source);
            return structure(state);
        }));
    }
    @Transactional(propagation=Propagation.NOT_SUPPORTED)
    public Table table(String id,long revision,String key,Integer first) {
        Actor actor=actor();State state=InputDraftDiagnostics.timed(InputDraftDiagnostics.Stage.TABLE_STATE_BEFORE,null,()->tx(false,()->{
            Draft draft=lockSnapshot(actor,id);Source source=checkedSource(actor,draft);
            // 오래된 revision과 없는 표는 행/이력 복원 전에 거절한다. 표 키는 성공 이력 적용으로 바뀌지 않는다.
            if(revision!=draft.revision())throw InputDraftException.conflict("REVISION_CONFLICT",draft.revision());
            TableRef selected=source.catalog.tables().stream().filter(t->t.key().equals(key)).findFirst().orElseThrow(InputDraftException::missing);
            return draft.revision()==0?initialRead(draft,source,selected,first):restore(draft,source);
        }));
        Table result=InputDraftDiagnostics.timed(InputDraftDiagnostics.Stage.TABLE_RENDER,null,()->renderGetTable(actor,state,key,first));
        // 행·이력·파일을 바꾸는 모든 저장은 revision을 올리므로, 같은 revision·원본 기준·파일이면 렌더 기준과 같다.
        // 전체 행 복원을 반복하지 않고 소유/원본 접근·고정 파일과 이 값들만 다시 확인한다.
        InputDraftDiagnostics.timed(InputDraftDiagnostics.Stage.TABLE_STATE_AFTER,null,()->tx(false,()->{
            Draft draft=lockSnapshot(actor,id);Source source=checkedSource(actor,draft);
            if(draft.revision()!=revision)throw InputDraftException.conflict("REVISION_CONFLICT",draft.revision());
            if(!java.util.Objects.equals(state.source.version,source.version)||!state.source.files.equals(source.files))throw InputDraftException.conflict("SOURCE_CHANGED",revision);
            return null;
        }));
        return result;
    }
    private Table renderTable(Actor actor,State state,String key,Integer first) {return renderTable(actor,state,key,first,InputDraftJson.RESPONSE_LIMIT);}
    private Table renderTable(Actor actor,State state,String key,Integer first,int limit) {
        return renderTable(actor,state,key,first,limit,null);
    }
    private Table renderGetTable(Actor actor,State state,String key,Integer first) {
        TableRef table=state.source.catalog.tables().stream().filter(t->t.key().equals(key)).findFirst().orElseThrow(InputDraftException::missing);
        FileRef file=state.source.files.stream().filter(f->f.role().equals(table.role())).findFirst().orElseThrow(InputDraftException::internal);
        var snapshot=tableSnapshots==null?null:tableSnapshots.select(actor,state.source,state.history.originalTable(table.id()),file);
        if(snapshot!=null) {
            reader.validatedLocation(file,actor.companyCode);
            try{return renderTable(actor,state,key,first,InputDraftJson.RESPONSE_LIMIT,snapshot);}
            catch(InputTableSnapshotReader.Unavailable ex) {
                // 부분 callback이 바꾼 rows/budget/cutoff/끝행/오류 집합은 모두 버린다.
            }
        }
        return renderTable(actor,state,key,first);
    }
    private Table renderTable(Actor actor,State state,String key,Integer first,int limit,InputTableSnapshotReader.Selection snapshot) {
        TableRef table=state.source.catalog.tables().stream().filter(t->t.key().equals(key)).findFirst().orElseThrow(InputDraftException::missing);
        InputReadDefinition def=state.source.definitions.get(table.id());List<Integer> candidate=def.page(first,2000),headers=def.headerRows();
        Map<Integer,String> identities=state.history.bindings(table.id());
        // 저장된 추가 행은 값이 비어도 표시한다. 빈 원본 행과 미활성 슬롯은 기존 값 기준으로 자른다.
        int[] lastVisibleRow={identities.entrySet().stream().filter(entry->"ADDED".equals(state.history.origin(entry.getValue())))
            .mapToInt(Map.Entry::getKey).max().orElse(0)};
        Map<Integer,Map<Integer,Cell>> rows=new HashMap<>(),baselineRows=new HashMap<>();Set<Integer> wanted=new HashSet<>(candidate);wanted.addAll(headers);
        long[] budget={0};int[] cutoff={Integer.MAX_VALUE};
        Set<Integer> invalidRows=new HashSet<>(),invalidControls=new HashSet<>();
        FileRef file=state.source.files.stream().filter(f->f.role().equals(table.role())).findFirst().orElseThrow(InputDraftException::internal);
        List<Window> windows=new ArrayList<>();
        Map<Integer,List<Integer>> sourceRows=new HashMap<>();
        // 끝 행은 현재 페이지 밖도 확인하되, 셀 값은 요청 페이지에만 보관한다.
        // 전체 행 목록/Set을 중복 생성하지 않고 발행 범위를 순회한다.
        java.util.function.IntConsumer register=row->{
            Integer original=state.history.sourceRow(table.id(),row);if(original!=null)sourceRows.computeIfAbsent(original,k->new ArrayList<>()).add(row);
            if(def.body(row)){
                Map<Integer,Cell> changed=new HashMap<>();state.history.overlay(table.id(),row,changed);
                if(hasBodyData(def,changed))lastVisibleRow[0]=Math.max(lastVisibleRow[0],row);
            }
        };
        headers.forEach(register::accept);
        for(var range:def.bodyRanges())for(int row=range.startRow();row<=range.endRow();row++)register.accept(row);
        // 이동 전 원본 좌표 기준으로 필요한 마지막 행까지만 판독한다(헤더·끝 행 확인용 본문 포함).
        int lastSourceRow=sourceRows.keySet().stream().mapToInt(Integer::intValue).max().orElse(0);
        windows.add(new Window(table.sheet(),sourceRows::containsKey,state.history.sourceColumns(table.id(),def.columns()),(original,sourceValues)->{
            Map<Integer,Cell> values=state.history.projectSource(table.id(),sourceValues);
            for(int row:sourceRows.get(original)) {
                Map<Integer,Cell> currentValues=values;
                if(def.body(row)){
                    currentValues=new HashMap<>(values);state.history.overlay(table.id(),row,currentValues);
                    if(hasBodyData(def,currentValues))lastVisibleRow[0]=Math.max(lastVisibleRow[0],row);
                }
                if(!wanted.contains(row))continue;
                if(!headers.contains(row)&&row>cutoff[0])continue;
                baselineRows.put(row,values);
                budget[0]+=json.bytes(currentValues).length;
                if(budget[0]>InputDraftJson.RESPONSE_LIMIT&&!headers.contains(row)&&candidate.contains(row)&&!candidate.isEmpty()&&row!=candidate.get(0)){cutoff[0]=Math.min(cutoff[0],row-1);continue;}
                rows.computeIfAbsent(row,k->new HashMap<>()).putAll(currentValues);
            }
        },original->invalidRows.addAll(sourceRows.get(original)),lastSourceRow));
        for(JsonNode control:def.controls()) {
            int boundRow=control.path("binding").path("rowNumber").asInt(),boundColumn=control.path("binding").path("columnIndex").asInt()+1;
            JsonNode original=state.history.originalControl(table.id(),control.path("id").asText());
            int sourceRow=original.path("binding").path("rowNumber").asInt(),sourceCol=original.path("binding").path("columnIndex").asInt()+1;
            windows.add(new Window(table.sheet(),row->row==sourceRow,Set.of(sourceCol),
                (row,values)->{if(values.containsKey(sourceCol))rows.computeIfAbsent(boundRow,k->new HashMap<>()).put(boundColumn,values.get(sourceCol));},invalidControls::add,sourceRow));
        }
        if(snapshot==null)reader.read(file,actor.companyCode,windows);
        else tableSnapshots.read(snapshot,windows);
        if(!invalidRows.isEmpty())throw InputDraftException.internal();
        InputReadDefinition displayDef=trimBody(def,lastVisibleRow[0]);
        List<Integer> displayCandidate=displayDef.page(first,2000);
        for(int row:wanted)state.history.overlay(table.id(),row,rows.computeIfAbsent(row,k->new HashMap<>()));
        for(JsonNode control:def.controls()) {
            int row=control.path("binding").path("rowNumber").asInt(),col=control.path("binding").path("columnIndex").asInt()+1;
            Cell value=state.history.control(control.path("sourceTableId").asText(),row,col);
            if(value!=null)rows.computeIfAbsent(row,k->new HashMap<>()).put(col,value);
        }
        List<Integer> page=new ArrayList<>(displayCandidate.stream().filter(row->row<=cutoff[0]).toList());
        // 변경 셀과 페이지 크기 재조정 모두 같은 요청의 검증 자료를 재사용한다.
        var cellValidation=new java.util.function.Consumer<InputDraftJson.ChangeRequest>() {
            private InputWorkbookEditor.ValidationContext context;
            @Override public void accept(InputDraftJson.ChangeRequest request) {
                if(context==null)context=InputWorkbookEditor.validationContext(state.source,state.history);
                InputWorkbookEditor.validate(state.source,context.bindings(),context.addedRows(),context.addedFields(),request,state.draft.revision());
            }
        };
        Table result;
        while(true) {
            result=project(state,table,displayDef,page,rows,baselineRows,identities,invalidRows,cellValidation);
            if(json.bytes(BaseResponseFactory.create(result)).length<=limit)break;
            if(page.size()<=1)throw InputDraftException.responseLarge();page=new ArrayList<>(page.subList(0,Math.max(1,page.size()/2)));
        }
        // 크기로 정한 범위만 검사한다. 오류 때문에 행을 건너뛰거나 페이지를 줄이지 않는다.
        if(!invalidControls.isEmpty()||invalidRows.stream().anyMatch(headers::contains)
            ||page.stream().anyMatch(invalidRows::contains))throw InputDraftException.internal();
        return result;
    }
    private static boolean hasBodyData(InputReadDefinition def,Map<Integer,Cell> cells) {
        return def.columns().stream().map(cells::get).anyMatch(value->value!=null&&value.raw()!=null
            &&(!(value.raw() instanceof String text)||!text.isEmpty()));
    }
    private static InputReadDefinition trimBody(InputReadDefinition def,int lastRow) {
        List<InputReadDefinition.Range> ranges=def.bodyRanges().stream().filter(range->range.startRow()<=lastRow)
            .map(range->new InputReadDefinition.Range(range.startRow(),Math.min(range.endRow(),lastRow))).toList();
        return new InputReadDefinition(def.columns(),def.headerRows(),ranges,def.identityColumns(),def.fields(),def.capabilities(),
            def.inputGuideRules(),def.controls(),def.displayUpdateRules(),def.columnCreation(),def.navigationVisible());
    }
    @io.swagger.v3.oas.annotations.media.Schema(requiredProperties={"kind","draftId","revision","draftEndpoint","structure","selection"})
    public record UploadResult(String kind,String draftId,long revision,String draftEndpoint,Structure structure,
        @io.swagger.v3.oas.annotations.media.Schema(nullable=true,types={"object","null"},schemaResolution=io.swagger.v3.oas.annotations.media.Schema.SchemaResolution.INLINE) Table selection) {}
    UploadResult previewUpload(Actor actor,Draft draft,Catalog catalog,List<FileRef> files,String sourceKey,
            List<Revision> chain,List<RowIdentity> identities,List<RowPosition> positions,String kind) {
        Source source=uploadSource(catalog,files,sourceKey);
        InputDraftHistory history=InputDraftHistory.restore(draft,source.catalog,source.definitions,chain,identities,positions,json);
        State state=new State(draft,new Source(null,history.catalog(),source.files,source.key,null,source.inputIds,history.definitions()),history);
        var first=state.source.catalog.tables().stream().filter(t->t.role().equals(kind)&&state.source.definitions.get(t.id()).navigationVisible()).findFirst().orElse(null);
        Structure structure=structure(state);
        int budget=InputDraftJson.BODY_LIMIT-json.bytes(structure).length-4096;
        if(budget<1)throw InputDraftException.responseLarge();
        Table selection=first==null?null:renderTable(actor,state,first.key(),null,budget);
        return new UploadResult(kind,draft.id(),draft.revision(),"/api/v1/mi/fundamental-model/input-drafts/"+draft.id(),structure,selection);
    }
    private Table project(State state,TableRef table,InputReadDefinition def,List<Integer> page,Map<Integer,Map<Integer,Cell>> values,Map<Integer,Map<Integer,Cell>> baselineRows,Map<Integer,String> ids,Set<Integer> invalidRows,java.util.function.Consumer<InputDraftJson.ChangeRequest> cellValidation) {
        List<Column> columns=def.columns().stream().map(col->new Column("column"+(col-1),CellReference.convertNumToColString(col-1))).toList();
        List<Map<String,Object>> grid=new ArrayList<>();List<Long> inputs=new ArrayList<>();List<RowBinding> bindings=new ArrayList<>();List<EditValue> edits=new ArrayList<>();List<CellRestore> cellRestores=new ArrayList<>();
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
                if(editable&&rowId!=null&&"SOURCE".equals(state.history.origin(rowId))
                    &&state.history.sourceColumn(table.id(),col)!=null&&(value==null||!value.formula())) {
                    Cell baseline=baselineRows.getOrDefault(row,Map.of()).get(col);
                    if(baseline==null||!baseline.formula())try {
                        Object old=baseline==null||baseline.raw()==null?null:typed(field.path("dataType").asText(),baseline.raw());
                        Object now=value==null||value.raw()==null?null:typed(field.path("dataType").asText(),value.raw());
                        ObjectNode status=changeItem("CELL","MODIFIED");
                        if(json.sameValue(json.tree(old),json.tree(now)))status.put("restoreStatus","NOT_ALLOWED").put("restoreReason","NO_CHANGE");
                        else {
                            ObjectNode command=(ObjectNode)json.tree(Map.of("type",old==null?"CLEAR":"SET","commandId","revert","tableId",table.key(),"rowRef",Map.of("rowId",rowId),"fieldKey",field.path("fieldKey").asText()));
                            if(old!=null)command.set("value",json.tree(old));
                            restorationStatus(status,state,command,cellValidation);
                        }
                        cellRestores.add(new CellRestore(rowId,field.path("fieldKey").asText(),comparisonValue(old),comparisonValue(now),status.path("restoreStatus").asText(),status.path("restoreReason").isNull()?null:status.path("restoreReason").asText()));
                    }catch(InputDraftException ex){if(ex.getStatus().is5xxServerError())invalidRows.add(row);else throw ex;}
                }
                if(editable&&rowId!=null&&value!=null&&value.raw()!=null&&!value.formula()) {
                    try {typed.put(field.path("fieldKey").asText(),typed(field.path("dataType").asText(),value.raw()));}
                    catch(InputDraftException ex){invalidRows.add(row);}
                }
            }
            if(rowId!=null)edits.add(new EditValue(rowId,Map.copyOf(typed)));
        }
        List<JsonNode> fields=new ArrayList<>();for(JsonNode original:def.fields()){ObjectNode field=original.deepCopy();int col=field.remove("columnNumber").asInt();field.put("columnField","column"+(col-1));if("READY".equals(field.path("policyStatus").path("SET_HEADER").asText())&&!(changesEnabled&&InputWorkbookPolicy.supportsHeader(table,col)))((ObjectNode)field.path("policyStatus")).put("SET_HEADER","UNSUPPORTED");fields.add(field);}
        // 표시 행 축소로 ADD_ROW 등 원래 발행된 편집 정책의 지원 판정을 바꾸지 않는다.
        InputReadDefinition policyDef=state.source.definitions.get(table.id());
        Map<String,String> caps=new LinkedHashMap<>();policyDef.capabilities().fields().forEachRemaining(e->caps.put(e.getKey(),"READY".equals(e.getValue().asText())
            &&!(changesEnabled&&InputWorkbookEditor.supports(table,policyDef,e.getKey()))?"UNSUPPORTED":e.getValue().asText()));
        List<JsonNode> controls=new ArrayList<>();for(JsonNode original:def.controls()){ObjectNode control=original.deepCopy();int row=control.path("binding").path("rowNumber").asInt(),col=control.path("binding").path("columnIndex").asInt()+1;Cell cell=values.getOrDefault(row,Map.of()).get(col);control.set("value",json.tree(cell==null?null:cell.raw()));control.put("editable",original.path("editable").asBoolean()&&"READY".equals(caps.get("SET"))&&InputWorkbookPolicy.supportsControl(table,original));controls.add(control);}
        Integer start=page.isEmpty()?null:page.get(0),end=page.isEmpty()?null:page.get(page.size()-1),next=null;
        if(end!=null) {List<Integer> after=def.page(end,2);if(after.size()>1)next=after.get(1);}
        return new Table(state.draft.id(),state.draft.revision(),state.source.key,state.source.version,
            new TableData(table.key(),columns,grid,inputs,def.inputGuideRules(),controls,def.displayUpdateRules()),
            new Page(def.totalRows(),page.size(),start,end,next!=null,next),new Editing(caps,fields,bindings,edits,List.copyOf(cellRestores),caps.get("ADD_COLUMN").equals("READY")?def.columnCreation():null));
    }
    private Object typed(String type,Object value) {
        if("DECIMAL_OR_TEXT".equals(type)&&(value instanceof String||value instanceof BigDecimal))return value;
        if("TEXT".equals(type)&&value instanceof String)return value;
        if(value instanceof BigDecimal number){if("DECIMAL".equals(type))return number;if("INTEGER".equals(type))try{return number.toBigIntegerExact();}catch(ArithmeticException ex){throw InputDraftException.internal();}}
        throw InputDraftException.internal();
    }
}
