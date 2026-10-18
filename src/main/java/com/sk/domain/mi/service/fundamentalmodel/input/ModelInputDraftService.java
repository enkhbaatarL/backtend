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
    private final AuthorizationService authorization;
    private final InputWorkbookReader reader;
    private final InputDraftJson json;
    // JpaConfig의 primary manager이며 JDBC도 같은 masterDataSource를 사용한다.
    private final PlatformTransactionManager transactionManager;
    private record Actor(int company,int user,String companyCode,String login) {}
    private record Source(long caseId,Catalog catalog,List<FileRef> files,String key,String version,
        Map<String,String> inputIds,Map<Long,InputReadDefinition> definitions) {}
    private record State(Draft draft,Source source) {}
    private Actor actor() {
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
            fixed(existing,current,requestedVersion);supported(existing);
            return new Created(summary(existing,current),false);
        });
        if(!basis.version.equals(requestedVersion))throw InputDraftException.conflict("SOURCE_CHANGED",null);
        List<RowRef> rows=new ArrayList<>();
        for(FileRef file:basis.files) {
            List<Window> windows=new ArrayList<>();
            for(TableRef table:basis.catalog.tables())if(table.role().equals(file.role())) {
                InputReadDefinition def=basis.definitions.get(table.id());
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
            reader.read(file,actor.companyCode,windows);
        }
        return tx(false,()->{
            Source current=source(actor,caseId,true);
            if(!basis.version.equals(current.version))throw InputDraftException.conflict("SOURCE_CHANGED",null);
            Draft existing=store.active(actor.company,actor.user,caseId);
            if(existing!=null){fixed(existing,current,requestedVersion);supported(existing);return new Created(summary(existing,current),false);}
            Draft created=store.create(actor.company,actor.user,caseId,current.catalog,current.version,current.files,rows,actor.login);
            return new Created(summary(created,current),true);
        });
    }
    private void fixed(Draft draft,Source source,String version) {
        if(!draft.sourceVersion().equals(version)||!draft.sourceVersion().equals(source.version)||draft.templateId()!=source.catalog.templateId())throw InputDraftException.conflict("DRAFT_SOURCE_CONFLICT",draft.revision());
        Map<String,Long> expected=new LinkedHashMap<>();source.files.forEach(f->expected.put(f.role(),f.fileNo()));
        if(!expected.equals(tx(true,()->store.fixedFiles(draft.id()))))throw InputDraftException.conflict("SOURCE_CHANGED",draft.revision());
    }
    private State state(Actor actor,String id) {
        Draft draft=store.owned(id,actor.company,actor.user);Source source=source(actor,draft.caseId(),false);
        if(!draft.sourceVersion().equals(source.version)) throw InputDraftException.conflict("SOURCE_CHANGED",draft.revision());
        fixed(draft,source,draft.sourceVersion());
        // 이번 slice는 성공 revision0만 생성. 이후 성공 chain reader는 CHANGE slice가 제공한다.
        supported(draft);
        return new State(draft,source);
    }
    private static void supported(Draft draft) {
        if(draft.revision()!=0||draft.activeOperation()!=null)throw InputDraftException.conflict("POLICY_UNSUPPORTED",draft.revision());
    }
    private Summary summary(Draft draft,Source source) {
        String label=switch(draft.status()){case "DRAFT"->"편집 중";case "FINALIZED"->"최종저장 완료";case "DISCARDED"->"폐기";default->throw InputDraftException.internal();};
        return new Summary(draft.id(),draft.caseId(),source.key,draft.sourceVersion(),draft.status(),label,draft.revision(),null,null,draft.finalizedCase(),"/api/v1/mi/fundamental-model/input-drafts/"+draft.id());
    }
    private List<Section> sections(Source source) {
        List<Section> result=new ArrayList<>();
        for(String role:ROLES) {
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
    public Structure structure(String id) {Actor actor=actor();State state=tx(true,()->state(actor,id));return new Structure(summary(state.draft,state.source),sections(state.source),null);}
    @Transactional(propagation=Propagation.NOT_SUPPORTED)
    public Table table(String id,long revision,String key,Integer first) {
        Actor actor=actor();State state=tx(true,()->state(actor,id));
        if(revision!=state.draft.revision())throw InputDraftException.conflict("REVISION_CONFLICT",state.draft.revision());
        TableRef table=state.source.catalog.tables().stream().filter(t->t.key().equals(key)).findFirst().orElseThrow(InputDraftException::missing);
        InputReadDefinition def=state.source.definitions.get(table.id());List<Integer> candidate=def.page(first,2000),headers=def.headerRows();
        Map<Integer,Map<Integer,Cell>> rows=new HashMap<>();Set<Integer> wanted=new HashSet<>(candidate);wanted.addAll(headers);
        long[] budget={0};int[] cutoff={Integer.MAX_VALUE};
        Set<Integer> invalidRows=new HashSet<>(),invalidControls=new HashSet<>();
        FileRef file=state.source.files.stream().filter(f->f.role().equals(table.role())).findFirst().orElseThrow(InputDraftException::internal);
        List<Window> windows=new ArrayList<>();
        windows.add(new Window(table.sheet(),
            row->headers.contains(row)||(wanted.contains(row)&&row<=cutoff[0]),Set.copyOf(def.columns()),(row,values)->{
            budget[0]+=json.bytes(values).length;
            if(budget[0]>InputDraftJson.RESPONSE_LIMIT&&!headers.contains(row)&&candidate.contains(row)&&!candidate.isEmpty()&&row!=candidate.get(0)){cutoff[0]=Math.min(cutoff[0],row-1);return;}
            rows.computeIfAbsent(row,k->new HashMap<>()).putAll(values);
        },invalidRows::add));
        for(JsonNode control:def.controls()) {
            int boundRow=control.path("binding").path("rowNumber").asInt(),boundColumn=control.path("binding").path("columnIndex").asInt()+1;
            windows.add(new Window(table.sheet(),row->row==boundRow,Set.of(boundColumn),
                (row,values)->rows.computeIfAbsent(row,k->new HashMap<>()).putAll(values),invalidControls::add));
        }
        reader.read(file,actor.companyCode,windows);
        List<Integer> page=new ArrayList<>(candidate.stream().filter(row->row<=cutoff[0]).toList());
        Map<Integer,String> identities=new HashMap<>();tx(true,()->store.rows(id,table.id())).forEach(row->identities.put(row.row(),row.id()));
        Table result;
        while(true) {
            result=project(state,table,def,page,rows,identities,invalidRows);
            if(json.bytes(BaseResponseFactory.create(result)).length<=InputDraftJson.RESPONSE_LIMIT)break;
            if(page.size()<=1)throw InputDraftException.responseLarge();page=new ArrayList<>(page.subList(0,Math.max(1,page.size()/2)));
        }
        // 크기로 정한 범위만 검사한다. 오류 때문에 행을 건너뛰거나 페이지를 줄이지 않는다.
        if(!invalidControls.isEmpty()||invalidRows.stream().anyMatch(headers::contains)
            ||page.stream().anyMatch(invalidRows::contains))throw InputDraftException.internal();
        State current=tx(true,()->state(actor,id));
        if(current.draft.revision()!=revision)throw InputDraftException.conflict("REVISION_CONFLICT",current.draft.revision());
        if(!state.source.version.equals(current.source.version))throw InputDraftException.conflict("SOURCE_CHANGED",revision);
        return result;
    }
    private Table project(State state,TableRef table,InputReadDefinition def,List<Integer> page,Map<Integer,Map<Integer,Cell>> values,Map<Integer,String> ids,Set<Integer> invalidRows) {
        List<Column> columns=def.columns().stream().map(col->new Column("column"+(col-1),CellReference.convertNumToColString(col-1))).toList();
        List<Map<String,Object>> grid=new ArrayList<>();List<Long> inputs=new ArrayList<>();List<RowBinding> bindings=new ArrayList<>();List<EditValue> edits=new ArrayList<>();
        Set<Integer> returned=new java.util.TreeSet<>(def.headerRows());returned.addAll(page);
        for(int row:returned) {
            Map<Integer,Cell> cells=values.getOrDefault(row,Map.of());Map<String,Object> record=new LinkedHashMap<>();record.put("id",row-1);record.put("rowNumber",row);
            for(int col:def.columns()){Cell value=cells.get(col);if(value!=null)record.put("column"+(col-1),value.display());}
            grid.add(record);
            if(!page.contains(row))continue;
            String rowId=ids.get(row);bindings.add(new RowBinding(row,rowId,rowId==null?"EMPTY_SLOT":"SOURCE"));Map<String,Object> typed=new LinkedHashMap<>();
            for(JsonNode field:def.fields()) {
                int col=field.path("columnNumber").asInt();Cell value=cells.get(col);
                boolean editable="READY".equals(field.path("policyStatus").path("SET").asText())&&table.scopes().stream().anyMatch(scope->scope.contains(row,col));
                if(editable&&(value==null||!value.formula()))inputs.add((long)row*16384+(col-1));
                if(editable&&rowId!=null&&value!=null&&!value.formula()) {
                    try {typed.put(field.path("fieldKey").asText(),typed(field.path("dataType").asText(),value.raw()));}
                    catch(InputDraftException ex){invalidRows.add(row);}
                }
            }
            if(rowId!=null)edits.add(new EditValue(rowId,Map.copyOf(typed)));
        }
        List<JsonNode> fields=new ArrayList<>();for(JsonNode original:def.fields()){ObjectNode field=original.deepCopy();int col=field.remove("columnNumber").asInt();field.put("columnField","column"+(col-1));fields.add(field);}
        Map<String,String> caps=new LinkedHashMap<>();def.capabilities().fields().forEachRemaining(e->caps.put(e.getKey(),"READY".equals(e.getValue().asText())?"UNSUPPORTED":e.getValue().asText()));
        List<JsonNode> controls=new ArrayList<>();for(JsonNode original:def.controls()){ObjectNode control=original.deepCopy();int row=control.path("binding").path("rowNumber").asInt(),col=control.path("binding").path("columnIndex").asInt()+1;Cell cell=values.getOrDefault(row,Map.of()).get(col);control.set("value",json.tree(cell==null?null:cell.raw()));controls.add(control);}
        Integer start=page.isEmpty()?null:page.get(0),end=page.isEmpty()?null:page.get(page.size()-1),next=null;
        if(end!=null) {List<Integer> after=def.page(end,2);if(after.size()>1)next=after.get(1);}
        return new Table(state.draft.id(),state.draft.revision(),state.source.key,state.source.version,
            new TableData(table.key(),columns,grid,inputs,def.inputGuideRules(),controls,def.displayUpdateRules()),
            new Page(def.totalRows(),page.size(),start,end,next!=null,next),new Editing(caps,fields,bindings,edits));
    }
    private Object typed(String type,Object value) {
        if("TEXT".equals(type)&&value instanceof String)return value;
        if(value instanceof BigDecimal number){if("DECIMAL".equals(type))return number;if("INTEGER".equals(type))try{return number.toBigIntegerExact();}catch(ArithmeticException ex){throw InputDraftException.internal();}}
        throw InputDraftException.internal();
    }
}
