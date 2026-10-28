package com.sk.domain.mi.service.fundamentalmodel.input;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sk.domain.shared.repository.master.tmm.ModelInputDraftRepository.TableRef;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Pattern;
import org.apache.poi.ss.util.CellReference;

/** 현재 파일 기준의 원본 identity만 복원한다. 과거 입력은 삭제 직전의 같은 파일에서 읽는다. */
final class InputWorkbookRestoration {
    record Snapshot(InputDraftHistory history, Path file) {}
    interface Past { Snapshot before(String table, String type, String id) throws Exception; }
    private static final Pattern FORMULA = Pattern.compile("<f(?=\\s|/?>)[^>]*?(?:/>|>.*?</f>)",Pattern.DOTALL);

    static void restoreRow(InputWorkbookCommands.Table table,String id,InputDraftHistory history) {
        includeHidden(table,history);
        var target=history.rowStates(table.original.id()).stream().filter(r->r.id().equals(id)&&r.original()!=null&&r.position()==null).findFirst().orElseThrow(InputDraftException::invalid);
        int position=history.rowStates(table.original.id()).stream().filter(r->r.original()!=null&&r.original()>target.original()&&table.position(r.id())!=null)
            .min(Comparator.comparing(InputDraftHistory.RowState::original)).map(r->table.row(r.id())).orElseGet(()->
                history.rowStates(table.original.id()).stream().filter(r->r.original()!=null&&r.original()<target.original()&&table.position(r.id())!=null)
                    .max(Comparator.comparing(InputDraftHistory.RowState::original)).map(r->table.row(r.id())+1)
                    .orElse(table.rows.isEmpty()?table.original.dataStart():table.rows.lastKey()+1));
        table.restoreRow(id,target.original(),position);
    }
    static void restoreColumn(InputWorkbookCommands.Table table,String field,InputDraftHistory history) {
        includeHidden(table,history);
        var target=history.columnStates(table.original.id()).stream().filter(c->field.equals(c.field())&&c.source()!=null&&c.position()==null).findFirst().orElseThrow(InputDraftException::invalid);
        int position=history.columnStates(table.original.id()).stream().filter(c->c.source()!=null&&c.source()>target.source()&&currentColumn(table,c)!=null)
            .min(Comparator.comparing(InputDraftColumns.Column::source)).map(c->currentColumn(table,c)).orElseGet(()->
                history.columnStates(table.original.id()).stream().filter(c->c.source()!=null&&c.source()<target.source()&&currentColumn(table,c)!=null)
                    .max(Comparator.comparing(InputDraftColumns.Column::source)).map(c->currentColumn(table,c)+1).orElse(table.endColumn+1));
        JsonNode definition=InputWorkbookPolicy.field(history.originalDefinition(table.original.id()),field);
        table.restoreColumn(field,target.source(),position,definition);
        if(history.originalDefinition(table.original.id()).identityColumns().contains(target.source())) {
            var identity=new TreeSet<Integer>();table.definition.path("identityColumns").forEach(c->identity.add(c.asInt()));identity.add(position);
            var list=table.definition.putArray("identityColumns");identity.forEach(list::add);
        }
    }

    private static Integer currentColumn(InputWorkbookCommands.Table table,InputDraftColumns.Column column) {
        Integer token=column.field()==null?table.sourceColumns.entrySet().stream().filter(e->Objects.equals(e.getValue(),column.source())).map(Map.Entry::getKey).findFirst().orElse(null):table.fieldTokens.get(column.field());return token==null?null:table.columnPositions.get(token);
    }
    private static void includeHidden(InputWorkbookCommands.Table table,InputDraftHistory history) {
        for(var col:history.columnStates(table.original.id()))if(col.position()!=null) {
            table.columnPositions.putIfAbsent(col.position(),col.position());table.sourceColumns.putIfAbsent(col.position(),col.source());
        }
    }

