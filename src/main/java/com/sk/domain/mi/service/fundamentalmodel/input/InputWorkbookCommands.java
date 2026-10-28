package com.sk.domain.mi.service.fundamentalmodel.input;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sk.domain.shared.repository.master.tmm.ModelInputDraftRepository.*;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;

/** 요청 순서대로 논리 identity를 배정하고, 같은 계획으로 실파일과 v2 결과를 만든다. */
final class InputWorkbookCommands {
    record Cell(long table, String rowId, int row, int column) {}
    private record AddedColumn(String field, String client, int token, Map<Integer, Object> headers) {}
    private record Axis(int first, int last, Integer shift) {}
    static final class Table {
        final TableRef original;
        final InputReadDefinition before;
        final ObjectNode definition;
        final TreeMap<Integer, String> rows;
        final Map<String, String> addedRows = new LinkedHashMap<>();
        final Map<String, Integer> restoredRows = new LinkedHashMap<>();
        final Map<String, Integer> restoredColumns = new LinkedHashMap<>();
        final Map<String, Integer> originalRows = new LinkedHashMap<>();
        final TreeMap<Integer, Integer> slots = new TreeMap<>();
        final Set<Integer> removed = new HashSet<>();
        final List<AddedColumn> addedColumns = new ArrayList<>();
        // token은 이번 요청 시작 위치 또는 신규 배정 위치다. 삭제/이동 후에도 제출값의 대상을 바꾸지 않는다.
        final Map<Integer, Integer> columnPositions = new LinkedHashMap<>();
        final Map<Integer, Integer> sourceColumns = new LinkedHashMap<>();
        final Map<String, Integer> fieldTokens = new LinkedHashMap<>();
        final Set<Integer> removedColumns = new HashSet<>();
        final Set<Integer> visibleTokens = new HashSet<>();
        Map<Integer, Map<Integer, Object>> periodHeaders;
        final Map<String, JsonNode> changedYears = new HashMap<>();
        final int styleRow;
        int endRow;
        int endColumn;
        int rowOffset;
        Table(TableRef table, InputDraftHistory history, InputDraftJson json) {
            original = table; before = history.definitions().get(table.id());visibleTokens.addAll(before.columns());
            definition = (ObjectNode) json.definition(table.definition()).deepCopy();
            rows = new TreeMap<>(history.bindings(table.id()));
            for (int column : before.columns()) { columnPositions.put(column, column); sourceColumns.put(column, history.sourceColumn(table.id(), column)); }
            if (InputWorkbookPolicy.kind(table) == InputWorkbookPolicy.Kind.REGAS) {
                columnPositions.put(table.endColumn(), table.endColumn()); sourceColumns.put(table.endColumn(), history.sourceColumn(table.id(), table.endColumn()));
                if (table.startColumn() == 1) { columnPositions.put(1, 1); sourceColumns.put(1, 1); }
            }
            if (InputWorkbookPolicy.kind(table) == InputWorkbookPolicy.Kind.REGAS_MONTHLY) for (int column = 117; column <= 126; column++) {
                columnPositions.put(column, column); sourceColumns.put(column, history.sourceColumn(table.id(), column));
            }
            before.fields().forEach(field -> fieldTokens.put(field.path("fieldKey").asText(), field.path("columnNumber").asInt()));
            rows.forEach((row, id) -> originalRows.put(id, row));
            for (var range : before.bodyRanges()) for (int row = range.startRow(); row <= range.endRow(); row++) slots.put(row, row);
            styleRow = rows.isEmpty() ? before.bodyRanges().isEmpty() ? table.startRow() : before.bodyRanges().get(0).startRow() : rows.lastKey();
            endRow = table.endRow(); endColumn = table.endColumn();
        }
        InputReadDefinition current() { return InputReadDefinition.parse(definition); }
        List<String> createdRows() {var ids=new ArrayList<>(addedRows.values());ids.addAll(restoredRows.keySet());return ids;}
        int row(String id) { return rows.entrySet().stream().filter(e -> e.getValue().equals(id)).map(Map.Entry::getKey).findFirst().orElseThrow(InputDraftException::missing); }
        Integer position(String id) { return rows.entrySet().stream().filter(e -> e.getValue().equals(id)).map(Map.Entry::getKey).findFirst().orElse(null); }
        int column(AddedColumn added) { return columnPositions.get(added.token); }
        List<AddedColumn> liveColumns() { return addedColumns.stream().filter(c -> columnPositions.get(c.token) != null).toList(); }
        int templateColumn() { return before.columns().stream().filter(c -> c >= 7).mapToInt(Integer::intValue).max().orElse(InputWorkbookPolicy.kind(original) == InputWorkbookPolicy.Kind.LIQUTIL ? 7 : 90); }
        boolean columnsChanged() { return !addedColumns.isEmpty() || !removedColumns.isEmpty() || !restoredColumns.isEmpty(); }
        boolean columnsMoved() { return sourceColumns.keySet().stream().anyMatch(column -> !java.util.Objects.equals(columnPositions.get(column), column)); }
        boolean rowsChanged() { return !restoredRows.isEmpty() || rows.size() != originalRows.size() || originalRows.entrySet().stream().anyMatch(row -> !java.util.Objects.equals(position(row.getKey()), row.getValue())); }
        void restoreRow(String id, int source, int position) {
            if (rows.containsValue(id) || position < original.dataStart() || position > 1048576) throw InputDraftException.invalid();
            var shifted=new TreeMap<>(rows.tailMap(position,true));shifted.keySet().forEach(rows::remove);
            shifted.forEach((row,value)->rows.put(row+1,value));
            var old=new TreeMap<>(slots.tailMap(position,true));old.keySet().forEach(slots::remove);old.forEach((row,value)->slots.put(row+1,value));
            rows.put(position,id);slots.put(position,null);restoredRows.put(id,source);
            int last=rows.lastKey();if(last>1048576)throw invalid();
            if(InputWorkbookPolicy.infrastructure(original))fitRows();else extendRows(this,last);
            slots.tailMap(endRow,false).clear();
        }
        void restoreColumn(String field,int source,int position,JsonNode originalField) {
            int token=Integer.MIN_VALUE+source;
            if(fieldTokens.containsKey(field)||position<original.startColumn()||endColumn>=16384)throw InputDraftException.invalid();
            columnPositions.replaceAll((key,value)->value!=null&&value>=position?value+1:value);
            columnPositions.put(token,position);sourceColumns.put(token,source);fieldTokens.put(field,token);visibleTokens.add(token);restoredColumns.put(field,source);endColumn=Math.max(original.endColumn(),columnPositions.values().stream().filter(java.util.Objects::nonNull).mapToInt(Integer::intValue).max().orElse(original.endColumn()));
            for(JsonNode item:definition.withArray("fields"))((ObjectNode)item).put("columnNumber",columnPositions.get(fieldTokens.get(item.path("fieldKey").asText())));
            ObjectNode restored=originalField.deepCopy();restored.put("columnNumber",position);definition.withArray("fields").add(restored);
            var columns=definition.putArray("columns");visibleTokens.stream().map(columnPositions::get).filter(java.util.Objects::nonNull).sorted().forEach(columns::add);
            var identities=new java.util.TreeSet<Integer>();before.identityColumns().forEach(col->identities.add(columnPositions.get(col)));
            if(originalField.path("identity").asBoolean())identities.add(position);
            var identity=definition.putArray("identityColumns");identities.forEach(identity::add);
        }
        void deleteRow(String id) {
            int row = row(id), last = InputWorkbookPolicy.kind(original) == InputWorkbookPolicy.Kind.HYDROGEN && row <= 736 ? 736 : slots.lastKey();
            Integer source = slots.get(row); if (source != null) removed.add(source);
            rows.remove(row);
            var shifted = new TreeMap<>(rows.subMap(row, false, last, true));
            shifted.keySet().forEach(rows::remove); shifted.forEach((from, value) -> rows.put(from - 1, value));
            for (int position = row; position < last; position++) slots.put(position, slots.get(position + 1));
            slots.put(last, null);
            if (InputWorkbookPolicy.infrastructure(original)) fitRows();
        }
        void shiftBlock(int shift) {
            rowOffset = shift;
            var oldRows = new TreeMap<>(rows); rows.clear(); oldRows.forEach((row, id) -> rows.put(row + shift, id));
            var oldSlots = new TreeMap<>(slots); slots.clear(); oldSlots.forEach((row, source) -> slots.put(row + shift, source));
            var headers = definition.putArray("headerRows"); before.headerRows().forEach(row -> headers.add(row + shift));
            var ranges = definition.putArray("bodyRanges"); before.bodyRanges().forEach(range -> ranges.addObject().put("startRow", range.startRow() + shift).put("endRow", range.endRow() + shift));
            endRow += shift;
            if (original.startRow() + shift < 1 || endRow > 1048576) throw invalid();
        }
        void fitRows() {
            int last = rows.isEmpty() ? original.dataStart() : rows.lastKey();
            definition.putArray("bodyRanges").addObject().put("startRow", original.dataStart()).put("endRow", last);
            endRow = last;
        }
        void deleteColumn(String field) {
            Integer token = fieldTokens.get(field), position = columnPositions.get(token);
            if (position == null || position < 7) throw invalid();
            var later = columnPositions.entrySet().stream().filter(e -> e.getValue() != null && e.getValue() > position)
                .sorted(Map.Entry.comparingByValue()).toList();
            int previous = position;
            for (var next : later) { int current = next.getValue(); columnPositions.put(next.getKey(), previous); previous = current; }
            columnPositions.put(token, null); removedColumns.add(token);
            var fields = definition.withArray("fields");
            for (int i = fields.size() - 1; i >= 0; i--) {
                ObjectNode item = (ObjectNode) fields.get(i); Integer target = columnPositions.get(fieldTokens.get(item.path("fieldKey").asText()));
                if (target == null) fields.remove(i); else item.put("columnNumber", target);
            }
            var columns = definition.putArray("columns"); columnPositions.values().stream().filter(java.util.Objects::nonNull).sorted().forEach(columns::add);
            if (InputWorkbookPolicy.infrastructure(original)) endColumn = Math.max(17,
                columnPositions.values().stream().filter(java.util.Objects::nonNull).mapToInt(Integer::intValue).max().orElse(17));
        }
    }

    private final InputDraftJson json;
    private final InputWorkbookXml.Checkpoint checkpoint;
    private final InputWorkbookReader reader;

    InputWorkbookCommands(InputDraftJson json, InputWorkbookReader reader, InputWorkbookXml.Checkpoint checkpoint) {
        this.json = json; this.reader = reader; this.checkpoint = checkpoint;
    }

