package com.sk.domain.mi.service.fundamentalmodel.input;

import com.fasterxml.jackson.databind.JsonNode;
import com.sk.domain.shared.repository.master.tmm.ModelInputDraftRepository.TableRef;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** BCRegas master와 Regas 파생 행의 내부 identity. 공개 client key와 분리한다. */
final class InputDraftRegasRows {
    static final String PREFIX = "_regas_";
    record Row(long table, String id, Integer source, Integer position, String client, long baseline) {}

    static boolean derived(JsonNode row) { return row.has("derivedFromRowId"); }

    static void creation(JsonNode change) {
        String master = change.path("derivedFromRowId").asText();
        require(master.matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"));
        require((PREFIX + master).equals(change.path("clientRowKey").asText()));
    }

    /** 원본 쌍은 원본 위치로, 추가 쌍은 저장된 master UUID로 연결한다. 현재 위치/이름으로 재매칭하지 않는다. */
    static Map<String, String> validate(List<TableRef> tables, Map<Long, InputReadDefinition> originalDefinitions,
            List<Row> rows, Map<String, JsonNode> additions, List<JsonNode> commands, JsonNode changedRows) {
        return validate(tables, originalDefinitions, rows, additions, commands, changedRows, false);
    }

    static Map<String, String> links(List<TableRef> tables, Map<Long, InputReadDefinition> originalDefinitions, List<Row> rows) {
        return validate(tables, originalDefinitions, rows, Map.of(), List.of(), List.of(), true);
    }

    private static Map<String, String> validate(List<TableRef> tables, Map<Long, InputReadDefinition> originalDefinitions,
            List<Row> rows, Map<String, JsonNode> additions, List<JsonNode> commands, Iterable<JsonNode> changedRows, boolean forced) {
        Set<String> masterCommands = new HashSet<>();
        tables.stream().filter(InputDraftRegasRows::master).forEach(t -> masterCommands.add(t.key()));
        Set<String> childTables = new HashSet<>();
        tables.stream().filter(t -> t.role().equals("SUPPLY") && t.sheet().equals("RegasAdjustment")
            && originalDefinitions.get(t.id()).body(11)).forEach(t -> childTables.add(t.key()));
        boolean relevant = forced || additions.values().stream().anyMatch(InputDraftRegasRows::derived)
            || commands.stream().anyMatch(c -> masterCommands.contains(c.path("tableId").asText())
                && Set.of("ADD_ROW", "DELETE_ROW").contains(c.path("type").asText()));
        for (JsonNode change : changedRows) if (masterCommands.contains(change.path("tableId").asText())
                || childTables.contains(change.path("tableId").asText())) relevant = true;
        if (!relevant) return Map.of();
        List<TableRef> masters = tables.stream().filter(InputDraftRegasRows::master).toList();
        List<TableRef> children = tables.stream().filter(t -> t.role().equals("SUPPLY") && t.sheet().equals("RegasAdjustment")
            && originalDefinitions.get(t.id()).body(11)).toList();
        require(masters.size() == 1 && children.size() == 1);
        long masterTable = masters.get(0).id(), childTable = children.get(0).id();
        Map<String, Row> byId = new HashMap<>(); Map<String, Row> sourceChildren = new HashMap<>();
        for (Row row : rows) {
            require(byId.putIfAbsent(row.id, row) == null);
            if (row.table == childTable && row.source != null)
                require(sourceChildren.putIfAbsent(row.baseline + "/" + row.source, row) == null);
        }
        Map<String, String> links = new LinkedHashMap<>(); Set<String> newMasters = new HashSet<>();
        for (JsonNode addition : additions.values()) if (!derived(addition) && masters.get(0).key().equals(addition.path("tableId").asText()))
            newMasters.add(addition.path("rowId").asText());
        for (Row row : rows) if (row.table == childTable && row.source == null) {
            require(row.client != null && row.client.startsWith(PREFIX));
            String id = row.client.substring(PREFIX.length()); Row parent = byId.get(id);
            require(parent != null && parent.table == masterTable && parent.source == null && parent.baseline == row.baseline);
            require(links.putIfAbsent(id, row.id) == null);
        }
        for (Row row : rows) if (row.table == masterTable) {
            if (row.source != null) {
                Row child = sourceChildren.get(row.baseline + "/" + (row.source + 3));
                require(child != null && links.putIfAbsent(row.id, child.id) == null);
            }
            Row child = byId.get(links.get(row.id)); require(child != null);
            require((row.position == null) == (child.position == null));
            if (row.position != null) require(child.position == row.position + 3);
        }
        Set<String> linkedNew = new HashSet<>();
        for (JsonNode addition : additions.values()) if (derived(addition)) {
            creation(addition); String id = addition.path("derivedFromRowId").asText();
            require(children.get(0).key().equals(addition.path("tableId").asText()) && newMasters.contains(id)
                && linkedNew.add(id) && addition.path("rowId").asText().equals(links.get(id)));
        }
        require(linkedNew.equals(newMasters));
        return Map.copyOf(links);
    }

    private static boolean master(TableRef table) { return table.role().equals("SUPPLY") && table.sheet().equals("BCRegasification"); }
    private static void require(boolean condition) { if (!condition) throw InputDraftException.internal(); }
}