    static void copy(Path baseline,Path current,InputDraftHistory history,Map<Long,InputWorkbookCommands.Table> tables,
            Map<String,InputWorkbookXml.SheetPatch> sheets,List<InputWorkbookStructure.ChainChange> chains,
            Set<InputWorkbookCommands.Cell> readBack,Map<InputWorkbookCommands.Cell,Object> values,
            InputDraftJson.ChangeRequest request,Past past,InputWorkbookReader reader,InputWorkbookXml.Checkpoint checkpoint,InputDraftJson json) throws Exception {
        Map<String,Snapshot> pastSnapshots=new HashMap<>();
        for(var table:tables.values()) {
            if(table.restoredRows.isEmpty()&&table.restoredColumns.isEmpty())continue;
            if(past==null)throw new InputDraftPreserveChange.Failure("PRESERVATION_FAILED");
            var patch=sheets.get(table.original.sheet());Set<String> addedFormulas=new LinkedHashSet<>();
            Map<Path,Map<Integer,Map<Integer,String>>> templates=new HashMap<>();
            Map<Path,Map<String,String>> formulas=new HashMap<>();
            Map<Path,Map<Integer,Map<Integer,InputWorkbookReader.Cell>>> typed=new HashMap<>();
            Map<Integer,Integer> originalRows=new HashMap<>();history.rowStates(table.original.id()).forEach(r->{if(r.original()!=null&&table.position(r.id())!=null)originalRows.put(table.row(r.id()),r.original());});
            Map<Integer,Integer> originalColumns=new HashMap<>();table.sourceColumns.forEach((token,source)->{Integer pos=table.columnPositions.get(token);if(pos!=null&&source!=null)originalColumns.put(pos,source);});
            // 原本의 비편집 표시 열도 baseline identity로 대응한다.
            for(var c:history.columnStates(table.original.id()))if(c.source()!=null&&c.position()!=null) {
                Integer pos=table.columnPositions.get(c.position());if(pos!=null)originalColumns.put(pos,c.source());
            }
            Set<Integer> targetRows=new TreeSet<>(table.rows.keySet());targetRows.addAll(table.current().headerRows());if(!table.restoredColumns.isEmpty())table.current().bodyRanges().forEach(range->{for(int r=range.startRow();r<=range.endRow();r++)targetRows.add(r);});
            for(int row:targetRows)for(int col:new TreeSet<>(table.columnPositions.values().stream().filter(Objects::nonNull).toList())) {
                String rowId=table.rows.get(row);boolean restoredRow=rowId!=null&&table.restoredRows.containsKey(rowId);
                String restoredField=table.restoredColumns.keySet().stream().filter(f->table.columnPositions.get(table.fieldTokens.get(f))==col).findFirst().orElse(null);
                if(!restoredRow&&restoredField==null)continue;
                checkpoint.check();Integer sourceRow=originalRows.get(row),sourceCol=originalColumns.get(col);Path source=baseline;
                Snapshot from=null;
                int headerIndex=table.current().headerRows().indexOf(row);
                if(headerIndex>=0)sourceRow=history.originalDefinition(table.original.id()).headerRows().get(headerIndex);
                else if(sourceRow==null&&rowId==null&&table.slots.get(row)!=null)sourceRow=history.sourceRow(table.original.id(),table.slots.get(row));
                JsonNode command=request.operations().stream().filter(c->c.path("tableId").asText().equals(table.original.key())&&c.path("type").asText().startsWith("RESTORE_")).findFirst().orElse(null);
                if(command==null)command=request.operations().stream().filter(c->c.path("type").asText().equals("RESTORE_ROW")).findFirst().orElseThrow(InputDraftException::invalid);
                if(sourceRow==null||sourceCol==null) {
                    String type=restoredRow?"RESTORE_ROW":"RESTORE_COLUMN",id=restoredRow?rowId:restoredField;
                    String key=table.original.key()+"/"+type+"/"+id;Snapshot previous=pastSnapshots.get(key);
                    if(previous==null){previous=past.before(table.original.key(),type,id);pastSnapshots.put(key,previous);}from=previous;source=previous.file();
                    sourceRow=rowId==null?null:previous.history().rowStates(table.original.id()).stream().filter(r->r.id().equals(rowId)).map(InputDraftHistory.RowState::position).filter(Objects::nonNull).findFirst().orElse(null);
                    String field=table.fieldTokens.entrySet().stream().filter(e->Objects.equals(table.columnPositions.get(e.getValue()),col)).map(Map.Entry::getKey).findFirst().orElse(null);
                    sourceCol=field==null?null:previous.history().columnStates(table.original.id()).stream().filter(c->field.equals(c.field())).map(InputDraftColumns.Column::position).filter(Objects::nonNull).findFirst().orElse(null);
                }
                String raw=null;
                if(sourceRow!=null&&sourceCol!=null) {
                    var rows=templates.get(source);
                    if(rows==null){rows=new HashMap<>();var loaded=rows;InputWorkbookXml.rows(source,InputWorkbookXml.sheetParts(source).get(table.original.sheet()),checkpoint,r->{if(r.number()>=history.originalTable(table.original.id()).startRow()&&r.number()<=Math.max(history.originalTable(table.original.id()).endRow(),table.endRow))loaded.put(r.number(),InputWorkbookXml.cells(r.xml()));});templates.put(source,rows);}
                    raw=rows.getOrDefault(sourceRow,Map.of()).get(sourceCol);
                    if(raw!=null) {
                        var all=formulas.get(source);if(all==null){
                            all=InputWorkbookStructure.formulas(source,table.original.sheet(),a->{var ref=new CellReference(a);return ref.getRow()+1>=history.originalTable(table.original.id()).startRow()&&ref.getRow()+1<=Math.max(history.originalTable(table.original.id()).endRow(),table.endRow)&&ref.getCol()+1>=table.original.startColumn()&&ref.getCol()+1<=Math.max(history.originalTable(table.original.id()).endColumn(),table.endColumn);},checkpoint);formulas.put(source,all);
                        }
                        String formula=all.get(InputWorkbookXml.address(sourceRow,sourceCol));
                        raw=InputWorkbookXml.replaceAttribute(raw,"r",InputWorkbookXml.address(row,col));
                        if(formula!=null&&specialized(table,sourceCol)) {raw=InputWorkbookXml.valueCell(raw,InputWorkbookXml.address(row,col),null);}
                        else if(formula!=null) {raw=FORMULA.matcher(raw).replaceFirst(java.util.regex.Matcher.quoteReplacement("<f>"+InputWorkbookXml.escape(remap(formula,table.original.sheet(),from==null?null:from.history(),history,tables))+"</f>"));if(InputWorkbookPolicy.kind(table.original)!=InputWorkbookPolicy.Kind.REGAS_MONTHLY)addedFormulas.add(InputWorkbookXml.address(row,col));}
                    }
                } else {
                    JsonNode field=java.util.stream.StreamSupport.stream(table.current().fields().spliterator(),false).filter(f->f.path("columnNumber").asInt()==col).findFirst().orElse(null);
                    if(field==null||!field.path("policyStatus").path("CREATE").asText().equals("READY"))throw InputDraftException.conflict("POLICY_UNSUPPORTED",request.baseRevision());
                    JsonNode initial=InputWorkbookPolicy.initial(field.path("create"));
                    if(field.path("create").path("required").asBoolean()&&(initial==null||initial.isNull()))throw InputDraftException.conflict("POLICY_UNSUPPORTED",request.baseRevision());
                    Object value=initial==null?null:initial.isNumber()?initial.decimalValue():initial.asText();
                    raw=InputWorkbookXml.valueCell(null,InputWorkbookXml.address(row,col),value);
                }
                JsonNode field=null;for(JsonNode f:table.current().fields())if(f.path("columnNumber").asInt()==col)field=f;
                if(field!=null&&"READY".equals(field.path("policyStatus").path("SET").asText())&&!(raw!=null&&InputWorkbookXml.hasFormula(raw))&&!(sourceCol!=null&&specialized(table,sourceCol))) {
                    Object value=null;
                    if(sourceRow!=null&&sourceCol!=null) {
                        var cache=typed.computeIfAbsent(source,ignored->new HashMap<>());
                        if(cache.isEmpty()) {
                            var cols=new HashSet<Integer>();for(int c=table.original.startColumn();c<=Math.max(history.originalTable(table.original.id()).endColumn(),table.endColumn);c++)cols.add(c);
                            int last=Math.max(history.originalTable(table.original.id()).endRow(),table.endRow);
                            reader.readLocal(source,List.of(new InputWorkbookReader.Window(table.original.sheet(),r->r>=history.originalTable(table.original.id()).startRow()&&r<=last,cols,(r,v)->cache.put(r,v),r->{throw InputDraftException.internal();},last)));
                        }
                        var cell=cache.getOrDefault(sourceRow,Map.of()).get(sourceCol);value=cell==null?null:cell.raw();
                    } else {JsonNode initial=InputWorkbookPolicy.initial(field.path("create"));value=initial==null?null:initial.isNumber()?initial.decimalValue():initial.asText();}
                    if(value==null)InputWorkbookPolicy.policy(field.path("policyStatus").path("CLEAR").asText(),request.baseRevision(),command,request,"CLEAR_NOT_ALLOWED");
                    else InputWorkbookPolicy.value(field,json.tree(value),field.path("setConstraints"),command,request,request.baseRevision());
                    var cell=new InputWorkbookCommands.Cell(table.original.id(),rowId,row,col);if(!values.containsKey(cell))values.put(cell,value);
                }
                patch.raw(row,col,raw);readBack.add(new InputWorkbookCommands.Cell(table.original.id(),rowId,row,col));
            }
            chains.add(new InputWorkbookStructure.ChainChange(InputWorkbookStructure.sheetId(current,table.original.sheet()),Map.of(),Set.of(),List.copyOf(addedFormulas)));
        }
    }
    private static boolean specialized(InputWorkbookCommands.Table table,int column) {
        var kind=InputWorkbookPolicy.kind(table.original);
        return kind==InputWorkbookPolicy.Kind.REGAS&&(column==1||column==table.sourceColumns.values().stream().filter(Objects::nonNull).mapToInt(Integer::intValue).max().orElse(table.endColumn))
            ||kind==InputWorkbookPolicy.Kind.REGAS_MONTHLY&&(column==7||column>=32&&column<=115||column>=118&&column<=126)
            ||InputDraftPmiRows.selected(table.original)&&column==15;
    }
    private static Map<Long,InputWorkbookCommands.Table> plannedLayouts(InputDraftHistory history,JsonNode command,InputDraftJson json) {
        Map<Long,InputWorkbookCommands.Table> layouts=new LinkedHashMap<>();
        TableRef owner=history.catalog().tables().stream().filter(t->t.key().equals(command.path("tableId").asText())).findFirst().orElseThrow(InputDraftException::missing);
        if(command.path("type").asText().equals("RESTORE_ROW")) {
            Set<String> ids=history.restoredRowIds(List.of(command));
            for(TableRef table:history.catalog().tables())for(var row:history.rowStates(table.id()))if(ids.contains(row.id())) {
                var layout=layouts.computeIfAbsent(table.id(),ignored->new InputWorkbookCommands.Table(table,history,json));restoreRow(layout,row.id(),history);
            }
        } else {var layout=new InputWorkbookCommands.Table(owner,history,json);restoreColumn(layout,command.path("fieldKey").asText(),history);layouts.put(owner.id(),layout);}
        return layouts;
    }
    static void verifyReferences(InputDraftHistory history,JsonNode command,Map<String,Map<String,String>> formulas,InputDraftJson json) {
        var layouts=plannedLayouts(history,command,json);
        for(var layout:layouts.values()) {
            Set<Integer> rows=new HashSet<>(layout.restoredRows.values()),columns=new HashSet<>(layout.restoredColumns.values());
            for(var formula:formulas.getOrDefault(layout.original.sheet(),Map.of()).entrySet()) {
                var ref=new CellReference(formula.getKey());if(!rows.contains(ref.getRow()+1)&&!columns.contains(ref.getCol()+1))continue;
                if(ref.getRow()+1<history.originalTable(layout.original.id()).startRow()||ref.getRow()+1>history.originalTable(layout.original.id()).endRow()||ref.getCol()+1<history.originalTable(layout.original.id()).startColumn()||ref.getCol()+1>history.originalTable(layout.original.id()).endColumn())continue;
                if(specialized(layout,ref.getCol()+1))continue;
                String expression=remap(formula.getValue(),layout.original.sheet(),null,history,layouts);
                if(!InputWorkbookCalculation.supportsExpression(expression))throw new InputDraftPreserveChange.Failure("POLICY_UNSUPPORTED");
            }
        }
    }