    ObjectNode transform(Path source, Path target, InputDraftHistory history, InputDraftJson.ChangeRequest request,
            String role, JsonNode expected, Path baseline) throws Exception {
        return transform(source,target,history,request,role,expected,baseline,null);
    }
    ObjectNode transform(Path source, Path target, InputDraftHistory history, InputDraftJson.ChangeRequest request,
            String role, JsonNode expected, Path baseline, InputWorkbookRestoration.Past past) throws Exception {
        Map<Long, Table> tables = new LinkedHashMap<>(); Map<String, String> rowClients = new HashMap<>(), columnClients = new HashMap<>();
        Map<String, Integer> lastColumns = new HashMap<>(); Map<Cell, Object> submittedValues = new LinkedHashMap<>();
        Integer hydrogenProtectedEnd = null;
        Integer shippingNextRow = null;
        Map<String, String> regasLinks = null;
        Table linkedMaster = null, linkedMonthly = null, linkedCountries = null;
        for (JsonNode command : request.operations()) {
            TableRef ref = history.catalog().tables().stream().filter(t -> t.key().equals(command.path("tableId").asText())).findFirst().orElseThrow(InputDraftException::missing);
            if (!ref.role().equals(role)) continue;
            Table table = tables.computeIfAbsent(ref.id(), ignored -> new Table(ref, history, json));
            String type = command.path("type").asText();
            if (!type.startsWith("RESTORE_") && !InputWorkbookPolicy.supports(ref, table.before, type)) throw new InputDraftPreserveChange.Failure("POLICY_UNSUPPORTED");
            if (command.has("controlId")) {
                JsonNode control = InputWorkbookPolicy.control(table.before, command.path("controlId").asText());
                TableRef owner = history.catalog().tables().stream().filter(t -> t.key().equals(control.path("sourceTableId").asText())).findFirst().orElseThrow(InputWorkbookCommands::invalid);
                Table targetTable = tables.computeIfAbsent(owner.id(), ignored -> new Table(owner, history, json));
                int row = control.path("binding").path("rowNumber").asInt(), column = control.path("binding").path("columnIndex").asInt() + 1;
                targetTable.columnPositions.put(column, column); targetTable.sourceColumns.put(column, history.sourceColumn(owner.id(), column));
                submittedValues.put(new Cell(owner.id(), null, row, column), scalar(command.path("value")));
                if (InputWorkbookPolicy.kind(ref) == InputWorkbookPolicy.Kind.SHIPPING_COST) {
                    var costs = history.catalog().tables().stream().filter(t -> InputWorkbookPolicy.kind(t) == InputWorkbookPolicy.Kind.SHIPPING_COST).toList();
                    if (costs.size() != 5 || !costs.stream().map(TableRef::startRow).collect(java.util.stream.Collectors.toSet()).equals(Set.of(2, 15, 26, 37, 48))) throw invalid();
                    for (TableRef cost : costs) tables.computeIfAbsent(cost.id(), ignored -> new Table(cost, history, json));
                }
                continue;
            }
            boolean masterInput = Set.of("SET", "CLEAR").contains(type) && ref.startColumn() == 1 && command.has("fieldKey")
                && InputWorkbookPolicy.field(table.current(), command.path("fieldKey").asText()).path("columnNumber").asInt() < 18;
            if (InputWorkbookPolicy.kind(ref) == InputWorkbookPolicy.Kind.REGAS && (Set.of("ADD_ROW", "DELETE_ROW", "RESTORE_ROW").contains(type) || masterInput) && regasLinks == null) {
                linkedMaster = table;
                TableRef monthly = history.catalog().tables().stream().filter(t -> InputWorkbookPolicy.kind(t) == InputWorkbookPolicy.Kind.REGAS_MONTHLY).findFirst().orElseThrow(InputWorkbookCommands::invalid);
                TableRef countries = history.catalog().tables().stream().filter(t -> t.role().equals("SUPPLY") && t.sheet().equals("RegasAdjustment") && t.startColumn() == 7).findFirst().orElseThrow(InputWorkbookCommands::invalid);
                linkedMonthly = tables.computeIfAbsent(monthly.id(), ignored -> new Table(monthly, history, json));
                linkedCountries = tables.computeIfAbsent(countries.id(), ignored -> new Table(countries, history, json));
                InputWorkbookRegas.linkedSourceRows(source, ref, monthly, table.rows.keySet(), checkpoint);
                regasLinks = new HashMap<>(history.regasLinks());
            }
            switch (type) {
                case "RESTORE_ROW" -> {
                    InputWorkbookRestoration.restoreRow(table,command.path("rowId").asText(),history);
                    if(InputDraftPmiRows.user(ref))for(TableRef childRef:history.catalog().tables().stream().filter(InputDraftPmiRows::child).toList()) {
                        Table child=tables.computeIfAbsent(childRef.id(),ignored->new Table(childRef,history,json));
                        String id=history.rowStates(childRef.id()).stream().filter(r->r.original()!=null&&r.original().equals(table.restoredRows.get(command.path("rowId").asText()))).map(InputDraftHistory.RowState::id).findFirst().orElseThrow(InputWorkbookCommands::invalid);
                        child.restoreRow(id,table.restoredRows.get(command.path("rowId").asText()),table.row(command.path("rowId").asText()));
                    }
                    if(table==linkedMaster) {
                        String id=regasLinks.get(command.path("rowId").asText());
                        InputWorkbookRestoration.restoreRow(linkedMonthly,id,history);
                    }
                }
                case "RESTORE_COLUMN" -> InputWorkbookRestoration.restoreColumn(table,command.path("fieldKey").asText(),history);
                case "ADD_ROW" -> {
                    String client = command.path("clientRowKey").asText(), id = identity(expected, "rows", "clientRowKey", client, "rowId", "");
                    int row = table.rows.isEmpty() ? table.before.bodyRanges().get(0).startRow() : table.rows.lastKey() + 1;
                    if (InputWorkbookPolicy.kind(ref) == InputWorkbookPolicy.Kind.HYDROGEN && row > 736) {
                        if (hydrogenProtectedEnd == null) hydrogenProtectedEnd = InputWorkbookRows.lastContentRow(baseline, ref.sheet(), 736, 6, 8, checkpoint);
                        row = Math.max(row, hydrogenProtectedEnd + 1);
                    }
                    if (InputWorkbookPolicy.shippingStacked(ref)) {
                        if (shippingNextRow == null) {
                            int reserved = InputWorkbookRows.lastContentRow(baseline, ref.sheet(), 90, 2, 6, checkpoint);
                            for (TableRef other : history.catalog().tables()) if (InputWorkbookPolicy.shippingStacked(other)) reserved = Math.max(reserved, other.endRow());
                            shippingNextRow = reserved + 1;
                        }
                        row = shippingNextRow++;
                    }
                    if (row > 1048576) throw invalid();
                    table.rows.put(row, id); table.addedRows.put(client, id); rowClients.put(client, id); table.slots.put(row, null);
                    extendRows(table, row);
                    for (JsonNode field : table.current().fields()) if (field.path("policyStatus").path("CREATE").asText().equals("READY")) {
                        String key = field.path("fieldKey").asText(); JsonNode value = command.path("values").has(key) ? command.path("values").get(key) : InputWorkbookPolicy.initial(field.path("create"));
                        submittedValues.put(new Cell(ref.id(), id, row, table.fieldTokens.get(key)), scalar(value));
                    }
                    if (InputDraftPmiRows.user(ref)) {
                        List<TableRef> children = history.catalog().tables().stream().filter(InputDraftPmiRows::child).toList();
                        if (children.size() != 2 || children.stream().map(TableRef::startColumn).distinct().count() != 2) throw invalid();
                        for (TableRef childRef : children) {
                            Table childTable = tables.computeIfAbsent(childRef.id(), ignored -> new Table(childRef, history, json));
                            String childClient = InputDraftPmiRows.prefix(childRef) + id;
                            String child = identity(expected, "rows", "clientRowKey", childClient, "rowId", "");
                            if (childTable.rows.containsKey(row) || (!childTable.rows.isEmpty() && childTable.rows.lastKey() != row - 1)
                                    || childTable.rows.isEmpty() && row != childTable.before.bodyRanges().get(0).startRow()) throw invalid();
                            childTable.rows.put(row, child); childTable.addedRows.put(childClient, child); childTable.slots.put(row, null);
                            extendRows(childTable, row);
                            for (int column = childRef.startColumn(); column <= childRef.endColumn(); column++)
                                if (column != 15) submittedValues.put(new Cell(childRef.id(), child, row, column), null);
                        }
                        for (int column : List.of(2, 3)) submittedValues.put(new Cell(ref.id(), id, row, column), null);
                    }
                    if (duplicateColumns(table).size() > 0) table.changedYears.put(id, command);
                    if (table == linkedMaster) {
                        String childClient = InputDraftRegasRows.PREFIX + id;
                        String child = identity(expected, "rows", "clientRowKey", childClient, "rowId", "");
                        int childRow = row + 3;
                        if (childRow > 1048576 || linkedMonthly.rows.containsKey(childRow)) throw invalid();
                        linkedMonthly.rows.put(childRow, child); linkedMonthly.addedRows.put(childClient, child); linkedMonthly.slots.put(childRow, null);
                        extendRows(linkedMonthly, childRow); regasLinks.put(id, child);
                        for (int column = 8; column <= 31; column++) submittedValues.put(new Cell(linkedMonthly.original.id(), child, childRow, column), null);
                    }
                }
                case "ADD_COLUMN" -> {
                    String client = command.path("clientColumnKey").asText();
                    String fieldKey = identity(expected, "columns", "clientColumnKey", client, "fieldKey", "p_");
                    Integer last = lastColumns.get(ref.sheet());
                    if (last == null && InputWorkbookPolicy.kind(ref) == InputWorkbookPolicy.Kind.LNG) last = ref.endColumn();
                    if (InputWorkbookPolicy.kind(ref) == InputWorkbookPolicy.Kind.REGAS) {
                        last = table.columnPositions.get(table.original.endColumn()) - 1;
                        if (last + 2 > 16384) throw invalid();
                        table.columnPositions.put(table.original.endColumn(), last + 2);
                        table.endColumn = last + 2;
                    }
                    if (InputWorkbookPolicy.kind(ref) == InputWorkbookPolicy.Kind.LIQUTIL) last = table.columnPositions.values().stream().filter(java.util.Objects::nonNull).mapToInt(Integer::intValue).max().orElse(6);
                    if (last == null) {
                        Set<Integer> reserved = new HashSet<>();
                        history.catalog().tables().stream().filter(t -> t.role().equals(role) && t.sheet().equals(ref.sheet())).forEach(t -> reserved.add(t.endColumn()));
                        last = InputWorkbookPeriods.nextFreeColumn(source, ref.sheet(), reserved, checkpoint) - 1;
                    }
                    int column = last + 1; if (column > 16384) throw invalid();
                    lastColumns.put(ref.sheet(), column); columnClients.put(client, fieldKey);
                    ObjectNode field = InputWorkbookPolicy.createdField(table.current().columnCreation(), InputWorkbookPolicy.deletableAddedColumn(ref, table.current()));
                    field.put("fieldKey", fieldKey).put("columnNumber", column);
                    ((ObjectNode) field.path("policyStatus")).put("SET_HEADER", InputWorkbookPolicy.newHeaderPolicy(ref, table.before));
                    Map<Integer, Object> headers = headers(table, source, baseline, command);
                    int token = -table.addedColumns.size() - 1;
                    table.definition.withArray("fields").add(field); table.definition.withArray("columns").add(column); table.endColumn = Math.max(table.endColumn, column);
                    table.columnPositions.put(token, column); table.fieldTokens.put(fieldKey, token);
                    table.addedColumns.add(new AddedColumn(fieldKey, client, token, headers));
                    Object initial = scalar(InputWorkbookPolicy.initial(field.path("create")));
                    for (var range : table.current().bodyRanges()) for (int row = range.startRow(); row <= range.endRow(); row++)
                        submittedValues.put(new Cell(ref.id(), table.rows.get(row), row, token), initial);
                    headers.forEach((row, value) -> submittedValues.put(new Cell(ref.id(), null, row, token), value));
                }
                case "SET", "CLEAR" -> {
                    if (command.has("headerKey")) {
                        String key = command.has("fieldKey") ? command.path("fieldKey").asText() : columnClients.get(command.path("clientColumnKey").asText());
                        submittedValues.put(new Cell(ref.id(), null, 3, table.fieldTokens.get(key)), scalar(command.path("value")));
                        break;
                    }
                    String id = command.path("rowRef").has("rowId") ? command.path("rowRef").path("rowId").asText() : rowClients.get(command.path("rowRef").path("clientRowKey").asText());
                    String fieldKey = command.has("fieldKey") ? command.path("fieldKey").asText() : columnClients.get(command.path("clientColumnKey").asText());
                    InputWorkbookPolicy.field(table.current(), fieldKey);
                    int column = table.fieldTokens.get(fieldKey);
                    submittedValues.put(new Cell(ref.id(), id, table.row(id), column), type.equals("CLEAR") ? null : scalar(command.path("value")));
                    if (ref.sheet().equals("BCPrices") && column == 2 || overwrite(table)) table.changedYears.put(id, command);
                }
                case "DELETE_COLUMN" -> {
                    String field = command.has("fieldKey") ? command.path("fieldKey").asText() : columnClients.get(command.path("clientColumnKey").asText());
                    if (InputWorkbookPolicy.infrastructure(ref) && command.has("fieldKey")
                            && history.sourceColumn(ref.id(), InputWorkbookPolicy.field(table.current(), field).path("columnNumber").asInt()) != null) throw invalid();
                    table.deleteColumn(field);
                }
                case "DELETE_ROW" -> {
                    String id = command.path("rowRef").has("rowId") ? command.path("rowRef").path("rowId").asText() : rowClients.get(command.path("rowRef").path("clientRowKey").asText());
                    if (InputWorkbookPolicy.infrastructure(ref) && !table.addedRows.containsValue(id) && !history.origin(id).equals("ADDED")) throw invalid();
                    if (InputDraftPmiRows.user(ref)) {
                        int row = table.row(id);
                        List<TableRef> children = history.catalog().tables().stream().filter(InputDraftPmiRows::child).toList();
                        if (children.size() != 2) throw invalid();
                        for (TableRef childRef : children) {
                            Table child = tables.computeIfAbsent(childRef.id(), ignored -> new Table(childRef, history, json));
                            String childId = child.rows.get(row);
                            if (childId == null) throw invalid();
                            child.deleteRow(childId);
                        }
                    }
                    table.deleteRow(id);
                    if (table == linkedMaster) linkedMonthly.deleteRow(regasLinks.get(id));
                }
                default -> throw new InputDraftPreserveChange.Failure("POLICY_UNSUPPORTED");
            }
        }
        for (Table table : tables.values()) if (defaultRowMoves(table)) {
            int last = Math.max(history.originalTable(table.original.id()).endRow(), table.rows.isEmpty() ? 0 : table.rows.lastKey());
            if (table.endRow != last) {
                table.endRow = last;
                var ranges = table.definition.withArray("bodyRanges");
                while (ranges.size() > 1 && ranges.get(ranges.size() - 1).path("startRow").asInt() > last) ranges.remove(ranges.size() - 1);
                ((ObjectNode) ranges.get(ranges.size() - 1)).put("endRow", last);
            }
        }
        if (linkedMaster != null) {
            int shift = linkedMaster.rows.size() - linkedMaster.originalRows.size();
            linkedMaster.fitRows(); linkedMonthly.fitRows(); linkedCountries.shiftBlock(shift);
        }
        Table regasMonthly = tables.values().stream().filter(table -> InputWorkbookPolicy.kind(table.original) == InputWorkbookPolicy.Kind.REGAS_MONTHLY).findFirst().orElse(null);
        Table regasCountries = null;
        if (regasMonthly != null) {
            TableRef country = history.catalog().tables().stream().filter(table -> table.role().equals("SUPPLY") && table.sheet().equals("RegasAdjustment") && table.startColumn() == 7).findFirst().orElseThrow(InputWorkbookCommands::invalid);
            regasCountries = tables.computeIfAbsent(country.id(), ignored -> new Table(country, history, json));
        }
        boolean hurricane = tables.values().stream().anyMatch(table -> InputWorkbookPolicy.kind(table.original) == InputWorkbookPolicy.Kind.HURRICANE);
        if (hurricane) {
            List<TableRef> impacts = history.catalog().tables().stream().filter(table -> InputWorkbookPolicy.kind(table) == InputWorkbookPolicy.Kind.HURRICANE).toList();
            if (impacts.size() != 2 || impacts.stream().map(TableRef::dataStart).collect(java.util.stream.Collectors.toSet()).size() != 2) throw invalid();
            for (TableRef table : impacts) tables.computeIfAbsent(table.id(), ignored -> new Table(table, history, json));
        }
        Map<Cell, Object> finalValues = new LinkedHashMap<>();
        submittedValues.forEach((cell, value) -> {
            Table table = tables.get(cell.table);
            Integer row = cell.rowId == null ? Integer.valueOf(cell.row) : table.position(cell.rowId);
            Integer column = table.columnPositions.get(cell.column);
            String rowId = cell.rowId == null ? table.rows.get(cell.row) : cell.rowId;
            if (row != null && column != null && row <= table.endRow) finalValues.put(new Cell(cell.table, rowId, row, column), value);
        });
        Map<Cell, Object> values = finalValues;
        // 같은 요청에서 앞쪽 행을 삭제해도 최종 위치로 정규화한 master 값을 사용한다.
        for (Table user : tables.values()) if (InputDraftPmiRows.user(user.original)) for (String id : user.addedRows.values()) {
            Integer row = user.position(id); if (row == null) continue;
            Object country = values.get(new Cell(user.original.id(), id, row, 4));
            for (Table child : tables.values()) if (InputDraftPmiRows.child(child.original))
                values.put(new Cell(child.original.id(), child.rows.get(row), row, child.original.startColumn() + 2), country);
        }
        for (var entry : new ArrayList<>(values.entrySet())) {
            Cell cell = entry.getKey(); Table table = tables.get(cell.table);
            if (overwrite(table) && cell.column == (InputWorkbookPolicy.kind(table.original) == InputWorkbookPolicy.Kind.SECTOR ? 9 : 17)) {
                values.put(new Cell(cell.table, cell.rowId, cell.row, cell.column - 1), null);
                values.put(new Cell(cell.table, cell.rowId, cell.row, cell.column + 1), entry.getValue());
            }
        }
        Map<String, InputWorkbookXml.SheetPatch> sheets = new LinkedHashMap<>();
        Map<String, Map<Integer, Integer>> columnStyles = new HashMap<>();
        Map<String, List<InputWorkbookMetadata.Copy>> ruleCopies = new HashMap<>();
        Map<String, InputWorkbookXml.PartPatch> parts = new LinkedHashMap<>();
        List<InputWorkbookStructure.ChainChange> chainChanges = new ArrayList<>();
        Set<Cell> readBack = new java.util.LinkedHashSet<>(values.keySet());
        Map<String, List<InputWorkbookStructure.Move>> moves = new LinkedHashMap<>();
        Map<String, List<InputWorkbookStructure.Area>> deleted = new LinkedHashMap<>();
        Map<Long, Set<Integer>> shiftedInfrastructureHelpers = new HashMap<>();
        for (Table table : tables.values()) if (table.columnsMoved() || !table.removed.isEmpty() || !table.restoredRows.isEmpty() || table.rowOffset != 0) {
            String sheet = table.original.sheet();
            Map<Integer, Integer> sourceRows = new TreeMap<>(), sourceColumns = new TreeMap<>();
            if (table.columnsMoved() || table.rowOffset != 0) table.before.headerRows().forEach(row -> sourceRows.put(row, row + table.rowOffset));
            for (var range : table.before.bodyRanges()) for (int row = range.startRow(); row <= range.endRow(); row++) sourceRows.put(row, null);
            table.slots.forEach((position, original) -> { if (original != null) sourceRows.put(original, position); });
            table.sourceColumns.keySet().stream().filter(column->column>0).forEach(column -> sourceColumns.put(column, table.columnPositions.get(column)));
            if (overwrite(table) || defaultRowMoves(table)) {
                // 삭제 열의 null과 이동 위치를 보존하고, 미정의 표시·수식 열만 보완한다.
                for (int column = table.original.startColumn(); column <= table.original.endColumn(); column++)
                    if (!sourceColumns.containsKey(column)) sourceColumns.put(column, column);
            }
            if (InputWorkbookPolicy.kind(table.original) == InputWorkbookPolicy.Kind.PRODUCTION) sourceColumns.remove(12);
            if (table.original.role().equals("SUPPLY") && table.original.sheet().equals("BCLiquefaction") && table.original.startColumn() == 2) sourceColumns.remove(13);
            if (InputWorkbookPolicy.infrastructure(table.original) && table.rowsChanged()) {
                Map<Integer, Integer> helperTargets = new HashMap<>();
                table.slots.forEach((position, sourceRow) -> { if (sourceRow != null && sourceRow > 400 && position > 400) helperTargets.put(sourceRow, position); });
                for (var range : table.before.bodyRanges()) for (int row = Math.max(401, range.startRow()); row <= range.endRow(); row++) {
                    Integer targetRow = helperTargets.get(row);
                    var helper = new InputWorkbookStructure.Area(row, row, 3, 3);
                    if (targetRow == null) deleted.computeIfAbsent(sheet, ignored -> new ArrayList<>()).add(helper);
                    else if (targetRow != row) {
                        moves.computeIfAbsent(sheet, ignored -> new ArrayList<>()).add(new InputWorkbookStructure.Move(helper, targetRow - row, 0));
                        shiftedInfrastructureHelpers.computeIfAbsent(table.original.id(), ignored -> new HashSet<>()).add(targetRow);
                    }
                }
            }
            // 행과 열을 따로 옮겨 교차 셀을 두 번 처리하지 않고 원본 사각형의 최종 위치를 한 번에 정한다.
            for (Axis rows : axes(sourceRows)) for (Axis columns : axes(sourceColumns)) {
                var area = new InputWorkbookStructure.Area(rows.first, rows.last, columns.first, columns.last);
                if (rows.shift == null || columns.shift == null) deleted.computeIfAbsent(sheet, ignored -> new ArrayList<>()).add(area);
                else if (rows.shift != 0 || columns.shift != 0) moves.computeIfAbsent(sheet, ignored -> new ArrayList<>()).add(new InputWorkbookStructure.Move(area, rows.shift, columns.shift));
            }
            for (var column : sourceColumns.entrySet()) {
                int token = column.getKey(); Integer position = column.getValue();
                if (position != null && position != token)
                    columnStyles.computeIfAbsent(sheet, ignored -> new LinkedHashMap<>()).put(position, token);
            }
            // active 열만 delta에 저장한다. 삭제된 꼬리 슬롯은 파일에서 비우되 조회 대상으로 되살리지 않는다.
            Set<Integer> active = new HashSet<>(table.current().columns());
            if (InputWorkbookPolicy.kind(table.original) == InputWorkbookPolicy.Kind.REGAS) active.add(table.endColumn);
            if (InputWorkbookPolicy.kind(table.original) == InputWorkbookPolicy.Kind.REGAS && table.original.startColumn() == 1) active.add(1);
            if (table == linkedMonthly) active.addAll(table.sourceColumns.keySet());
            if (overwrite(table)) active.addAll(sourceColumns.values());
            // 순수 행 이동은 rowId와 원본 좌표/기존 cell delta가 함께 유지된다.
            // 새로 개방한 표의 미변경 셀을 전부 다시 저장하면 대형 표에서 delta 상한을 넘는다.
            boolean retainedRowValues = !table.columnsMoved() && table.rowOffset == 0
                && defaultRowMoves(table);
            for (int column : active) {
                if (retainedRowValues) continue;
                if (table.columnsMoved() || table.rowOffset != 0) for (int row : table.current().headerRows()) readBack.add(new Cell(table.original.id(), null, row, column));
                for (var range : table.current().bodyRanges()) for (int row = range.startRow(); row <= range.endRow(); row++)
                    readBack.add(new Cell(table.original.id(), table.rows.get(row), row, column));
            }
        }
        Set<String> structuralSheets = new HashSet<>(deleted.keySet()); structuralSheets.addAll(moves.keySet());
        for (String sheet : structuralSheets) {
            var structure = InputWorkbookStructure.move(source, sheet, moves.getOrDefault(sheet, List.of()), deleted.getOrDefault(sheet, List.of()), checkpoint);
            sheets.put(sheet, structure.sheet());
            chainChanges.add(structure.chainChange());
        }
        for (Table table : tables.values()) {
            InputWorkbookXml.SheetPatch sheet = sheets.computeIfAbsent(table.original.sheet(), ignored -> new InputWorkbookXml.SheetPatch());
            if (!table.liveColumns().isEmpty()) {
                int template = table.templateColumn();
                for (AddedColumn column : table.liveColumns()) columnStyles.computeIfAbsent(table.original.sheet(), ignored -> new LinkedHashMap<>()).put(table.column(column), template);
                for (AddedColumn column : table.liveColumns()) for (var range : table.before.bodyRanges()) ruleCopies.computeIfAbsent(table.original.sheet(), ignored -> new ArrayList<>())
                    .add(new InputWorkbookMetadata.Copy(new InputWorkbookStructure.Area(range.startRow(), range.endRow(), template, template), 0, table.column(column) - template));
                List<Integer> labels = InputWorkbookPolicy.months(table.original) ? List.of(10) : List.of();
                var periods = InputWorkbookPeriods.literals(source, table.original.sheet(), template,
                    table.liveColumns().stream().map(table::column).toList(), table.liveColumns().stream().map(AddedColumn::headers).toList(),
                    table.current().bodyRanges(), labels, scalar(InputWorkbookPolicy.initial(table.current().columnCreation().path("body").path("create"))),
                    !table.columnsMoved() && InputWorkbookPolicy.kind(table.original) != InputWorkbookPolicy.Kind.LIQUTIL ? null : sheet, checkpoint);
                merge(sheet, periods.sheet());
                for (AddedColumn column : table.liveColumns()) for (int row : labels) readBack.add(new Cell(table.original.id(), null, row, table.column(column)));
            }
            if (!table.addedRows.isEmpty()) {
                Map<Integer, Map<Integer, Object>> appended = new LinkedHashMap<>();
                for (String id : table.addedRows.values()) {
                    Integer row = table.position(id); if (row == null) continue;
                    for (JsonNode field : table.current().fields()) if (field.path("policyStatus").path("CREATE").asText().equals("READY")
                            && !InputDraftPmiRows.user(table.original) && !InputDraftPmiRows.child(table.original)) {
                        int column = field.path("columnNumber").asInt(), from = table.liveColumns().stream().anyMatch(c -> table.column(c) == column) ? table.templateColumn() : table.fieldTokens.get(field.path("fieldKey").asText());
                        ruleCopies.computeIfAbsent(table.original.sheet(), ignored -> new ArrayList<>()).add(new InputWorkbookMetadata.Copy(
                            new InputWorkbookStructure.Area(table.styleRow, table.styleRow, from, from), row - table.styleRow, column - from));
                    }
                    if (InputDraftPmiRows.user(table.original) || InputDraftPmiRows.child(table.original))
                        ruleCopies.computeIfAbsent(table.original.sheet(), ignored -> new ArrayList<>()).add(new InputWorkbookMetadata.Copy(
                            new InputWorkbookStructure.Area(table.styleRow, table.styleRow, table.original.startColumn(), table.original.endColumn()), row - table.styleRow, 0));
                    Map<Integer, Object> rowValues = new HashMap<>();
                    values.forEach((cell, value) -> { if (cell.table == table.original.id() && cell.row == row) rowValues.put(cell.column, value); });
                    appended.put(row, rowValues);
                }
                Map<Integer, Integer> addedColumnTemplates = new LinkedHashMap<>();
                for (AddedColumn column : table.liveColumns()) addedColumnTemplates.put(table.column(column), table.templateColumn());
                if (!appended.isEmpty()) merge(sheet, InputWorkbookRows.append(source, table.original.sheet(), table.styleRow, appended,
                    table.originalRows.isEmpty() || !sheet.raw.isEmpty() ? sheet : null, addedColumnTemplates, checkpoint));
            }
        }
        InputWorkbookRestoration.copy(baseline,source,history,tables,sheets,chainChanges,readBack,values,request,past,reader,checkpoint,json);
        Path calculationSource = source;
        if (tables.values().stream().anyMatch(table -> !table.restoredRows.isEmpty() || defaultRowMoves(table) && !table.removed.isEmpty())) {
            Map<String, InputWorkbookXml.PartPatch> provisional = new LinkedHashMap<>(parts);
            if (!chainChanges.isEmpty()) provisional.put("xl/calcChain.xml", InputWorkbookStructure.chain(chainChanges));
            InputWorkbookXml.write(source, target, sheets, provisional, checkpoint);
            calculationSource = target;
        }
        values.forEach((cell, value) -> {
            var sheet = sheets.get(tables.get(cell.table).original.sheet());
            var old = sheet.edits.getOrDefault(cell.row, Map.of()).get(cell.column);
            sheet.put(cell.row, cell.column, new InputWorkbookXml.Edit(value, old == null ? null : old.template(), false));
        });
        for (Table selected : tables.values()) if (InputDraftPmiRows.selected(selected.original) && !selected.createdRows().isEmpty()) {
            Set<Integer> created = new HashSet<>(); selected.createdRows().forEach(id -> { Integer row = selected.position(id); if (row != null) created.add(row); });
            if (created.isEmpty()) continue;
            InputWorkbookPmi.append(calculationSource, baseline, history.originalTable(selected.original.id()).endRow(), created, sheets.get(selected.original.sheet()), reader, checkpoint,selected.restoredRows.keySet().stream().map(selected::row).collect(java.util.stream.Collectors.toSet()));
            for (int row : created) readBack.add(new Cell(selected.original.id(), selected.rows.get(row), row, 15));
            chainChanges.add(new InputWorkbookStructure.ChainChange(InputWorkbookStructure.sheetId(source, selected.original.sheet()), Map.of(), Set.of(),
                created.stream().sorted().map(row -> "O" + row).toList()));
        }
        if (linkedMaster != null) {
            Set<Integer> created = new HashSet<>();
            for (String id : linkedMaster.createdRows()) { Integer row = linkedMaster.position(id); if (row != null) created.add(row); }
            if (!created.isEmpty()) {
                List<String> formulas = InputWorkbookRegas.masterRows(source, baseline, linkedMaster.original, linkedMaster.styleRow, linkedMaster.endColumn, created,
                    sheets.get(linkedMaster.original.sheet()), reader, checkpoint);
                for (int row : created) for (int column : List.of(1, linkedMaster.endColumn)) {
                    readBack.add(new Cell(linkedMaster.original.id(), linkedMaster.rows.get(row), row, column));
                }
                chainChanges.add(new InputWorkbookStructure.ChainChange(InputWorkbookStructure.sheetId(source, linkedMaster.original.sheet()), Map.of(), Set.of(), formulas));
            }
        }
        if (hurricane) for (int row : InputWorkbookHurricanes.calculate(source, sheets.get("BCHurricanes"), reader, checkpoint)) {
            Table owner = tables.values().stream().filter(table -> InputWorkbookPolicy.kind(table.original) == InputWorkbookPolicy.Kind.HURRICANE
                && row >= table.original.dataStart() && row <= table.original.endRow()).findFirst().orElseThrow(InputWorkbookCommands::invalid);
            readBack.add(new Cell(owner.original.id(), owner.rows.get(row), row, 7));
        }
        for (Table table : tables.values()) if (Set.of(InputWorkbookPolicy.Kind.PRODUCTION, InputWorkbookPolicy.Kind.BCPRODUCTION).contains(InputWorkbookPolicy.kind(table.original))) {
            Set<Integer> created = new HashSet<>();
            for (String id : table.createdRows()) { Integer row = table.position(id); if (row != null) created.add(row); }
            var result = InputWorkbookPolicy.kind(table.original) == InputWorkbookPolicy.Kind.PRODUCTION
                ? InputWorkbookProduction.adjustment(calculationSource, baseline, table.original.endRow(), table.endRow, !table.removed.isEmpty(), created, sheets.get(table.original.sheet()), reader, checkpoint)
                : InputWorkbookProduction.baseline(calculationSource, baseline, table.styleRow, table.endRow, created, sheets.get(table.original.sheet()), reader, checkpoint);
            for (String address : result.calculated()) {
                var ref = new org.apache.poi.ss.util.CellReference(address); int row = ref.getRow() + 1;
                readBack.add(new Cell(table.original.id(), table.rows.get(row), row, ref.getCol() + 1));
            }
            chainChanges.add(new InputWorkbookStructure.ChainChange(InputWorkbookStructure.sheetId(source, table.original.sheet()), Map.of(), Set.of(), result.added()));
            if (InputWorkbookPolicy.kind(table.original) == InputWorkbookPolicy.Kind.BCPRODUCTION && !table.removed.isEmpty()) {
                var sequence = InputWorkbookProduction.reindex(baseline, table.rows.keySet(), sheets.get(table.original.sheet()), checkpoint);
                for (String address : sequence.calculated()) {
                    int row = new org.apache.poi.ss.util.CellReference(address).getRow() + 1;
                    readBack.add(new Cell(table.original.id(), table.rows.get(row), row, 1));
                }
                Integer firstSource = table.slots.get(table.rows.isEmpty() ? table.original.dataStart() : table.rows.firstKey());
                chainChanges.add(new InputWorkbookStructure.ChainChange(InputWorkbookStructure.sheetId(source, table.original.sheet()), Map.of(),
                    firstSource == null ? Set.of() : Set.of("A" + firstSource), List.of()));
            }
        }
        for (Table table : tables.values()) if (InputWorkbookPolicy.infrastructure(table.original)) {
            Set<Integer> created = new HashSet<>();
            for (String id : table.createdRows()) { Integer row = table.position(id); if (row != null) created.add(row); }
            var result = InputWorkbookCapacity.infrastructure(source, table.styleRow, created,
                shiftedInfrastructureHelpers.getOrDefault(table.original.id(), Set.of()), sheets.get(table.original.sheet()), reader, checkpoint);
            for (String address : result.calculated()) {
                var ref = new org.apache.poi.ss.util.CellReference(address); int row = ref.getRow() + 1;
                readBack.add(new Cell(table.original.id(), table.rows.get(row), row, 3));
            }
            if (!result.added().isEmpty()) chainChanges.add(new InputWorkbookStructure.ChainChange(InputWorkbookStructure.sheetId(source, table.original.sheet()), Map.of(), Set.of(), result.added()));
        }
        for (Table table : tables.values()) if (InputWorkbookPolicy.kind(table.original) == InputWorkbookPolicy.Kind.ALLOCATION) {
            Set<Integer> created = new HashSet<>();
            for (String id : table.createdRows()) { Integer row = table.position(id); if (row != null) created.add(row); }
            var result = InputWorkbookCapacity.shares(calculationSource, baseline, table.endRow, table.styleRow, created, sheets.get(table.original.sheet()), reader, checkpoint);
            for (String address : result.calculated()) {
                var ref = new org.apache.poi.ss.util.CellReference(address); int row = ref.getRow() + 1;
                readBack.add(new Cell(table.original.id(), table.rows.get(row), row, ref.getCol() + 1));
            }
            chainChanges.add(new InputWorkbookStructure.ChainChange(InputWorkbookStructure.sheetId(source, table.original.sheet()), Map.of(), Set.of(), result.added()));
        }
        for (Table table : tables.values()) if (InputWorkbookPolicy.shippingStacked(table.original)) {
            Set<Integer> created = new HashSet<>();
            for (String id : table.createdRows()) { Integer row = table.position(id); if (row != null) created.add(row); }
            var result = InputWorkbookShipping.stacked(source, table.original.dataStart(), table.styleRow,
                List.copyOf(table.rows.keySet()), created, InputWorkbookPolicy.kind(table.original) == InputWorkbookPolicy.Kind.SHIPPING_DISRUPTION,
                sheets.get(table.original.sheet()), reader, checkpoint);
            for (String address : result.calculated()) {
                var ref = new org.apache.poi.ss.util.CellReference(address); int row = ref.getRow() + 1;
                readBack.add(new Cell(table.original.id(), table.rows.get(row), row, ref.getCol() + 1));
            }
            if (!result.added().isEmpty()) chainChanges.add(new InputWorkbookStructure.ChainChange(InputWorkbookStructure.sheetId(source, table.original.sheet()), Map.of(), Set.of(), result.added()));
        }
        for (Table table : tables.values()) if (InputWorkbookPolicy.kind(table.original) == InputWorkbookPolicy.Kind.SHIPPING_ROUTE) {
            Set<Integer> created = new HashSet<>();
            for (String id : table.createdRows()) { Integer row = table.position(id); if (row != null) created.add(row); }
            var result = InputWorkbookShipping.routes(source, table.endRow, table.styleRow, created, sheets.get(table.original.sheet()), reader, checkpoint);
            for (String address : result.calculated()) {
                var ref = new org.apache.poi.ss.util.CellReference(address); int row = ref.getRow() + 1;
                readBack.add(new Cell(table.original.id(), table.rows.get(row), row, ref.getCol() + 1));
            }
            chainChanges.add(new InputWorkbookStructure.ChainChange(InputWorkbookStructure.sheetId(source, table.original.sheet()), Map.of(), Set.of(), result.added()));
        }
        for (Table table : tables.values()) if (InputWorkbookPolicy.kind(table.original) == InputWorkbookPolicy.Kind.MACRO) {
            Set<Integer> created = new HashSet<>();
            for (String id : table.createdRows()) { Integer row = table.position(id); if (row != null) created.add(row); }
            var result = InputWorkbookDemandLogic.macro(calculationSource, baseline, table.endRow, table.styleRow, created, sheets.get(table.original.sheet()), reader, checkpoint);
            for (String address : result.calculated()) {
                var ref = new org.apache.poi.ss.util.CellReference(address); int row = ref.getRow() + 1;
                readBack.add(new Cell(table.original.id(), table.rows.get(row), row, ref.getCol() + 1));
            }
            chainChanges.add(new InputWorkbookStructure.ChainChange(InputWorkbookStructure.sheetId(source, table.original.sheet()), Map.of(), Set.of(), result.added()));
        }
        for (Table table : tables.values()) if (InputWorkbookPolicy.kind(table.original) == InputWorkbookPolicy.Kind.WEATHER_LOGIC) {
            Set<Integer> created = new HashSet<>();
            for (String id : table.createdRows()) { Integer row = table.position(id); if (row != null) created.add(row); }
            var result = InputWorkbookWeather.geography(calculationSource, baseline, table.original.startColumn(), table.endRow, table.styleRow, created, sheets.get(table.original.sheet()), reader, checkpoint);
            for (String address : result.calculated()) {
                var ref = new org.apache.poi.ss.util.CellReference(address); int row = ref.getRow() + 1;
                readBack.add(new Cell(table.original.id(), table.rows.get(row), row, ref.getCol() + 1));
            }
            chainChanges.add(new InputWorkbookStructure.ChainChange(InputWorkbookStructure.sheetId(source, table.original.sheet()), Map.of(), Set.of(), result.added()));
        }
        for (Table table : tables.values()) if (InputWorkbookPolicy.kind(table.original) == InputWorkbookPolicy.Kind.COST) {
            Set<Integer> created = new HashSet<>();
            for (String id : table.createdRows()) { Integer row = table.position(id); if (row != null) created.add(row); }
            Set<Object> removedYears = new HashSet<>();
            if (table.original.startColumn() == 37 && !table.removed.isEmpty()) {
                Map<Integer, Object> previousYears = new HashMap<>(); table.removed.forEach(row -> previousYears.put(row, null));
                reader.readLocal(source, List.of(new InputWorkbookReader.Window(table.original.sheet(), table.removed::contains, Set.of(37),
                    (row, cells) -> { if (cells.containsKey(37)) previousYears.put(row, cells.get(37).raw()); })));
                removedYears.addAll(previousYears.values());
                removedYears.add(null); // 삭제 후 빈 슬롯은 COUNTIFS의 연도0 그룹에도 영향을 준다.
            }
            var result = table.original.startColumn() == 37
                ? InputWorkbookCost.ranking(calculationSource, baseline, table.original.endRow(), table.endRow, table.styleRow, table.rows.keySet(), created, removedYears, sheets.get(table.original.sheet()), reader, checkpoint)
                : InputWorkbookCost.inputs(calculationSource, baseline, table.original.startColumn(), table.endColumn - 2, table.endRow, table.styleRow,
                    created, sheets.get(table.original.sheet()), reader, checkpoint);
            for (String address : result.calculated()) {
                var ref = new org.apache.poi.ss.util.CellReference(address); int row = ref.getRow() + 1;
                readBack.add(new Cell(table.original.id(), table.rows.get(row), row, ref.getCol() + 1));
            }
            chainChanges.add(new InputWorkbookStructure.ChainChange(InputWorkbookStructure.sheetId(source, table.original.sheet()), Map.of(), Set.of(), result.added()));
        }
        if (role.equals("TRADE") && sheets.containsKey("DemandAdjLogicInput"))
            InputWorkbookTradeAdjustments.demandInputs(source, sheets.get("DemandAdjLogicInput"), reader, checkpoint);
        for (Table table : tables.values()) if (InputWorkbookPolicy.kind(table.original) == InputWorkbookPolicy.Kind.UPSTREAM) {
            var calculated = table.original.startColumn() == 9
                ? InputWorkbookTradeAdjustments.supply(source, sheets.get(table.original.sheet()), reader, checkpoint)
                : InputWorkbookTradeAdjustments.countries(calculationSource, table.original.endRow(), table.endRow, !table.removed.isEmpty(), sheets.get(table.original.sheet()), reader, checkpoint);
            for (String address : calculated) {
                var ref = new org.apache.poi.ss.util.CellReference(address); int row = ref.getRow() + 1;
                readBack.add(new Cell(table.original.id(), table.rows.get(row), row, ref.getCol() + 1));
            }
        }
        for (Table table : tables.values()) if (InputWorkbookPolicy.kind(table.original) == InputWorkbookPolicy.Kind.SHIPPING_TECHNICAL)
            for (String address : InputWorkbookShipping.technical(source, sheets.get(table.original.sheet()), reader, checkpoint))
                readBack.add(new Cell(table.original.id(), table.rows.get(83), 83, 4));
        Map<Integer, Set<Integer>> baseCaseRows = new HashMap<>();
        for (Table table : tables.values()) if (InputWorkbookPolicy.kind(table.original) == InputWorkbookPolicy.Kind.HURRICANE_ADJUSTMENT) {
            Set<Integer> created = new HashSet<>();
            for (String id : table.createdRows()) { Integer row = table.position(id); if (row != null) created.add(row); }
            int previousActiveEnd = table.originalRows.values().stream().mapToInt(Integer::intValue).max().orElse(table.original.dataStart() - 1);
            String previousCount = InputWorkbookStructure.formulas(source, table.original.sheet(), "I3"::equals, checkpoint).get("I3");
            int previousGeometryEnd = table.original.endRow();
            String geometryCount = (previousGeometryEnd - 7) + "-COUNTIFS($E$8:$E$" + previousGeometryEnd + ",\" \")";
            if (geometryCount.equals(previousCount)) previousActiveEnd = previousGeometryEnd;
            boolean compactCount = !table.removed.isEmpty() || previousActiveEnd != previousGeometryEnd;
            int activeEnd = compactCount ? table.rows.isEmpty() ? table.original.dataStart() - 1 : table.rows.lastKey() : table.endRow;
            var result = InputWorkbookHurricanes.adjustment(calculationSource, baseline, previousActiveEnd, activeEnd, !table.removed.isEmpty(), created, sheets.get(table.original.sheet()), reader, checkpoint);
            for (String address : result.calculated()) {
                var ref = new org.apache.poi.ss.util.CellReference(address); int row = ref.getRow() + 1;
                readBack.add(new Cell(table.original.id(), table.rows.get(row), row, ref.getCol() + 1));
            }
            chainChanges.add(new InputWorkbookStructure.ChainChange(InputWorkbookStructure.sheetId(source, table.original.sheet()), Map.of(), Set.of(), result.added()));
        }
        for (Table table : tables.values()) if (InputWorkbookPolicy.kind(table.original) == InputWorkbookPolicy.Kind.BCINPUT)
            baseCaseRows.put(table.original.startColumn(), Set.copyOf(table.originalRows.values()));
        if (!baseCaseRows.isEmpty()) {
            Set<String> calculated = InputWorkbookBcInput.helpers(calculationSource, baseCaseRows, sheets.get("BCInput"), reader, checkpoint);
            for (String address : calculated) {
                var ref = new org.apache.poi.ss.util.CellReference(address); int row = ref.getRow() + 1, column = ref.getCol() + 1;
                Table owner = tables.values().stream().filter(t -> InputWorkbookPolicy.kind(t.original) == InputWorkbookPolicy.Kind.BCINPUT
                    && column >= t.original.startColumn() && column <= t.endColumn).findFirst().orElseThrow(InputWorkbookCommands::invalid);
                readBack.add(new Cell(owner.original.id(), owner.rows.get(row), row, column));
            }
        }
        for (Table table : tables.values()) if (InputWorkbookPolicy.months(table.original) && !table.removedColumns.isEmpty()) {
            Map<Integer, Integer> positions = new LinkedHashMap<>(); table.before.columns().forEach(column -> positions.put(column, table.columnPositions.get(column)));
            InputWorkbookPeriods.monthlyYears(source, table.original.sheet(), positions, sheets.get(table.original.sheet()), reader, checkpoint);
        }
        for (Table table : tables.values()) if (InputWorkbookPolicy.kind(table.original) == InputWorkbookPolicy.Kind.LIQUTIL && !table.liveColumns().isEmpty()) {
            Map<Integer, Map<Integer, Object>> headers = new LinkedHashMap<>();
            table.periodHeaders.forEach((token, valuesForColumn) -> { Integer position = table.columnPositions.get(token); if (position != null) headers.put(position, valuesForColumn); });
            for (AddedColumn column : table.liveColumns()) headers.put(table.column(column), column.headers);
            Set<Integer> added = table.liveColumns().stream().map(table::column).collect(java.util.stream.Collectors.toSet());
            List<String> formulas = InputWorkbookPeriods.utilizationYears(headers, added, sheets.get(table.original.sheet()), checkpoint);
            for (String address : formulas) {
                int column = new org.apache.poi.ss.util.CellReference(address).getCol() + 1;
                Cell cell = new Cell(table.original.id(), null, 7, column); values.remove(cell); readBack.add(cell);
            }
            if (!formulas.isEmpty()) chainChanges.add(new InputWorkbookStructure.ChainChange(InputWorkbookStructure.sheetId(source, table.original.sheet()), Map.of(), Set.of(), formulas));
        }
        for (Table table : tables.values()) if (InputWorkbookPolicy.kind(table.original) == InputWorkbookPolicy.Kind.LNG) {
            var patch = sheets.get(table.original.sheet()); Set<Integer> existing = new HashSet<>();
            for (Cell cell : values.keySet()) if (cell.table == table.original.id() && cell.row >= 4 && cell.column <= table.original.endColumn()) {
                if (cell.column >= 5 && cell.column <= 14) for (int c = 5; c <= 14; c++) existing.add(c);
                else if (cell.column > 14) existing.add(cell.column);
            }
            Set<String> calculated = InputWorkbookPeriods.lngExisting(source, existing, patch, reader, checkpoint);
            for (String address : calculated) {
                var ref = new org.apache.poi.ss.util.CellReference(address); int row = ref.getRow() + 1, column = ref.getCol() + 1;
                readBack.add(new Cell(table.original.id(), table.rows.get(row), row, column));
            }
            List<String> created = new ArrayList<>();
            for (AddedColumn column : table.liveColumns()) {
                InputWorkbookPeriods.lngTotals(source, table.original.sheet(), table.original.endColumn(), table.column(column), patch, checkpoint);
                for (int row : InputWorkbookPeriods.LNG_TOTALS) {
                    Cell cell = new Cell(table.original.id(), table.rows.get(row), row, table.column(column));
                    values.remove(cell); readBack.add(cell); created.add(InputWorkbookXml.address(row, table.column(column)));
                }
            }
            if (!created.isEmpty()) chainChanges.add(new InputWorkbookStructure.ChainChange(InputWorkbookStructure.sheetId(source, table.original.sheet()), Map.of(), Set.of(), created));
        }
        for (Table table : tables.values()) if (InputWorkbookPolicy.kind(table.original) == InputWorkbookPolicy.Kind.REGAS) {
            Set<Integer> calculatedRows = new HashSet<>();
            if (table.columnsChanged()) calculatedRows.addAll(table.rows.keySet());
            else values.keySet().stream().filter(cell -> cell.table == table.original.id() && cell.row >= 8 && cell.column >= 7).forEach(cell -> calculatedRows.add(cell.row));
            InputWorkbookPeriods.regasMaximum(source, table.original.endColumn(), table.endColumn, calculatedRows, table == linkedMaster && table.rowsChanged() ? table.slots : null,
                sheets.get(table.original.sheet()), reader, checkpoint);
            for (int row : calculatedRows) readBack.add(new Cell(table.original.id(), table.rows.get(row), row, table.endColumn));
        }
        if (regasMonthly != null && (linkedMaster == null || !linkedMaster.rowsChanged())) {
            Set<String> calculated = InputWorkbookRegas.direct(source, regasMonthly.original, regasCountries.original,
                sheets.get(regasMonthly.original.sheet()), reader, checkpoint);
            for (String address : calculated) {
                var ref = new org.apache.poi.ss.util.CellReference(address); int row = ref.getRow() + 1, column = ref.getCol() + 1;
                Table owner = row >= regasCountries.original.dataStart() ? regasCountries : regasMonthly;
                readBack.add(new Cell(owner.original.id(), owner.rows.get(row), row, column));
            }
        }
        Map<Integer, Set<Integer>> changedMasterInputs = new LinkedHashMap<>();
        if (linkedMaster != null) for (Cell cell : values.keySet())
            if (cell.table == linkedMaster.original.id() && cell.column >= 2 && cell.column <= 17 && (linkedMaster.originalRows.containsKey(cell.rowId)||linkedMaster.restoredRows.containsKey(cell.rowId)))
                changedMasterInputs.computeIfAbsent(cell.row, ignored -> new HashSet<>()).add(cell.column);
        Set<Integer> changedCapacities = new HashSet<>();
        changedMasterInputs.forEach((row, columns) -> { if (columns.stream().anyMatch(c -> c >= 7)) changedCapacities.add(row); });
        Map<InputWorkbookCalculation.Address, Object> masterLinks = Map.of();
        if (!changedMasterInputs.isEmpty()) {
            // 이동한 최종 master 입력을 먼저 연결한다. 삭제 lookup이 이전 설비명을 읽지 않게 한다.
            Map<String, InputWorkbookXml.PartPatch> provisional = new LinkedHashMap<>(parts);
            if (!chainChanges.isEmpty()) provisional.put("xl/calcChain.xml", InputWorkbookStructure.chain(chainChanges));
            InputWorkbookXml.write(source, target, sheets, provisional, checkpoint);
            masterLinks = InputWorkbookRegas.masterInputLinks(target, changedMasterInputs, sheets, reader, checkpoint);
            for (var address : masterLinks.keySet()) {
                var ref = new org.apache.poi.ss.util.CellReference(address.cell()); int row = ref.getRow() + 1;
                Table owner = address.sheet().equals(linkedMaster.original.sheet()) ? linkedMaster : linkedMonthly;
                readBack.add(new Cell(owner.original.id(), owner.rows.get(row), row, ref.getCol() + 1));
            }
        }
        if (linkedMaster != null && linkedMaster.rowsChanged()) {
            Map<InputWorkbookCalculation.Address, Object> generated = new LinkedHashMap<>(masterLinks);
            Set<Integer> created = new HashSet<>();
            for (String id : linkedMonthly.createdRows()) { Integer row = linkedMonthly.position(id); if (row != null) created.add(row); }
            Map<String, InputWorkbookXml.PartPatch> provisional = new LinkedHashMap<>(parts);
            if (!chainChanges.isEmpty()) provisional.put("xl/calcChain.xml", InputWorkbookStructure.chain(chainChanges));
            InputWorkbookXml.write(source, target, sheets, provisional, checkpoint);
            if (!created.isEmpty()) {
                var added = InputWorkbookRegas.monthlyRows(source, baseline, target, linkedCountries.original, linkedMaster.endColumn, linkedMonthly.styleRow, created,
                    sheets.get(linkedMonthly.original.sheet()), reader, checkpoint,linkedMonthly.restoredRows.keySet().stream().map(linkedMonthly::row).collect(java.util.stream.Collectors.toSet()));
                generated.putAll(added);
                List<String> formulas = new ArrayList<>();
                for (var address : added.keySet()) {
                    var ref = new org.apache.poi.ss.util.CellReference(address.cell()); int row = ref.getRow() + 1, column = ref.getCol() + 1;
                    formulas.add(address.cell()); readBack.add(new Cell(linkedMonthly.original.id(), linkedMonthly.rows.get(row), row, column));
                }
                chainChanges.add(new InputWorkbookStructure.ChainChange(InputWorkbookStructure.sheetId(source, linkedMonthly.original.sheet()), Map.of(), Set.of(), formulas));
            }
            if (!linkedMaster.removed.isEmpty() || !changedCapacities.isEmpty() || changedMasterInputs.values().stream().anyMatch(c -> c.contains(5))) {
                Set<Integer> changed = InputWorkbookRegas.changedLookupRows(source, target, linkedMonthly.rows.keySet(), linkedMonthly.slots, linkedMaster.slots, reader, checkpoint, changedCapacities, true);
                var recalculated = InputWorkbookRegas.changedLookups(target, linkedMaster.endColumn, changed, sheets.get(linkedMonthly.original.sheet()), reader, checkpoint);
                generated.putAll(recalculated);
                for (var address : recalculated.keySet()) {
                    var ref = new org.apache.poi.ss.util.CellReference(address.cell()); int row = ref.getRow() + 1, column = ref.getCol() + 1;
                    readBack.add(new Cell(linkedMonthly.original.id(), linkedMonthly.rows.get(row), row, column));
                }
            }
            Set<String> calculated = InputWorkbookRegas.countriesAfterRows(source, linkedMonthly.original, linkedCountries.original,
                linkedMonthly.rows.keySet(), linkedMonthly.slots, linkedCountries.rowOffset, generated, sheets.get(linkedMonthly.original.sheet()), reader, checkpoint);
            for (String address : calculated) {
                var ref = new org.apache.poi.ss.util.CellReference(address); int row = ref.getRow() + 1, column = ref.getCol() + 1;
                readBack.add(new Cell(linkedCountries.original.id(), linkedCountries.rows.get(row), row, column));
            }
        }
        if (!chainChanges.isEmpty()) parts.put("xl/calcChain.xml", linkedMaster != null && linkedMaster.rowsChanged()
            ? InputWorkbookStructure.chain(chainChanges, InputWorkbookStructure.sheetId(source, linkedMonthly.original.sheet()),
                new InputWorkbookStructure.Area(linkedCountries.original.dataStart() + linkedCountries.rowOffset, linkedCountries.endRow, 8, 115))
            : InputWorkbookStructure.chain(chainChanges));
        for (var sheet : columnStyles.entrySet()) sheets.get(sheet.getKey()).metadata = InputWorkbookMetadata.columns(source, sheet.getKey(), sheet.getValue(), checkpoint);
        for (var sheet : ruleCopies.entrySet()) {
            var previous = sheets.get(sheet.getKey()).metadata; var rules = InputWorkbookMetadata.rules(sheet.getValue());
            sheets.get(sheet.getKey()).metadata = token -> rules.apply(previous.apply(token));
        }
        Map<String, Map.Entry<String, String>> appendedFilters = new LinkedHashMap<>();
        for (Table table : tables.values()) if (table.endRow > table.original.endRow() && (
                table.original.role().equals("SUPPLY") && Set.of("BCLiquefaction", "BCProduction").contains(table.original.sheet())
                || InputWorkbookPolicy.kind(table.original) == InputWorkbookPolicy.Kind.MACRO
                || InputWorkbookPolicy.kind(table.original) == InputWorkbookPolicy.Kind.COST && table.original.startColumn() == 37)) {
            boolean cost = table.original.sheet().equals("CostInput");
            boolean macro = InputWorkbookPolicy.kind(table.original) == InputWorkbookPolicy.Kind.MACRO;
            String first = macro ? "I" : cost ? "AK" : table.original.sheet().equals("BCProduction") ? "A" : "B", last = macro ? "P" : cost ? "AS" : table.original.sheet().equals("BCProduction") ? "L" : "N";
            int header = macro ? 8 : cost ? 2 : 7;
            var patch = sheets.get(table.original.sheet()); var previous = patch.metadata;
            var filter = InputWorkbookMetadata.appendFilter(first + header + ":" + last + table.original.endRow(), first + header + ":" + last + table.endRow);
            patch.metadata = token -> filter.apply(previous.apply(token));
            appendedFilters.put(table.original.sheet(), Map.entry("$" + first + "$" + header + ":$" + last + "$" + table.original.endRow(), "$" + first + "$" + header + ":$" + last + "$" + table.endRow));
        }
        if (linkedMaster != null || !appendedFilters.isEmpty()) parts.put("xl/workbook.xml", InputWorkbookMetadata.filterNames(
            linkedMaster == null ? null : linkedMonthly.original.endRow(), linkedMaster == null ? null : linkedMonthly.endRow,
            linkedMaster == null ? null : linkedMaster.endRow, appendedFilters));
        if (linkedMaster != null) {
            var patch = sheets.get(linkedMaster.original.sheet()); var previous = patch.metadata;
            var filter = InputWorkbookMetadata.regasMasterFilter(linkedMaster.endRow);
            patch.metadata = token -> filter.apply(previous.apply(token));
        }
        if (linkedMaster != null && linkedMaster.rowsChanged() && sheets.get(linkedMonthly.original.sheet()).edits.values().stream().anyMatch(cells -> cells.keySet().stream().anyMatch(column -> column >= 8 && column <= 31))) {
            // 같은 요청의 직접 월 편집은 이동을 마친 좌표/수식으로 읽는다. 이 중간 파일도 호출자의 scratch다.
            InputWorkbookXml.write(source, target, sheets, parts, checkpoint);
            Set<String> calculated = InputWorkbookRegas.direct(target, currentRef(linkedMonthly), currentRef(linkedCountries),
                sheets.get(linkedMonthly.original.sheet()), reader, checkpoint);
            for (String address : calculated) {
                var ref = new org.apache.poi.ss.util.CellReference(address); int row = ref.getRow() + 1, column = ref.getCol() + 1;
                Table owner = row >= linkedCountries.original.dataStart() + linkedCountries.rowOffset ? linkedCountries : linkedMonthly;
                readBack.add(new Cell(owner.original.id(), owner.rows.get(row), row, column));
            }
        }
        if (linkedMaster != null && !linkedMaster.rowsChanged()) {
            if (!changedMasterInputs.isEmpty()) {
                InputWorkbookXml.write(source, target, sheets, parts, checkpoint);
                Set<Integer> affected = changedCapacities.isEmpty() && changedMasterInputs.values().stream().noneMatch(c -> c.contains(5)) ? Set.of()
                    : InputWorkbookRegas.changedLookupRows(source, target, linkedMonthly.rows.keySet(), linkedMonthly.slots, linkedMaster.slots, reader, checkpoint, changedCapacities, true);
                var generated = InputWorkbookRegas.changedLookups(target, linkedMaster.endColumn, affected, sheets.get(linkedMonthly.original.sheet()), reader, checkpoint);
                for (var address : generated.keySet()) {
                    var ref = new org.apache.poi.ss.util.CellReference(address.cell()); int row = ref.getRow() + 1;
                    readBack.add(new Cell(linkedMonthly.original.id(), linkedMonthly.rows.get(row), row, ref.getCol() + 1));
                }
                if (!affected.isEmpty() || changedMasterInputs.values().stream().anyMatch(c -> c.contains(4))) {
                    Map<Integer, Integer> currentRows = new LinkedHashMap<>(); linkedMonthly.rows.keySet().forEach(row -> currentRows.put(row, row));
                    var calculated = InputWorkbookRegas.countriesAfterRows(target, currentRef(linkedMonthly), currentRef(linkedCountries),
                        linkedMonthly.rows.keySet(), currentRows, 0, generated, sheets.get(linkedMonthly.original.sheet()), reader, checkpoint);
                    for (String address : calculated) {
                        var ref = new org.apache.poi.ss.util.CellReference(address); int row = ref.getRow() + 1;
                        readBack.add(new Cell(linkedCountries.original.id(), linkedCountries.rows.get(row), row, ref.getCol() + 1));
                    }
                }
            }
        }
        InputWorkbookXml.write(source, target, sheets, parts, checkpoint);
        if(InputWorkbookRestoration.recalculate(target,tables,sheets,reader,checkpoint))InputWorkbookXml.write(source,target,sheets,parts,checkpoint);
        validateYears(target, tables, request);
        ObjectNode delta = (ObjectNode) json.tree(Map.of("tables", List.of(), "rows", List.of(), "columns", List.of(), "cells", List.of()));
        for (Table table : tables.values()) {
            if (table.endRow != table.original.endRow() || table.columnsChanged() || table.rowOffset != 0) delta.withArray("tables").add(tableChange(table,history));
            table.addedRows.forEach((client, id) -> {
                ObjectNode added = delta.withArray("rows").addObject().put("tableId", table.original.key()).put("rowId", id).put("clientRowKey", client);
                added.set("rowNumber", json.tree(table.position(id)));
                if (client.startsWith(InputDraftPmiRows.BASE) || client.startsWith(InputDraftPmiRows.SELECTED))
                    added.put("derivedFromRowId", client.substring(client.startsWith(InputDraftPmiRows.BASE) ? InputDraftPmiRows.BASE.length() : InputDraftPmiRows.SELECTED.length()));
                if (client.startsWith(InputDraftRegasRows.PREFIX)) added.put("derivedFromRowId", client.substring(InputDraftRegasRows.PREFIX.length()));
            });
            table.restoredRows.forEach((id, originalRow)->delta.withArray("rows").addObject().put("tableId",table.original.key()).put("rowId",id).putNull("clientRowKey").put("rowNumber",table.row(id)));
            Map<String, Integer> finalPositions = new HashMap<>();
            table.rows.forEach((position, id) -> finalPositions.put(id, position));
            table.originalRows.forEach((id, originalRow) -> {
                Integer row = finalPositions.get(id);
                if (!java.util.Objects.equals(row, originalRow)) delta.withArray("rows").addObject().put("tableId", table.original.key()).put("rowId", id).putNull("clientRowKey").set("rowNumber", json.tree(row));
            });
            for (AddedColumn column : table.addedColumns) delta.withArray("columns").addObject().put("tableId", table.original.key()).put("fieldKey", column.field)
                .putNull("sourceColumnNumber").put("clientColumnKey", column.client).set("columnNumber", json.tree(table.columnPositions.get(column.token)));
            for (int token : table.sourceColumns.keySet()) if (!java.util.Objects.equals(table.columnPositions.get(token), token)) {
                String field = table.fieldTokens.entrySet().stream().filter(e -> e.getValue() == token).map(Map.Entry::getKey).findFirst().orElse(null);
                ObjectNode change = delta.withArray("columns").addObject().put("tableId", table.original.key()).putNull("clientColumnKey");
                change.set("fieldKey", json.tree(field)); change.set("sourceColumnNumber", json.tree(table.sourceColumns.get(token))); change.set("columnNumber", json.tree(table.columnPositions.get(token)));
            }
        }
        InputDraftDiagnostics.timedChecked(InputDraftDiagnostics.Stage.WORKBOOK_READ_BACK, role, () -> {
            readCells(target, tables, readBack, values, delta); return null;
        });
        return delta;
    }

