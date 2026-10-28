package com.sk.domain.mi.service.fundamentalmodel.input;

import com.fasterxml.jackson.databind.JsonNode;
import com.sk.domain.shared.repository.master.tmm.ModelInputDraftRepository.TableRef;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** 신규 PMI 행은 UUID, 원본 행은 고정 source 행 번호로 대응시킨다. 국가명은 연결 기준이 아니다. */
final class InputDraftPmiRows {
    static final String BASE = "_pmi_base_", SELECTED = "_pmi_selected_";
    static boolean user(TableRef table) { return pmi(table) && table.startColumn() == 2; }
    static boolean child(TableRef table) { return pmi(table) && (table.startColumn() == 7 || selected(table)); }
    static boolean selected(TableRef table) { return pmi(table) && table.startColumn() == 12; }
    private static boolean pmi(TableRef table) { return table.role().equals("DEMAND") && table.sheet().equals("PMILogicInput"); }
    static String prefix(TableRef table) { return selected(table) ? SELECTED : BASE; }
    static boolean derived(JsonNode row) {
        String client = row.path("clientRowKey").asText();
        return row.has("derivedFromRowId") && (client.startsWith(BASE) || client.startsWith(SELECTED));
    }
    static void creation(JsonNode row) {
        String master = row.path("derivedFromRowId").asText(), client = row.path("clientRowKey").asText();
        require(master.matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")
            && (client.equals(BASE + master) || client.equals(SELECTED + master)));
    }
    static Map<String, List<String>> validate(List<TableRef> tables, List<InputDraftRegasRows.Row> rows,
            Map<String, JsonNode> additions, Map<String, Map<Integer, InputWorkbookReader.Cell>> cells, InputDraftJson json) {
        Map<Long, TableRef> byTable = new HashMap<>(); tables.forEach(t -> byTable.put(t.id(), t));
        Map<String, InputDraftRegasRows.Row> byId = new HashMap<>(); rows.forEach(r -> byId.put(r.id(), r));
        Map<String, Map<Integer, String>> links = new LinkedHashMap<>();
        for (var row : rows) {
            TableRef table = byTable.get(row.table());
            if (!child(table) || row.source() != null) continue;
            require(row.client() != null && row.client().startsWith(prefix(table)));
            String id = row.client().substring(prefix(table).length()); var parent = byId.get(id);
            require(parent != null && user(byTable.get(parent.table())) && parent.source() == null
                && parent.baseline() == row.baseline() && java.util.Objects.equals(parent.position(), row.position()));
            require(links.computeIfAbsent(id, ignored -> new HashMap<>()).put(table.startColumn(), row.id()) == null);
        }
        Map<String, List<String>> result = new LinkedHashMap<>();
        for (var row : rows) if (user(byTable.get(row.table())) && row.source() == null) {
            Map<Integer, String> children = links.getOrDefault(row.id(), Map.of());
            require(children.size() == 2 && children.containsKey(7) && children.containsKey(12));
            result.put(row.id(), List.of(children.get(7), children.get(12)));
            if (!additions.containsKey(row.client()) || row.position() == null) continue;
            for (String child : result.get(row.id())) {
                var linked = byId.get(child); JsonNode addition = additions.get(linked.client());
                require(addition != null && derived(addition)); creation(addition);
                require(row.id().equals(addition.path("derivedFromRowId").asText()));
                int first = byTable.get(linked.table()).startColumn();
                var country = cells.getOrDefault(child, Map.of()).get(first + 2);
                var masterCountry = cells.getOrDefault(row.id(), Map.of()).get(4);
                require(country != null && masterCountry != null && !country.formula()
                    && json.sameValue(json.tree(country.raw()), json.tree(masterCountry.raw())));
                for (int column = first; column <= first + 1; column++) {
                    var empty = cells.getOrDefault(child, Map.of()).get(column);
                    require(empty != null && empty.raw() == null && !empty.formula());
                }
                var intensity = cells.getOrDefault(child, Map.of()).get(first + 3);
                require(intensity != null && (first == 7 ? intensity.raw() == null && !intensity.formula() : intensity.formula()));
            }
        }
        for (JsonNode addition : additions.values()) if (derived(addition)) {
            creation(addition); String master = addition.path("derivedFromRowId").asText();
            var parent = byId.get(master);
            require(parent != null && additions.containsKey(parent.client())
                && result.getOrDefault(master, List.of()).contains(addition.path("rowId").asText()));
        }
        return Map.copyOf(result);
    }
    /** 과거 v2의 독립 원본 행 판독에는 새 대응 제약을 소급하지 않는다. */
    static Map<String, List<String>> validate(List<TableRef> tables, List<InputDraftRegasRows.Row> rows,
            Map<String, JsonNode> additions, Map<String, Map<Integer, InputWorkbookReader.Cell>> cells,
            InputDraftJson json, Set<String> sourceParents) {
        Map<String, List<String>> result = new LinkedHashMap<>(validate(tables, rows, additions, cells, json));
        var originals = originalLinks(tables, rows, sourceParents);
        require(originals.keySet().containsAll(sourceParents));
        result.putAll(originals);
        return Map.copyOf(result);
    }
    /** 명시적 원본 구조 변경 대상만 source/baseline과 child 역할·활성 위치로 대조한다. */
    static Map<String, List<String>> originalLinks(List<TableRef> tables, List<InputDraftRegasRows.Row> rows, Set<String> targets) {
        Map<Long, TableRef> byTable = new HashMap<>(); tables.forEach(table -> byTable.put(table.id(), table));
        Map<String, List<String>> result = new LinkedHashMap<>();
        for (var parent : rows) {
            if (!targets.contains(parent.id()) || parent.source() == null || !user(byTable.get(parent.table()))) continue;
            Map<Integer, String> children = new HashMap<>(); boolean valid = true;
            for (var row : rows) if (child(byTable.get(row.table())) && parent.baseline() == row.baseline()
                    && parent.source().equals(row.source())) {
                if (!java.util.Objects.equals(parent.position(), row.position())
                        || children.putIfAbsent(byTable.get(row.table()).startColumn(), row.id()) != null) valid = false;
            }
            if (valid && children.size() == 2 && children.containsKey(7) && children.containsKey(12))
                result.put(parent.id(), List.of(children.get(7), children.get(12)));
        }
        return Map.copyOf(result);
    }
    private static void require(boolean condition) { if (!condition) throw InputDraftException.internal(); }
}