    /** 추가 축 교차는 삭제 직전 이력 또는 승인 CREATE 값으로만 READY를 판정한다. 파일 재생은 필요 없다. */
    static void verifyCrossings(InputDraftHistory history,JsonNode command,java.util.function.Function<String,InputDraftHistory> before,InputDraftJson json,long revision) {
        var request=new InputDraftJson.ChangeRequest(UUID.randomUUID().toString(),revision,List.of(command));
        for(var layout:plannedLayouts(history,command,json).values()) {
            InputDraftHistory past=null;Map<Integer,Map<Integer,InputWorkbookReader.Cell>> previousValues=new HashMap<>();Map<String,Integer> previousRows=new HashMap<>(),previousColumns=new HashMap<>();
            Set<String> addedRows=new HashSet<>();history.rowStates(layout.original.id()).stream().filter(r->r.original()==null&&r.position()!=null).forEach(r->addedRows.add(r.id()));
            for(var row:layout.rows.entrySet())for(JsonNode field:layout.current().fields()) {
                String key=field.path("fieldKey").asText();boolean restoredRow=layout.restoredRows.containsKey(row.getValue()),restoredColumn=layout.restoredColumns.containsKey(key);
                if(!restoredRow&&!restoredColumn)continue;
                boolean addedColumn=layout.sourceColumns.get(layout.fieldTokens.get(key))==null;
                if(!addedRows.contains(row.getValue())&&!addedColumn)continue;
                InputTempFiles.checkpoint();if(past==null){past=before.apply(layout.original.key());past.rowStates(layout.original.id()).forEach(r->{if(r.position()!=null)previousRows.put(r.id(),r.position());});past.columnStates(layout.original.id()).forEach(c->{if(c.position()!=null&&c.field()!=null)previousColumns.put(c.field(),c.position());});}
                Integer previousRow=previousRows.get(row.getValue()),previousColumn=previousColumns.get(key);
                Object value;
                if(previousRow!=null&&previousColumn!=null) {
                    var cells=previousValues.get(previousRow);if(cells==null){cells=new HashMap<>();past.overlay(layout.original.id(),previousRow,cells);previousValues.put(previousRow,cells);}
                    var cell=cells.get(previousColumn);if(cell!=null&&cell.formula())throw new InputDraftPreserveChange.Failure("POLICY_UNSUPPORTED");value=cell==null?null:cell.raw();
                } else {
                    if(!field.path("policyStatus").path("CREATE").asText().equals("READY"))throw new InputDraftPreserveChange.Failure("POLICY_UNSUPPORTED");
                    JsonNode initial=InputWorkbookPolicy.initial(field.path("create"));
                    if(field.path("create").path("required").asBoolean()&&(initial==null||initial.isNull()))throw new InputDraftPreserveChange.Failure("POLICY_UNSUPPORTED");
                    value=initial==null?null:json.scalarValue(initial);
                }
                if(field.path("policyStatus").path("SET").asText().equals("READY")) {
                    if(value==null)InputWorkbookPolicy.policy(field.path("policyStatus").path("CLEAR").asText(),revision,command,request,"CLEAR_NOT_ALLOWED");
                    else InputWorkbookPolicy.value(field,json.tree(value),field.path("setConstraints"),command,request,revision);
                }
            }
        }
    }