    private static boolean defaultRowMoves(Table table) {
        return !InputWorkbookPolicy.infrastructure(table.original)
            && !Set.of(InputWorkbookPolicy.Kind.SECTOR, InputWorkbookPolicy.Kind.MONTHLY,
                InputWorkbookPolicy.Kind.LIQUTIL, InputWorkbookPolicy.Kind.REGAS).contains(InputWorkbookPolicy.kind(table.original))
            && ("READY".equals(table.before.capabilities().path("ADD_ROW").asText())
                && "READY".equals(table.before.capabilities().path("DELETE_ROW").asText())
                && InputWorkbookPolicy.supports(table.original, table.before, "ADD_ROW") || InputDraftPmiRows.child(table.original));
    }

    private void readCells(Path file, Map<Long, Table> tables, Set<Cell> readBack, Map<Cell, Object> values, ObjectNode delta) throws Exception {
        Map<String, Map<Integer, Map<Integer, InputWorkbookReader.Cell>>> actual = new HashMap<>();
        Map<String, Set<Integer>> rows = new HashMap<>(), columns = new HashMap<>();
        readBack.forEach(cell -> {
            String sheet = tables.get(cell.table).original.sheet();
            rows.computeIfAbsent(sheet, ignored -> new HashSet<>()).add(cell.row); columns.computeIfAbsent(sheet, ignored -> new HashSet<>()).add(cell.column);
        });
        List<InputWorkbookReader.Window> windows = new ArrayList<>();
        for (String sheet : rows.keySet()) windows.add(new InputWorkbookReader.Window(sheet, rows.get(sheet)::contains, columns.get(sheet),
            (row, cells) -> actual.computeIfAbsent(sheet, ignored -> new HashMap<>()).put(row, cells)));
        reader.readLocal(file, windows);
        for (Cell wanted : readBack) {
            TableRef table = tables.get(wanted.table).original;
            InputWorkbookReader.Cell cell = actual.getOrDefault(table.sheet(), Map.of()).getOrDefault(wanted.row, Map.of()).get(wanted.column);
            Object value = cell == null ? null : cell.raw();
            if (values.containsKey(wanted) && (cell != null && cell.formula() || !json.sameValue(json.tree(values.get(wanted)), json.tree(value)))) throw new InputDraftPreserveChange.Failure("PRESERVATION_FAILED");
            ObjectNode saved = delta.withArray("cells").addObject().put("tableId", table.key()).put("columnNumber", wanted.column).put("formula", cell != null && cell.formula());
            if (wanted.rowId == null) saved.putNull("rowId").put("rowNumber", wanted.row);
            else saved.put("rowId", wanted.rowId).putNull("rowNumber");
            saved.set("raw", json.tree(value)); saved.set("display", json.tree(cell == null ? null : cell.display()));
        }
    }

