package com.sk.domain.mi.service.fundamentalmodel.input;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sk.domain.shared.repository.master.tmm.ModelInputDraftRepository.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** 고정 발행/원본 행과 성공 delta만 합성한다. workbook 계산이나 영속 cache는 만들지 않는다. */
final class InputDraftHistory {
    private final InputDraftJson json;
    private final InputDraftColumns columns;
    private final Catalog basis;
    private final Map<Long,InputReadDefinition> originalDefinitions;
    private final Map<Long,TableRef> tables=new LinkedHashMap<>();
    private final Map<Long,InputReadDefinition> definitions=new LinkedHashMap<>();
    private final Map<String,LogicalRow> rows=new LinkedHashMap<>();
    private final Map<Long,Map<Integer,LogicalRow>> activeRows=new HashMap<>();
    private final Map<Long,Set<Integer>> originalRows=new HashMap<>();
    private final Map<String,Map<Integer,InputWorkbookReader.Cell>> rowCells=new HashMap<>();
    private final Map<Long,Map<Integer,Map<Integer,InputWorkbookReader.Cell>>> fixedCells=new HashMap<>();
    private final List<RowPosition> positions=new ArrayList<>();
    private final Map<String,Long> rowBaselines=new HashMap<>();
    private final Map<Long,Long> tableBaselines=new HashMap<>();
    private record LogicalRow(long table,String id,Integer original,Integer position,String operation,String client) {}
    private record CellLocation(long table,int row,int column) {}
    record Applied(String delta,String result,List<RowWrite> rows) {}