    static boolean recalculate(Path file,Map<Long,InputWorkbookCommands.Table> tables,Map<String,InputWorkbookXml.SheetPatch> patches,
            InputWorkbookReader reader,InputWorkbookXml.Checkpoint checkpoint) throws Exception {
        Map<InputWorkbookCalculation.Address,String> formulas=new LinkedHashMap<>();Map<InputWorkbookCalculation.Address,String> raw=new HashMap<>();
        for(var table:tables.values())if((!table.restoredRows.isEmpty()||!table.restoredColumns.isEmpty())&&InputWorkbookPolicy.kind(table.original)!=InputWorkbookPolicy.Kind.REGAS&&InputWorkbookPolicy.kind(table.original)!=InputWorkbookPolicy.Kind.REGAS_MONTHLY&&!InputDraftPmiRows.selected(table.original)) {
            Set<Integer> rows=new HashSet<>();table.restoredRows.keySet().forEach(id->rows.add(table.row(id)));Set<Integer> columns=new HashSet<>();table.restoredColumns.keySet().forEach(f->columns.add(table.columnPositions.get(table.fieldTokens.get(f))));
            var expanded=InputWorkbookStructure.formulas(file,table.original.sheet(),a->{var c=new CellReference(a);return (rows.contains(c.getRow()+1)||columns.contains(c.getCol()+1))&&c.getRow()+1>=table.original.startRow()&&c.getRow()+1<=table.endRow&&c.getCol()+1>=table.original.startColumn()&&c.getCol()+1<=table.endColumn;},checkpoint);
            expanded.forEach((address,formula)->formulas.put(new InputWorkbookCalculation.Address(table.original.sheet(),address),formula));
            InputWorkbookXml.rows(file,InputWorkbookXml.sheetParts(file).get(table.original.sheet()),checkpoint,row->InputWorkbookXml.cells(row.xml()).forEach((col,xml)->{var address=new InputWorkbookCalculation.Address(table.original.sheet(),InputWorkbookXml.address(row.number(),col));if(formulas.containsKey(address))raw.put(address,xml);}));
        }
        if(formulas.isEmpty())return false;
        Map<String,int[]> bounds=new LinkedHashMap<>();
        formulas.forEach((address,formula)->InputWorkbookFormula.remap(formula,address.sheet(),(sheet,row,col)->{int[] range=bounds.computeIfAbsent(sheet,k->new int[]{col,col});range[0]=Math.min(range[0],col);range[1]=Math.max(range[1],col);return new InputWorkbookFormula.Position(row,col);}));
        Map<InputWorkbookCalculation.Address,Object> constants=new LinkedHashMap<>();List<InputWorkbookReader.Window> windows=new ArrayList<>();List<InputWorkbookCalculation.KnownRange> known=new ArrayList<>();
        bounds.forEach((sheet,range)->{Set<Integer> columns=new HashSet<>();for(int col=range[0];col<=range[1];col++)columns.add(col);
            known.add(new InputWorkbookCalculation.KnownRange(sheet,new InputWorkbookStructure.Area(1,1048576,range[0],range[1])));
            windows.add(new InputWorkbookReader.Window(sheet,r->true,columns,(row,cells)->{checkpoint.check();cells.forEach((col,cell)->{var address=new InputWorkbookCalculation.Address(sheet,InputWorkbookXml.address(row,col));if(!formulas.containsKey(address)&&cell.raw()!=null)constants.put(address,cell.raw());});if(constants.size()+formulas.size()>250000)throw new InputDraftPreserveChange.Failure("CALCULATION_FAILED");}));});
        reader.readLocal(file,windows);var calculated=InputWorkbookCalculation.evaluate(constants,formulas,known,checkpoint);
        calculated.forEach((address,value)->{var ref=new CellReference(address.cell());patches.get(address.sheet()).raw(ref.getRow()+1,ref.getCol()+1,InputWorkbookCalculation.cache(raw.get(address),value));});return true;
    }