    private Map<Integer, Object> headers(Table table, Path source, Path baseline, JsonNode command) throws Exception {
        if (InputWorkbookPolicy.kind(table.original) == InputWorkbookPolicy.Kind.LIQPROD)
            return InputWorkbookPeriods.productionHeaders(command.path("headerValues").path("year").decimalValue(), command.path("headerValues").path("month").decimalValue());
        if (InputWorkbookPolicy.kind(table.original) != InputWorkbookPolicy.Kind.LIQUTIL)
            return Map.of(InputWorkbookPolicy.annualHeader(table.original), scalar(command.path("headerValues").get("year")));
        if (table.periodHeaders == null) table.periodHeaders = InputWorkbookPeriods.monthHeaders(source, table.original.sheet(),
            table.before.columns().stream().filter(c -> c >= 7).collect(java.util.stream.Collectors.toSet()), reader, checkpoint);
        var previous = table.columnPositions.entrySet().stream().filter(e -> e.getValue() != null && e.getValue() >= 7).max(Map.Entry.comparingByValue()).orElse(null);
        if (previous == null) return InputWorkbookPeriods.monthHeaders(baseline, table.original.sheet(), Set.of(7), reader, checkpoint).get(7);
        Map<Integer, Object> values = previous.getKey() < 0 ? table.addedColumns.stream().filter(c -> c.token == previous.getKey()).findFirst().orElseThrow().headers : table.periodHeaders.get(previous.getKey());
        if (values == null) throw invalid();
        return InputWorkbookPeriods.nextUtilizationHeaders((java.math.BigDecimal) values.get(7), (java.math.BigDecimal) values.get(8), (java.math.BigDecimal) values.get(9));
    }

