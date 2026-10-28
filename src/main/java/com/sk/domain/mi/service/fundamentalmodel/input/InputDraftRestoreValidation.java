package com.sk.domain.mi.service.fundamentalmodel.input;

import com.fasterxml.jackson.databind.JsonNode;
import com.sk.domain.shared.repository.master.tmm.ModelInputDraftRepository.*;
import java.util.*;

/** 복원 target의 고정 정책과 요청 순서를 검사한 뒤 기존 값 검증기를 재사용한다. */
final class InputDraftRestoreValidation {
    static void validate(ModelInputDraftService.Source source,InputDraftHistory history,InputDraftJson.ChangeRequest request,long revision) {
        Map<Long,InputWorkbookCommands.Table> layouts=new LinkedHashMap<>();
        Set<String> liveRows=new HashSet<>(),liveColumns=new HashSet<>();
        for(TableRef t:source.catalog().tables()) {
            history.rowStates(t.id()).stream().filter(r->r.position()!=null).forEach(r->liveRows.add(t.key()+"/"+r.id()));
            history.columnStates(t.id()).stream().filter(c->c.position()!=null&&c.field()!=null).forEach(c->liveColumns.add(t.key()+"/"+c.field()));
        }
        for(JsonNode c:request.operations()) {
            TableRef table=source.catalog().tables().stream().filter(t->t.key().equals(c.path("tableId").asText())).findFirst().orElseThrow(InputDraftException::missing);
            String type=c.path("type").asText();
            if(type.startsWith("RESTORE_")) {
                String action=type.replace("RESTORE_","DELETE_");var definition=source.definitions().get(table.id());
                InputWorkbookPolicy.policy(definition.capabilities().path(action).asText(),revision,c,request,"ACTION_NOT_ALLOWED");
                if(!InputWorkbookPolicy.supports(table,definition,action))throw InputDraftException.conflict("POLICY_UNSUPPORTED",revision);
                var layout=layouts.computeIfAbsent(table.id(),ignored->new InputWorkbookCommands.Table(table,history,new InputDraftJson(new com.fasterxml.jackson.databind.ObjectMapper())));
                if(type.equals("RESTORE_ROW")) {
                    String id=c.path("rowId").asText();history.restoredRowIds(List.of(c));InputWorkbookRestoration.restoreRow(layout,id,history);liveRows.add(table.key()+"/"+id);
                } else {
                    String field=c.path("fieldKey").asText();
                    JsonNode original=InputWorkbookPolicy.field(history.originalDefinition(table.id()),field);
                    InputWorkbookPolicy.policy(original.path("policyStatus").path("DELETE_COLUMN").asText(),revision,c,request,"ACTION_NOT_ALLOWED");
                    InputWorkbookRestoration.restoreColumn(layout,field,history);liveColumns.add(table.key()+"/"+field);
                }
            } else {
                if(c.path("rowRef").has("rowId")&&!liveRows.contains(table.key()+"/"+c.path("rowRef").path("rowId").asText()))throw InputDraftException.invalid();
                if(c.has("fieldKey")&&!liveColumns.contains(table.key()+"/"+c.path("fieldKey").asText()))throw InputDraftException.invalid();
            }
        }
        Map<Long,InputReadDefinition> definitions=new HashMap<>(source.definitions());List<TableRef> tables=new ArrayList<>();Map<Long,Map<Integer,String>> bindings=new HashMap<>();
        Set<String> addedRows=new HashSet<>(),addedColumns=new HashSet<>();
        for(TableRef table:source.catalog().tables()) {
            var layout=layouts.get(table.id());
            if(layout!=null) {definitions.put(table.id(),layout.current());tables.add(effective(layout,history));bindings.put(table.id(),Map.copyOf(layout.rows));}
            else {tables.add(table);bindings.put(table.id(),history.bindings(table.id()));}
            history.rowStates(table.id()).stream().filter(r->r.original()==null&&r.position()!=null).forEach(r->addedRows.add(r.id()));
            history.columnStates(table.id()).stream().filter(c->c.source()==null&&c.position()!=null&&c.field()!=null).forEach(c->addedColumns.add(table.key()+"/"+c.field()));
        }
        var effective=new ModelInputDraftService.Source(source.caseId(),new Catalog(source.catalog().templateId(),source.catalog().version(),source.catalog().roles(),tables),source.files(),source.key(),source.version(),source.inputIds(),definitions);
        var ordinary=request.operations().stream().filter(c->!c.path("type").asText().startsWith("RESTORE_")).toList();
        if(!ordinary.isEmpty())InputWorkbookPolicy.validate(effective,bindings,addedRows,addedColumns,new InputDraftJson.ChangeRequest(request.requestId(),revision,ordinary),revision);
    }
    static TableRef effective(InputWorkbookCommands.Table layout,InputDraftHistory history) {
        TableRef old=layout.original;
        return new TableRef(old.id(),old.role(),old.sheet(),old.name(),old.description(),old.startRow(),layout.endRow,old.startColumn(),layout.endColumn,old.headerStart(),old.headerEnd(),old.dataStart(),old.rowAdd(),old.columnAdd(),layout.definition.toString(),scopes(layout,history));
    }
    static List<Scope> scopes(InputWorkbookCommands.Table layout,InputDraftHistory history) {
        Set<String> addedIds=new HashSet<>();history.rowStates(layout.original.id()).stream().filter(r->r.original()==null).forEach(r->addedIds.add(r.id()));
        List<Scope> result=new ArrayList<>();
        for(JsonNode field:layout.current().fields()) {
            String key=field.path("fieldKey").asText();int col=field.path("columnNumber").asInt(),token=layout.fieldTokens.get(key);
            boolean restoredColumn=layout.restoredColumns.containsKey(key);Integer baselineColumn=layout.sourceColumns.get(token);
            TreeSet<Integer> allowed=new TreeSet<>();
            for(int row:layout.slots.keySet()) {
                String id=layout.rows.get(row);boolean restoredRow=id!=null&&layout.restoredRows.containsKey(id);
                Integer previousRow=layout.slots.get(row);boolean added=id!=null&&(layout.addedRows.containsValue(id)||addedIds.contains(id));
                TableRef owner;Integer sourceRow,sourceColumn;
                if(restoredColumn||restoredRow&&baselineColumn!=null) {
                    owner=history.originalTable(layout.original.id());sourceColumn=baselineColumn;
                    sourceRow=restoredRow?layout.restoredRows.get(id):previousRow==null?null:history.sourceRow(layout.original.id(),previousRow);
                } else {owner=layout.original;sourceColumn=token;sourceRow=previousRow;}
                if(sourceColumn==null)continue;final int column=sourceColumn;
                if((added||restoredRow&&baselineColumn==null)&&owner.scopes().stream().anyMatch(scope->column>=scope.startColumn()&&column<=scope.endColumn()))allowed.add(row);
                else if(sourceRow!=null) {final int r=sourceRow;if(owner.scopes().stream().anyMatch(scope->scope.contains(r,column)))allowed.add(row);}
            }
            int begin=-1,last=-1;for(int row:allowed){if(begin>=0&&row!=last+1){result.add(new Scope(begin,last,col,col));begin=-1;}if(begin<0)begin=row;last=row;}if(begin>=0)result.add(new Scope(begin,last,col,col));
        }
        return List.copyOf(result);
    }
}