    private InputDraftHistory(Catalog basis,Map<Long,InputReadDefinition> definitions,InputDraftJson json) {
        this.basis=basis;this.json=json;this.originalDefinitions=definitions;
        this.columns=new InputDraftColumns(basis.tables(),definitions);
        basis.tables().forEach(table->tables.put(table.id(),table));this.definitions.putAll(definitions);
    }
    static InputDraftHistory restore(Draft draft,Catalog basis,Map<Long,InputReadDefinition> definitions,
            List<Revision> revisions,List<RowIdentity> identities,List<RowPosition> storedPositions,InputDraftJson json) {
        InputDraftHistory history=new InputDraftHistory(basis,definitions,json);
        for(RowIdentity row:identities)if(row.original()!=null&&row.baseline()==0) {
            TableRef table=history.table(row.table());
            require(row.operation()==null&&row.client()==null&&uuid(row.id())&&definitions.get(table.id()).body(row.original()));
            require(history.rows.putIfAbsent(row.id(),new LogicalRow(row.table(),row.id(),row.original(),row.original(),null,null))==null);
            history.originalRows.computeIfAbsent(row.table(),ignored->new HashSet<>()).add(row.original());
            history.positions.add(new RowPosition(row.table(),row.id(),0,null,row.original()));
            history.rowBaselines.put(row.id(),0L);
        }
        require(revisions.size()==draft.revision()+1);
        long expected=0;
        for(Revision revision:revisions) {
            require(revision.number()==expected++&&revision.schema()==2);
            if(revision.number()==0) {
                require(revision.base()==null&&revision.operation()==null&&revision.delta()==null);
                continue;
            }
            require(revision.base()!=null&&revision.base()==revision.number()-1&&uuid(revision.operation())
                &&Set.of("CHANGE","UPLOAD_FILE").contains(revision.type())&&"SUCCEEDED".equals(revision.status())&&revision.operationSchema()==2
                &&revision.base().equals(revision.operationBase()));
            if("UPLOAD_FILE".equals(revision.type())) {
                JsonNode delta=json.payload(revision.delta());exact(delta,"policyTemplateId","uploadFileId","kind");
                require(delta.path("policyTemplateId").asLong()==basis.templateId()&&uuid(delta.path("uploadFileId").asText())
                    &&Set.of("SUPPLY","DEMAND","TRADE","OUTPUT").contains(delta.path("kind").asText()));
                history.resetFile(delta.path("kind").asText(),revision.number(),identities);
                continue;
            }
            Applied applied=history.apply(json.delta(revision.delta()),revision.request(),revision.operation(),revision.number());
            require(json.sameValue(json.payload(revision.result()),json.payload(applied.result())));
        }
        Set<RowIdentity> expectedRows=new HashSet<>();
        history.rows.values().forEach(row->expectedRows.add(new RowIdentity(row.table,row.id,row.original,row.operation,row.client,history.rowBaselines.getOrDefault(row.id,0L))));
        require(expectedRows.size()==identities.size()&&expectedRows.equals(new HashSet<>(identities)));
        require(history.positions.size()==storedPositions.size()&&new HashSet<>(history.positions).equals(new HashSet<>(storedPositions)));
        history.validatePositions();
        return history;
    }
    private void resetFile(String role,long revision,List<RowIdentity> identities) {
        for(TableRef table:basis.tables())if(table.role().equals(role)) {
            tables.put(table.id(),table);definitions.put(table.id(),originalDefinitions.get(table.id()));
            columns.reset(table,originalDefinitions.get(table.id()));tableBaselines.put(table.id(),revision);
            originalRows.remove(table.id());fixedCells.remove(table.id());
            for(var entry:new ArrayList<>(rows.entrySet())) {
                LogicalRow row=entry.getValue();if(row.table!=table.id())continue;
                rowCells.remove(row.id);
                if(row.position!=null) {
                    for(int i=0;i<positions.size();i++){RowPosition p=positions.get(i);if(p.id().equals(row.id)&&p.end()==null)positions.set(i,new RowPosition(p.table(),p.id(),p.begin(),revision,p.row()));}
                    positions.add(new RowPosition(row.table,row.id,revision,null,null));
                    rows.put(row.id,new LogicalRow(row.table,row.id,row.original,null,row.operation,row.client));
                }
            }
            for(RowIdentity row:identities)if(row.table()==table.id()&&row.original()!=null&&row.baseline()==revision) {
                require(row.operation()==null&&row.client()==null&&uuid(row.id())&&definitions.get(table.id()).body(row.original()));
                require(rows.putIfAbsent(row.id(),new LogicalRow(row.table(),row.id(),row.original(),row.original(),null,null))==null);
                rowBaselines.put(row.id(),revision);
                originalRows.computeIfAbsent(row.table(),ignored->new HashSet<>()).add(row.original());
                positions.add(new RowPosition(row.table(),row.id(),revision,null,row.original()));
            }
        }
        validatePositions();
    }
    Applied apply(JsonNode delta,String storedRequest,String operation,long revision) {
        exact(delta,"policyTemplateId","commands","tables","rows","columns","cells");
        require(integer(delta.get("policyTemplateId"),1,Integer.MAX_VALUE)==basis.templateId());
        for(String field:List.of("commands","tables","rows","columns","cells"))require(delta.path(field).isArray());
        InputDraftJson.ChangeRequest request;
        try {request=json.changeRequest(new java.io.ByteArrayInputStream(storedRequest.getBytes(StandardCharsets.UTF_8)));}
        catch(RuntimeException ex){throw InputDraftException.internal();}
        require(request.baseRevision()==revision-1&&json.sameValue(json.tree(request.operations()),delta.path("commands")));
        InputDraftColumns beforeColumns=new InputDraftColumns(columns);
        Map<String,LogicalRow> before=new LinkedHashMap<>(rows);
        Map<Long,InputReadDefinition> previousDefinitions=new LinkedHashMap<>(definitions);
        Set<String> affected=new LinkedHashSet<>(),changedTables=new HashSet<>();
        for(JsonNode command:delta.path("commands"))affected.add(command.path("tableId").asText());
        for(JsonNode change:delta.path("tables")) {
            exact(change,"tableId","startRow","endRow","startColumn","endColumn","dataStart","definition","scopes");
            TableRef old=table(change.path("tableId").asText());require(changedTables.add(old.key()));
            int start=integer(change.get("startRow"),1,1048576),end=integer(change.get("endRow"),start,1048576),data=integer(change.get("dataStart"),start,1048576);
            InputReadDefinition def;
            try {def=InputReadDefinition.parse(change.get("definition"));}
            catch(InputDraftException ex){throw InputDraftException.internal();}
            InputReadDefinition previous=definitions.get(old.id());
            for(String field:List.of("schemaVersion","capabilities","columnCreation"))
                require(json.sameValue(json.definition(old.definition()).path(field),change.path("definition").path(field)));
            int startCol=integer(change.get("startColumn"),1,16384),endCol=integer(change.get("endColumn"),startCol,16384);
            require(def.controls().size()==previous.controls().size());
            for(int n=0;n<def.controls().size();n++) {
                ObjectNode a=def.controls().get(n).deepCopy(),b=previous.controls().get(n).deepCopy();a.remove("binding");b.remove("binding");require(json.sameValue(a,b));
            }
            require(data<=end&&def.headerRows().stream().allMatch(row->row>=start&&row<=end)
                &&def.bodyRanges().stream().allMatch(range->range.startRow()>=data&&range.endRow()<=end));
            require(change.path("scopes").isArray());List<Scope> scopes=new ArrayList<>();
            for(JsonNode scope:change.path("scopes")) {
                exact(scope,"startRow","endRow","startColumn","endColumn");
                int first=integer(scope.get("startRow"),start,end),last=integer(scope.get("endRow"),first,end);
                int col=integer(scope.get("startColumn"),startCol,endCol),lastCol=integer(scope.get("endColumn"),col,endCol);
                scopes.add(new Scope(first,last,col,lastCol));
            }
            tables.put(old.id(),new TableRef(old.id(),old.role(),old.sheet(),old.name(),old.description(),start,end,startCol,endCol,
                def.headerRows().isEmpty()?0:def.headerRows().get(0),def.headerRows().isEmpty()?0:def.headerRows().get(def.headerRows().size()-1),data,
                old.rowAdd(),old.columnAdd(),change.path("definition").toString(),List.copyOf(scopes)));
            definitions.put(old.id(),def);affected.add(old.key());
        }
        Map<Long,Map<String,InputDraftColumns.Column>> columnAdditions=new HashMap<>();Set<String> touchedColumns=new HashSet<>();
        for(JsonNode change:delta.path("columns"))table(change.path("tableId").asText());
        for(TableRef table:tables.values()) {
            columnAdditions.put(table.id(),columns.apply(table,delta.path("columns"),touchedColumns));
            columns.validate(table,definitions.get(table.id()));
        }
        validateDefinitionChanges(previousDefinitions,beforeColumns,columnAdditions);
        validateControls();
        relocateRowCells(beforeColumns);
        relocateFixedCells(previousDefinitions,beforeColumns,delta.path("cells"));
        List<RowWrite> writes=new ArrayList<>();Map<String,JsonNode> additions=new LinkedHashMap<>();Set<String> changedRows=new HashSet<>();
        for(JsonNode change:delta.path("rows")) {
            exact(change,"tableId","rowId","clientRowKey","rowNumber");
            TableRef table=table(change.path("tableId").asText());String id=text(change.get("rowId"));
            require(uuid(id)&&changedRows.add(id));Integer position=nullableRow(change.get("rowNumber"));
            require(position==null||definitions.get(table.id()).body(position));LogicalRow old=rows.get(id);
            String client=null;
            if(old==null) {
                client=text(change.get("clientRowKey"));require(client.matches("[A-Za-z][A-Za-z0-9_-]{0,63}")&&additions.putIfAbsent(client,change)==null);
            } else require(old.table==table.id()&&old.position!=null&&change.path("clientRowKey").isNull());
            LogicalRow updated=old==null?new LogicalRow(table.id(),id,null,position,operation,client)
                :new LogicalRow(old.table,old.id,old.original,position,old.operation,old.client);
            rows.put(id,updated);if(old==null)rowBaselines.put(id,tableBaselines.getOrDefault(table.id(),0L));writes.add(new RowWrite(table.id(),id,client,position,old==null));affected.add(table.key());
            if(old!=null) {
                boolean closed=false;
                for(int n=0;n<positions.size();n++) {
                    RowPosition p=positions.get(n);
                    if(p.id().equals(id)&&p.end()==null){require(p.begin()<revision);positions.set(n,new RowPosition(p.table(),p.id(),p.begin(),revision,p.row()));closed=true;}
                }
                require(closed);
            }
            positions.add(new RowPosition(table.id(),id,revision,null,position));
        }
        validatePositions();
        Set<String> cellKeys=new HashSet<>();
        for(JsonNode cell:delta.path("cells")) {
            exact(cell,"tableId","rowId","rowNumber","columnNumber","raw","display","formula");
            TableRef table=table(cell.path("tableId").asText());int col=integer(cell.get("columnNumber"),table.startColumn(),table.endColumn());
            require(columns.active(table.id(),col));
            require(cellKeys.add(table.id()+"/"+cell.path("rowId")+"/"+cell.path("rowNumber")+"/"+col));
            require(scalar(cell.get("raw"))&&scalar(cell.get("display"))&&cell.path("formula").isBoolean());
            require(!cell.path("raw").isNull()||(cell.path("display").isNull()&&!cell.path("formula").booleanValue()));
            InputWorkbookReader.Cell value=new InputWorkbookReader.Cell(value(cell.get("raw")),value(cell.get("display")),cell.path("formula").booleanValue());
            if(cell.path("rowId").isNull()) {
                int row=integer(cell.get("rowNumber"),table.startRow(),table.endRow());require(binding(table.id(),row)==null);
                fixedCells.computeIfAbsent(table.id(),ignored->new HashMap<>()).computeIfAbsent(row,ignored->new HashMap<>()).put(col,value);
            } else {
                String id=text(cell.get("rowId"));LogicalRow row=rows.get(id);require(row!=null&&row.table==table.id()&&row.position!=null&&cell.path("rowNumber").isNull());
                rowCells.computeIfAbsent(id,ignored->new HashMap<>()).put(col,value);
            }
            affected.add(table.key());
        }
        List<Map<String,Object>> columnMappings=columns.commands(request.operations(),beforeColumns,tables,columnAdditions);
        List<Map<String,Object>> mappings=validateCommands(request.operations(),before,additions,columnAdditions);
        validateCreatedValues(request.operations(),columnAdditions,additions);
        Map<String,Object> result=new LinkedHashMap<>();result.put("resultRevision",revision);result.put("rowMappings",mappings);result.put("columnMappings",columnMappings);
        tables.values().stream().map(TableRef::key).filter(affected::contains).filter(key->request.operations().stream().noneMatch(c->key.equals(c.path("tableId").asText()))).sorted().forEach(key->{affected.remove(key);affected.add(key);});
        result.put("affectedTableIds",List.copyOf(affected));
        byte[] encoded=json.bytes(result);require(encoded.length<=InputDraftJson.BODY_LIMIT);
        return new Applied(json.deltaText(delta),new String(encoded,StandardCharsets.UTF_8),List.copyOf(writes));
    }
    private List<Map<String,Object>> validateCommands(List<JsonNode> commands,Map<String,LogicalRow> before,Map<String,JsonNode> additions,Map<Long,Map<String,InputDraftColumns.Column>> columnAdditions) {
        Set<String> live=new HashSet<>();before.values().stream().filter(row->row.position!=null).forEach(row->live.add(row.id));
        Map<String,Map<String,JsonNode>> expected=new LinkedHashMap<>();List<Map<String,Object>> mappings=new ArrayList<>();Set<String> used=new HashSet<>();
        for(JsonNode command:commands) {
            TableRef table=table(command.path("tableId").asText());String type=command.path("type").asText(),id;
            if(type.equals("ADD_ROW")) {
                String client=command.path("clientRowKey").asText();JsonNode addition=additions.get(client);
                require(addition!=null&&addition.path("tableId").asText().equals(table.key())&&used.add(client));id=addition.path("rowId").asText();live.add(id);
                Map<String,Object> mapping=new LinkedHashMap<>();mapping.put("commandId",command.path("commandId").asText());mapping.put("clientRowKey",client);mapping.put("tableId",table.key());mapping.put("rowId",id);
                mapping.put("rowState",rows.get(id).position==null?"DELETED":"ACTIVE");mappings.add(mapping);
                var values=command.path("values").fields();while(values.hasNext()){var value=values.next();expected.computeIfAbsent(id,k->new LinkedHashMap<>()).put(value.getKey(),value.getValue());}
            } else if(!type.equals("ADD_COLUMN")&&!type.equals("DELETE_COLUMN")) {
                JsonNode ref=command.path("rowRef");JsonNode added=additions.get(ref.path("clientRowKey").asText());
                id=ref.has("rowId")?ref.path("rowId").asText():added==null?null:added.path("rowId").asText();
                LogicalRow row=rows.get(id);require(row!=null&&row.table==table.id()&&live.contains(id));
                if(type.equals("DELETE_ROW"))live.remove(id);
                else expected.computeIfAbsent(id,k->new LinkedHashMap<>()).put(command.has("fieldKey")?command.path("fieldKey").asText():columnAdditions.get(table.id()).get(command.path("clientColumnKey").asText()).field(),type.equals("CLEAR")?json.tree(null):command.get("value"));
            }
        }
        require(used.equals(additions.keySet()));
        for(LogicalRow row:rows.values())require(live.contains(row.id)==(row.position!=null));
        for(var row:expected.entrySet()) {
            if(!live.contains(row.getKey()))continue;LogicalRow logical=rows.get(row.getKey());
            for(var field:row.getValue().entrySet()) {
                JsonNode definition=null;for(JsonNode candidate:definitions.get(logical.table).fields())if(candidate.path("fieldKey").asText().equals(field.getKey()))definition=candidate;
                if(definition==null){require(columns.field(logical.table,field.getKey())!=null&&columns.field(logical.table,field.getKey()).position()==null);continue;}InputWorkbookReader.Cell cell=rowCells.getOrDefault(logical.id,Map.of()).get(definition.path("columnNumber").asInt());
                require(cell!=null&&!cell.formula()&&json.sameValue(json.tree(cell.raw()),field.getValue()));
                if(!field.getValue().isNull()) {
                    String type=definition.path("dataType").asText();
                    require(type.equals("TEXT")?field.getValue().isTextual():field.getValue().isNumber());
                    if(type.equals("INTEGER"))try {field.getValue().decimalValue().toBigIntegerExact();}catch(ArithmeticException ex){throw InputDraftException.internal();}
                }
            }
        }
        return List.copyOf(mappings);
    }
    private void validateCreatedValues(List<JsonNode> commands,Map<Long,Map<String,InputDraftColumns.Column>> columnAdditions,Map<String,JsonNode> rowAdditions) {
        for(JsonNode command:commands)if(command.path("type").asText().equals("ADD_COLUMN")) {
            TableRef table=table(command.path("tableId").asText());JsonNode creation=definitions.get(table.id()).columnCreation();require(creation!=null);
            JsonNode submitted=command.path("headerValues");
            submitted.fieldNames().forEachRemaining(key->require(creation.path("headerValues").has(key)));
            var headers=creation.path("headerValues").fields();while(headers.hasNext()) {
                var entry=headers.next();JsonNode rule=entry.getValue(),value=submitted.get(entry.getKey());
                if(value==null)require(!rule.path("create").path("required").asBoolean());
                else require(typeMatches(rule.path("dataType").asText(),value));
            }
            InputDraftColumns.Column column=columnAdditions.get(table.id()).get(command.path("clientColumnKey").asText());
            if(column.position()==null)continue;
            JsonNode field=null;for(JsonNode candidate:definitions.get(table.id()).fields())if(column.field().equals(candidate.path("fieldKey").asText()))field=candidate;
            require(field!=null&&field.path("dataType").equals(creation.path("body").path("dataType")));
            for(LogicalRow row:rows.values())if(row.table==table.id()&&row.position!=null)
                validateInitial(row,column.field(),column.position(),creation.path("body").path("create"),commands,columnAdditions,rowAdditions);
        }
        for(JsonNode addition:rowAdditions.values()) {
            LogicalRow row=rows.get(addition.path("rowId").asText());if(row.position==null)continue;
            for(JsonNode field:definitions.get(row.table).fields())if("READY".equals(field.path("policyStatus").path("CREATE").asText()))
                validateInitial(row,field.path("fieldKey").asText(),field.path("columnNumber").asInt(),field.path("create"),commands,columnAdditions,rowAdditions);
        }
    }
    private void validateInitial(LogicalRow row,String field,int column,JsonNode create,List<JsonNode> commands,
            Map<Long,Map<String,InputDraftColumns.Column>> columnAdditions,Map<String,JsonNode> rowAdditions) {
        boolean explicit=false;
        for(JsonNode command:commands)if(table(command.path("tableId").asText()).id()==row.table) {
            String type=command.path("type").asText();
            if(type.equals("ADD_ROW")&&row.id.equals(rowAdditions.get(command.path("clientRowKey").asText()).path("rowId").asText())&&command.path("values").has(field))explicit=true;
            if(type.equals("SET")||type.equals("CLEAR")) {
                JsonNode ref=command.path("rowRef");String id=ref.has("rowId")?ref.path("rowId").asText():rowAdditions.get(ref.path("clientRowKey").asText()).path("rowId").asText();
                String key=command.has("fieldKey")?command.path("fieldKey").asText():columnAdditions.get(row.table).get(command.path("clientColumnKey").asText()).field();
                if(row.id.equals(id)&&field.equals(key))explicit=true;
            }
        }
        InputWorkbookReader.Cell cell=rowCells.getOrDefault(row.id,Map.of()).get(column);
        if(create.path("required").asBoolean())require(cell!=null&&cell.raw()!=null&&(!(cell.raw() instanceof String text)||!text.isBlank()));
        if(!explicit&&"VALUE".equals(create.path("initial").path("kind").asText()))
            require(cell!=null&&!cell.formula()&&json.sameValue(json.tree(cell.raw()),create.path("initial").get("value")));
    }
    private static boolean typeMatches(String type,JsonNode value) {
        if(type.equals("TEXT"))return value.isTextual();if(!value.isNumber())return false;
        if(type.equals("INTEGER"))try{value.decimalValue().toBigIntegerExact();}catch(ArithmeticException ex){return false;}return true;
    }
    private void validateControls() {
        Map<String,JsonNode> shared=new HashMap<>();
        for(TableRef table:tables.values())for(JsonNode control:definitions.get(table.id()).controls()) {
            TableRef owner=table(control.path("sourceTableId").asText());int row=control.path("binding").path("rowNumber").asInt(),col=control.path("binding").path("columnIndex").asInt()+1;
            require(owner.role().equals(table.role())&&owner.sheet().equals(table.sheet())&&row>=owner.startRow()&&row<=owner.endRow()&&col>=owner.startColumn()&&col<=owner.endColumn());
            InputReadDefinition def=definitions.get(table.id());require(!def.columns().contains(col)||(!def.headerRows().contains(row)&&!def.body(row)));
            JsonNode previous=shared.putIfAbsent(table.role()+"/"+table.sheet()+"/"+control.path("id").asText(),control);require(previous==null||json.sameValue(previous,control));
        }
    }
    private void relocateFixedCells(Map<Long,InputReadDefinition> previous,InputDraftColumns beforeColumns,JsonNode currentCells) {
        Map<CellLocation,Set<CellLocation>> moves=new LinkedHashMap<>();
        for(var entry:previous.entrySet()) {
            long id=entry.getKey();InputReadDefinition old=entry.getValue(),current=definitions.get(id);
            for(int n=0;n<old.headerRows().size();n++) {
                int source=old.headerRows().get(n);
                for(int col:fixedCells.getOrDefault(id,Map.of()).getOrDefault(source,Map.of()).keySet()) {
                    var targets=moves.computeIfAbsent(new CellLocation(id,source,col),ignored->new LinkedHashSet<>());
                    Integer moved=columns.moved(id,col,beforeColumns);
                    if(n<current.headerRows().size()&&moved!=null)targets.add(new CellLocation(id,current.headerRows().get(n),moved));
                }
            }
            for(int n=0;n<old.controls().size();n++) {
                JsonNode from=old.controls().get(n),to=current.controls().get(n);
                long owner=table(from.path("sourceTableId").asText()).id();
                var source=new CellLocation(owner,from.path("binding").path("rowNumber").asInt(),from.path("binding").path("columnIndex").asInt()+1);
                var target=new CellLocation(owner,to.path("binding").path("rowNumber").asInt(),to.path("binding").path("columnIndex").asInt()+1);
                moves.computeIfAbsent(source,ignored->new LinkedHashSet<>()).add(target);
            }
        }
        for(var table:fixedCells.entrySet())for(var row:table.getValue().entrySet())for(int col:row.getValue().keySet()) {
            CellLocation source=new CellLocation(table.getKey(),row.getKey(),col);
            if(!moves.containsKey(source)) {
                Set<CellLocation> targets=new LinkedHashSet<>();Integer moved=columns.moved(table.getKey(),col,beforeColumns);
                if(moved!=null)targets.add(new CellLocation(table.getKey(),row.getKey(),moved));moves.put(source,targets);
            }
        }
        Set<CellLocation> replaced=new HashSet<>();
        for(JsonNode cell:currentCells)if(cell.path("rowId").isNull()&&cell.path("rowNumber").isIntegralNumber()&&cell.path("columnNumber").isIntegralNumber())
            replaced.add(new CellLocation(table(cell.path("tableId").asText()).id(),cell.path("rowNumber").asInt(),cell.path("columnNumber").asInt()));
        Map<CellLocation,InputWorkbookReader.Cell> moved=new LinkedHashMap<>();
        // 공유 control 중복/좌표 교환을 포함해 모든 출발값을 먼저 읽는다.
        for(var move:moves.entrySet()) {
            CellLocation source=move.getKey();var value=fixedCells.getOrDefault(source.table,Map.of()).getOrDefault(source.row,Map.of()).get(source.column);
            if(value!=null)for(CellLocation target:move.getValue()) {
                var existing=moved.putIfAbsent(target,value);
                require(existing==null||existing.equals(value)||replaced.contains(target));
            }
        }
        for(CellLocation source:moves.keySet()) {
            var table=fixedCells.get(source.table);if(table==null)continue;var row=table.get(source.row);
            if(row!=null){row.remove(source.column);if(row.isEmpty())table.remove(source.row);}
        }
        for(var entry:moved.entrySet()) {
            CellLocation target=entry.getKey();fixedCells.computeIfAbsent(target.table,ignored->new HashMap<>())
                .computeIfAbsent(target.row,ignored->new HashMap<>()).put(target.column,entry.getValue());
        }
        // 이번 revision의 명시 cells는 apply의 아래 단계에서 마지막에 적용된다.
    }
    private void relocateRowCells(InputDraftColumns before) {
        for(var entry:rowCells.entrySet()) {
            LogicalRow row=rows.get(entry.getKey());Map<Integer,InputWorkbookReader.Cell> moved=new HashMap<>();
            for(var cell:entry.getValue().entrySet()) {
                Integer target=columns.moved(row.table,cell.getKey(),before);if(target!=null)require(moved.putIfAbsent(target,cell.getValue())==null);
            }
            entry.setValue(moved);
        }
    }
    private void validateDefinitionChanges(Map<Long,InputReadDefinition> before,InputDraftColumns previous,Map<Long,Map<String,InputDraftColumns.Column>> additions) {
        for(TableRef table:tables.values()) {
            InputReadDefinition old=before.get(table.id()),current=definitions.get(table.id());
            for(JsonNode field:old.fields()) {
                String key=field.path("fieldKey").asText();InputDraftColumns.Column column=columns.field(table.id(),key);
                if(column.position()==null)continue;
                JsonNode updated=null;for(JsonNode candidate:current.fields())if(key.equals(candidate.path("fieldKey").asText()))updated=candidate;
                require(updated!=null);ObjectNode a=field.deepCopy(),b=updated.deepCopy();a.remove("columnNumber");b.remove("columnNumber");require(json.sameValue(a,b));
            }
            Set<Integer> visible=new java.util.TreeSet<>();for(int col:old.columns()){Integer moved=columns.moved(table.id(),col,previous);if(moved!=null)visible.add(moved);}
            for(var column:additions.get(table.id()).values())if(column.position()!=null)visible.add(column.position());
            require(current.columns().equals(List.copyOf(visible)));
            List<Integer> identity=new ArrayList<>();for(int col:old.identityColumns()){Integer moved=columns.moved(table.id(),col,previous);if(moved!=null)identity.add(moved);}
            require(current.identityColumns().equals(identity.stream().sorted().toList()));
            for(int n=0;n<old.controls().size();n++) {
                JsonNode from=old.controls().get(n),to=current.controls().get(n);long owner=table(from.path("sourceTableId").asText()).id();
                Integer moved=columns.moved(owner,from.path("binding").path("columnIndex").asInt()+1,previous);
                require(moved!=null&&moved==to.path("binding").path("columnIndex").asInt()+1);
            }
        }
    }
    Integer sourceColumn(long table,int current) {return columns.source(table,current);}
    Set<Integer> sourceColumns(long table,List<Integer> current) {
        Set<Integer> result=new HashSet<>();for(int col:current){Integer source=sourceColumn(table,col);if(source!=null)result.add(source);}return Set.copyOf(result);
    }
    Map<Integer,InputWorkbookReader.Cell> projectSource(long table,Map<Integer,InputWorkbookReader.Cell> source) {
        Map<Integer,InputWorkbookReader.Cell> result=new HashMap<>();
        for(int col:definitions.get(table).columns()){Integer original=sourceColumn(table,col);if(original!=null&&source.containsKey(original))result.put(col,source.get(original));}
        return result;
    }
    private void validatePositions() {
        activeRows.clear();
        for(LogicalRow row:rows.values())if(row.position!=null)require(definitions.get(row.table).body(row.position)
            &&activeRows.computeIfAbsent(row.table,ignored->new HashMap<>()).putIfAbsent(row.position,row)==null);
    }
    private LogicalRow binding(long table,int row) {return activeRows.getOrDefault(table,Map.of()).get(row);}
    Map<Integer,String> bindings(long table) {
        Map<Integer,String> result=new HashMap<>();rows.values().stream().filter(row->row.table==table&&row.position!=null).forEach(row->result.put(row.position,row.id));return result;
    }
    String origin(String id) {LogicalRow row=rows.get(id);return row.original==null?"ADDED":"SOURCE";}
    Integer sourceRow(long table,int current) {
        LogicalRow row=binding(table,current);if(row!=null)return row.original;
        int header=definitions.get(table).headerRows().indexOf(current);
        if(header>=0) {List<Integer> original=originalDefinitions.get(table).headerRows();return header<original.size()?original.get(header):null;}
        if(originalRows.getOrDefault(table,Set.of()).contains(current))return null;
        return originalDefinitions.get(table).body(current)?current:null;
    }
    void overlay(long table,int row,Map<Integer,InputWorkbookReader.Cell> cells) {
        LogicalRow logical=binding(table,row);
        if(logical==null)cells.putAll(fixedCells.getOrDefault(table,Map.of()).getOrDefault(row,Map.of()));
        else cells.putAll(rowCells.getOrDefault(logical.id,Map.of()));
    }
    InputWorkbookReader.Cell control(String table,int row,int col) {return fixedCells.getOrDefault(table(table).id(),Map.of()).getOrDefault(row,Map.of()).get(col);}
    JsonNode originalControl(long table,String id) {for(JsonNode control:originalDefinitions.get(table).controls())if(control.path("id").asText().equals(id))return control;throw InputDraftException.internal();}
    Catalog catalog() {return new Catalog(basis.templateId(),basis.version(),basis.roles(),List.copyOf(tables.values()));}
    Map<Long,InputReadDefinition> definitions() {return Map.copyOf(definitions);}
    private TableRef table(String key) {return tables.values().stream().filter(table->table.key().equals(key)).findFirst().orElseThrow(InputDraftException::internal);}
    private TableRef table(long id) {TableRef table=tables.get(id);require(table!=null);return table;}
    private static boolean uuid(String id) {return id!=null&&id.matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");}
    private static void exact(JsonNode node,String... fields) {
        require(node!=null&&node.isObject()&&node.size()==fields.length);Set<String> allowed=Set.of(fields);
        node.fieldNames().forEachRemaining(field->require(allowed.contains(field)));
    }
    private static String text(JsonNode value) {require(value!=null&&value.isTextual());return value.textValue();}
    private static Integer nullableRow(JsonNode value) {require(value!=null);return value.isNull()?null:integer(value,1,1048576);}
    private static int integer(JsonNode value,int min,int max) {require(value!=null&&value.isIntegralNumber()&&value.canConvertToInt()&&value.intValue()>=min&&value.intValue()<=max);return value.intValue();}
    private static boolean scalar(JsonNode value) {return value!=null&&(value.isNull()||value.isTextual()||value.isNumber()||value.isBoolean());}
    private static Object value(JsonNode value) {return value.isNull()?null:value.isNumber()?value.decimalValue():value.isBoolean()?value.booleanValue():value.textValue();}
    private static void require(boolean valid) {if(!valid)throw InputDraftException.internal();}
}