    private void validateYears(Path file, Map<Long, Table> tables, InputDraftJson.ChangeRequest request) throws Exception {
        for (Table table : tables.values()) if (!table.changedYears.isEmpty()) {
            Map<List<Object>, List<String>> keys = new HashMap<>(); List<Integer> columns = duplicateColumns(table);
            reader.readLocal(file, List.of(new InputWorkbookReader.Window(table.original.sheet(), table.rows::containsKey, Set.copyOf(columns), (row, cells) -> {
                List<Object> key = new ArrayList<>();
                for (int column : columns) { var cell = cells.get(column); Object value = cell == null ? null : cell.raw(); key.add(value instanceof java.math.BigDecimal n ? n.stripTrailingZeros() : value); }
                if (key.stream().anyMatch(java.util.Objects::nonNull)) keys.computeIfAbsent(key, ignored -> new ArrayList<>()).add(table.rows.get(row));
            })));
            String reason = switch (InputWorkbookPolicy.kind(table.original)) { case SECTOR -> "DUPLICATE_SECTOR_KEY"; case MONTHLY -> "DUPLICATE_MONTHLY_KEY"; default -> "DUPLICATE_YEAR"; };
            for (var matches : keys.values()) if (matches.size() > 1) for (String id : matches) if (table.changedYears.containsKey(id))
                throw InputWorkbookPolicy.invalid(table.changedYears.get(id), request, request.baseRevision(), reason);
        }
    }