    private static String remap(String formula,String sheet,InputDraftHistory from,InputDraftHistory current,Map<Long,InputWorkbookCommands.Table> layouts) {
        return InputWorkbookFormula.remap(formula,sheet,(owner,row,col)->{
            for(TableRef candidate:current.catalog().tables()) {
                TableRef before=from==null?current.originalTable(candidate.id()):from.catalog().tables().stream().filter(t->t.id()==candidate.id()).findFirst().orElseThrow(InputDraftException::internal);
                if(!before.sheet().equals(owner)||row<before.startRow()||row>before.endRow()||col<before.startColumn()||col>before.endColumn())continue;
                var layout=layouts.get(candidate.id());Integer mappedRow=row,mappedCol=col;
                var sourceRows=(from==null?current:from).rowStates(candidate.id());
                var sourceRow=sourceRows.stream().filter(r->Objects.equals(from==null?r.original():r.position(),row)).findFirst().orElse(null);
                if(sourceRow!=null)mappedRow=layout==null?current.rowStates(candidate.id()).stream().filter(r->r.id().equals(sourceRow.id())).map(InputDraftHistory.RowState::position).filter(Objects::nonNull).findFirst().orElse(null):layout.position(sourceRow.id());
                else {
                    var oldDefinition=from==null?current.originalDefinition(candidate.id()):from.definitions().get(candidate.id());int header=oldDefinition.headerRows().indexOf(row);
                    var definition=layout==null?current.definitions().get(candidate.id()):layout.current();
                    if(header>=0)mappedRow=definition.headerRows().get(header);
                    else if(row==before.endRow())mappedRow=layout==null?candidate.endRow():layout.endRow;
                    else if(layout!=null&&layout.rowsChanged())throw new InputDraftPreserveChange.Failure("POLICY_UNSUPPORTED");
                }
                var sourceColumn=(from==null?current:from).columnStates(candidate.id()).stream().filter(c->Objects.equals(from==null?c.source():c.position(),col)).findFirst().orElse(null);
                if(sourceColumn!=null) {
                    if(layout!=null)mappedCol=currentColumn(layout,sourceColumn);
                    else mappedCol=current.columnStates(candidate.id()).stream().filter(c->sourceColumn.field()!=null?sourceColumn.field().equals(c.field()):Objects.equals(sourceColumn.source(),c.source())).map(InputDraftColumns.Column::position).filter(Objects::nonNull).findFirst().orElse(null);
                }
                return mappedRow==null||mappedCol==null?null:new InputWorkbookFormula.Position(mappedRow,mappedCol);
            }
            return new InputWorkbookFormula.Position(row,col);
        });
    }

}