    private static boolean overwrite(Table table) { return Set.of(InputWorkbookPolicy.Kind.SECTOR, InputWorkbookPolicy.Kind.MONTHLY).contains(InputWorkbookPolicy.kind(table.original)); }
    private static List<Integer> duplicateColumns(Table table) {
        if (table.original.sheet().equals("BCPrices")) return List.of(2);
        return switch (InputWorkbookPolicy.kind(table.original)) { case SECTOR -> List.of(4, 5, 6, 7); case MONTHLY -> List.of(13, 14, 15); default -> List.of(); };
    }
    private static List<Axis> axes(Map<Integer, Integer> positions) {
        List<Axis> result = new ArrayList<>(); int first = -1, last = -1; Integer previous = null;
        for (var entry : positions.entrySet()) {
            int from = entry.getKey(); Integer shift = entry.getValue() == null ? null : entry.getValue() - from;
            if (first >= 0 && (from != last + 1 || !java.util.Objects.equals(previous, shift))) { result.add(new Axis(first, last, previous)); first = -1; }
            if (first < 0) first = from; last = from; previous = shift;
        }
        if (first >= 0) result.add(new Axis(first, last, previous)); return result;
    }

    private ObjectNode tableChange(Table table,InputDraftHistory history) {
        TableRef original = table.original;
        List<Scope> scopes = new ArrayList<>();
        for (Scope scope : original.scopes()) {
            if ((InputWorkbookPolicy.kind(original) == InputWorkbookPolicy.Kind.HYDROGEN || InputWorkbookPolicy.shippingStacked(original)) && scope.endRow() == original.endRow()) {
                for (var range : table.current().bodyRanges()) if (range.endRow() >= scope.startRow())
                    scopes.add(new Scope(Math.max(scope.startRow(), range.startRow()), range.endRow(), scope.startColumn(), scope.endColumn()));
                continue;
            }
            int end = scope.endRow() == original.endRow() ? table.endRow : scope.endRow();
            if (table.removedColumns.isEmpty()) scopes.add(new Scope(scope.startRow(), end, scope.startColumn(), scope.endColumn()));
            else for (int token = scope.startColumn(); token <= scope.endColumn(); token++) {
                Integer position = table.columnPositions.get(token);
                if (position != null) scopes.add(new Scope(scope.startRow(), end, position, position));
            }
        }
        for (AddedColumn column : table.liveColumns()) for (var range : table.current().bodyRanges()) {
            if (InputWorkbookPolicy.kind(original) == InputWorkbookPolicy.Kind.LNG) {
                for (int row = range.startRow(); row <= range.endRow(); row++) if (!InputWorkbookPeriods.LNG_TOTALS.contains(row)) scopes.add(new Scope(row, row, table.column(column), table.column(column)));
            } else scopes.add(new Scope(range.startRow(), range.endRow(), table.column(column), table.column(column)));
        }
        for(String field:table.restoredColumns.keySet())for(var range:table.current().bodyRanges()) {
            int column=table.columnPositions.get(table.fieldTokens.get(field));scopes.add(new Scope(range.startRow(),range.endRow(),column,column));
        }
        if(!table.restoredRows.isEmpty()||!table.restoredColumns.isEmpty())scopes=new ArrayList<>(InputDraftRestoreValidation.scopes(table,history));
        ObjectNode change = (ObjectNode) json.tree(Map.of("tableId", original.key(), "startRow", original.startRow() + table.rowOffset, "endRow", table.endRow,
            "startColumn", original.startColumn(), "endColumn", table.endColumn, "dataStart", original.dataStart() + table.rowOffset, "definition", table.definition, "scopes", scopes));
        return change;
    }
    private static TableRef currentRef(Table table) {
        TableRef old = table.original;
        return new TableRef(old.id(), old.role(), old.sheet(), old.name(), old.description(), old.startRow() + table.rowOffset, table.endRow,
            old.startColumn(), table.endColumn, old.headerStart() + table.rowOffset, old.headerEnd() + table.rowOffset, old.dataStart() + table.rowOffset,
            old.rowAdd(), old.columnAdd(), table.definition.toString(), old.scopes());
    }
    private static void extendRows(Table table, int row) {
        var ranges = table.definition.withArray("bodyRanges"); ObjectNode last = (ObjectNode) ranges.get(ranges.size() - 1);
        if (row > last.path("endRow").asInt()) {
            if ((InputWorkbookPolicy.kind(table.original) == InputWorkbookPolicy.Kind.HYDROGEN || InputWorkbookPolicy.shippingStacked(table.original)) && row > last.path("endRow").asInt() + 1)
                ranges.addObject().put("startRow", row).put("endRow", row);
            else last.put("endRow", row);
            table.endRow = Math.max(table.endRow, row);
        }
    }
    private String identity(JsonNode expected, String array, String clientName, String client, String idName, String prefix) {
        if (expected == null) return prefix + UUID.randomUUID();
        for (JsonNode entry : expected.path(array)) if (entry.path(clientName).asText().equals(client)) return entry.path(idName).asText();
        throw new InputDraftPreserveChange.Failure("PRESERVATION_FAILED");
    }
    private static Object scalar(JsonNode value) { return value == null || value.isNull() ? null : value.isNumber() ? value.decimalValue() : value.textValue(); }
    private static void merge(InputWorkbookXml.SheetPatch target, InputWorkbookXml.SheetPatch source) {
        source.edits.forEach((row, cells) -> cells.forEach((column, edit) -> {
            var previous = target.edits.getOrDefault(row, Map.of()).get(column);
            target.put(row, column, edit.template() == null && previous != null ? new InputWorkbookXml.Edit(edit.value(), previous.template(), edit.replaceFormula()) : edit);
        }));
        source.raw.forEach((row, cells) -> cells.forEach((column, cell) -> target.raw(row, column, cell)));
        target.newRowOpenings.putAll(source.newRowOpenings);
    }
    private static InputDraftPreserveChange.Failure invalid() { return new InputDraftPreserveChange.Failure("STRUCTURE_VALIDATION_FAILED"); }
}
