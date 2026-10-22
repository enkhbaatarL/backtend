package com.sk.domain.mi.service.fundamentalmodel.input;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.poi.ss.util.CellReference;

/** 운송 입력의 승인된 내부 계산. Annual Shipping Route Costs 보존 영역은 갱신하지 않는다. */
final class InputWorkbookShipping {
    record Result(Set<String> calculated, List<String> added) {}

    /** 이어 배치된 운임/운하 표의 논리 행 순서를 따른다. 다른 표와 A 운송비는 보존한다. */
    static Result stacked(Path source, int first, int styleRow, List<Integer> rows, Set<Integer> created,
            boolean disruption, InputWorkbookXml.SheetPatch patch, InputWorkbookReader reader,
            InputWorkbookXml.Checkpoint checkpoint) throws Exception {
        String sheet = "ShippingCostInput"; boolean charter = first == 6 || first == 17;
        Map<String, String> expected = new LinkedHashMap<>();
        boolean modeChanged = patch.edits.getOrDefault(3, Map.of()).containsKey(3), affected = modeChanged;
        for (int i = 0; i < rows.size(); i++) {
            int row = rows.get(i), previous = i == 0 ? 0 : rows.get(i - 1);
            if (disruption) { if (created.contains(row)) expected.put("D" + row, "$D$" + first); continue; }
            var changed = patch.edits.getOrDefault(row, Map.of());
            affected |= created.contains(row) || changed.containsKey(3) || previous > 0 && patch.edits.getOrDefault(previous, Map.of()).containsKey(4);
            if (charter && created.contains(row)) expected.put("C" + row, "C" + previous + "*(1.02)");
            if (affected) expected.put("E" + row, previous == 0 ? "C" + row
                : "IF($C$3=\"RE Base Case\",C" + row + ",IFERROR(E" + previous + "*(1+D" + previous + "),C" + row + "))");
        }
        if (expected.isEmpty()) return new Result(Set.of(), List.of());
        String reference = disruption ? "D" + (first + 1) : "E" + (first + 1);
        var originals = InputWorkbookStructure.formulas(source, sheet, a -> expected.containsKey(a) || a.equals(reference) || charter && a.equals("C" + (first + 1)), checkpoint);
        String referenceFormula = disruption ? "$D$" + first
            : "IF($C$3=\"RE Base Case\",C" + (first + 1) + ",IFERROR(E" + first + "*(1+D" + first + "),C" + (first + 1) + "))";
        if (!referenceFormula.equals(originals.get(reference))) throw invalid();
        if (charter && !("C" + first + "*(1.02)").equals(originals.get("C" + (first + 1)))) throw invalid();
        Map<String, String> templates = new HashMap<>();
        InputWorkbookXml.rows(source, InputWorkbookXml.sheetParts(source).get(sheet), checkpoint, row -> {
            if (rows.contains(row.number()) || row.number() == styleRow) InputWorkbookXml.cells(row.xml()).forEach((column, cell) -> {
                if (column >= 3 && column <= 5) templates.put(InputWorkbookXml.address(row.number(), column), cell);
            });
        });
        Map<InputWorkbookCalculation.Address, String> formulas = new LinkedHashMap<>(); List<String> added = new ArrayList<>();
        for (var entry : expected.entrySet()) {
            String target = entry.getKey(), template = templates.get(target); var ref = new CellReference(target);
            if (created.contains(ref.getRow() + 1)) {
                if (originals.containsKey(target) || template != null && java.util.regex.Pattern.compile("<(?:f|v|is)(?=\\s|/?>)").matcher(template).find()) throw invalid();
                if (template == null) template = templates.get(InputWorkbookXml.address(styleRow, ref.getCol() + 1));
                if (template == null) throw invalid();
                template = InputWorkbookXml.valueCell(template, target, null).replace("</c>", "<f>" + InputWorkbookXml.escape(entry.getValue()) + "</f><v>0</v></c>");
                added.add(target);
            } else if (!entry.getValue().equals(originals.get(target)) || template == null) throw invalid();
            patch.raw(ref.getRow() + 1, ref.getCol() + 1, template);
            formulas.put(new InputWorkbookCalculation.Address(sheet, target), entry.getValue());
        }
        Map<InputWorkbookCalculation.Address, Object> constants = new LinkedHashMap<>();
        constants.put(new InputWorkbookCalculation.Address(sheet, "C3"), null);
        for (int row : rows) for (int column = 3; column <= 5; column++) {
            var address = new InputWorkbookCalculation.Address(sheet, InputWorkbookXml.address(row, column));
            if (!formulas.containsKey(address)) constants.put(address, null);
        }
        reader.readLocal(source, List.of(new InputWorkbookReader.Window(sheet, row -> row == 3 || rows.contains(row), Set.of(3, 4, 5), (row, cells) -> cells.forEach((column, cell) -> {
            var address = new InputWorkbookCalculation.Address(sheet, InputWorkbookXml.address(row, column));
            if (constants.containsKey(address)) constants.put(address, cell.raw());
        }))));
        patch.edits.forEach((row, cells) -> cells.forEach((column, edit) -> {
            var address = new InputWorkbookCalculation.Address(sheet, InputWorkbookXml.address(row, column));
            if (constants.containsKey(address)) constants.put(address, edit.value());
        }));
        InputWorkbookCalculation.evaluate(constants, formulas, checkpoint).forEach((address, value) -> {
            var ref = new CellReference(address.cell());
            patch.raw(ref.getRow() + 1, ref.getCol() + 1, InputWorkbookCalculation.cache(patch.raw.get(ref.getRow() + 1).get(ref.getCol() + 1), value));
        });
        return new Result(Set.copyOf(expected.keySet()), List.copyOf(added));
    }

    static Result routes(Path source, int end, int styleRow, Set<Integer> created, InputWorkbookXml.SheetPatch patch,
            InputWorkbookReader reader, InputWorkbookXml.Checkpoint checkpoint) throws Exception {
        String sheet = "ShippingCostInput"; Map<String, String> expected = new LinkedHashMap<>();
        Set<Integer> rows = new HashSet<>(created);
        patch.edits.forEach((row, cells) -> {
            if (row >= 4 && row <= end && Set.of(31, 34, 35, 38).stream().anyMatch(cells::containsKey)) rows.add(row);
        });
        if (rows.isEmpty()) return new Result(Set.of(), List.of());
        for (int row : rows) {
            var changed = patch.edits.getOrDefault(row, Map.of());
            boolean duration = created.contains(row) || Set.of(31, 34, 38).stream().anyMatch(changed::containsKey);
            if (duration) for (int column : List.of(32, 33)) expected.put(InputWorkbookXml.address(row, column), routeFormula(row, column));
            for (int column : List.of(36, 37)) expected.put(InputWorkbookXml.address(row, column), routeFormula(row, column));
        }
        Set<String> bases = Set.of("AF4", "AG4", "AJ4", "AK4");
        var originals = InputWorkbookStructure.formulas(source, sheet, a -> expected.containsKey(a) || bases.contains(a), checkpoint);
        for (int column : List.of(32, 33, 36, 37)) if (!routeFormula(4, column).equals(originals.get(InputWorkbookXml.address(4, column)))) throw invalid();
        Map<String, String> templates = new HashMap<>();
        InputWorkbookXml.rows(source, InputWorkbookXml.sheetParts(source).get(sheet), checkpoint, row -> {
            if (rows.contains(row.number()) || row.number() == styleRow) InputWorkbookXml.cells(row.xml()).forEach((column, cell) -> {
                if (Set.of(32, 33, 36, 37).contains(column)) templates.put(InputWorkbookXml.address(row.number(), column), cell);
            });
        });
        Map<InputWorkbookCalculation.Address, String> formulas = new LinkedHashMap<>(); List<String> added = new ArrayList<>();
        for (var entry : expected.entrySet()) {
            String target = entry.getKey(), template = templates.get(target); var ref = new CellReference(target); int row = ref.getRow() + 1, column = ref.getCol() + 1;
            if (created.contains(row)) {
                if (originals.containsKey(target) || template != null && java.util.regex.Pattern.compile("<(?:f|v|is)(?=\\s|/?>)").matcher(template).find()) throw invalid();
                if (template == null) template = templates.get(InputWorkbookXml.address(styleRow, column));
                if (template == null) throw invalid();
                template = InputWorkbookXml.valueCell(template, target, null).replace("</c>", "<f>" + InputWorkbookXml.escape(entry.getValue()) + "</f><v>0</v></c>"); added.add(target);
            } else if (!entry.getValue().equals(originals.get(target)) || template == null) throw invalid();
            patch.raw(row, column, template); formulas.put(new InputWorkbookCalculation.Address(sheet, target), entry.getValue());
        }
        Map<InputWorkbookCalculation.Address, Object> constants = new LinkedHashMap<>();
        for (int row : rows) for (int column : List.of(31, 32, 33, 34, 35, 38)) {
            var address = new InputWorkbookCalculation.Address(sheet, InputWorkbookXml.address(row, column)); if (!formulas.containsKey(address)) constants.put(address, null);
        }
        constants.put(new InputWorkbookCalculation.Address(sheet, "D61"), null); constants.put(new InputWorkbookCalculation.Address(sheet, "D72"), null);
        reader.readLocal(source, List.of(new InputWorkbookReader.Window(sheet, row -> rows.contains(row) || row == 61 || row == 72, Set.of(4, 31, 32, 33, 34, 35, 38), (row, cells) -> cells.forEach((column, cell) -> {
            var address = new InputWorkbookCalculation.Address(sheet, InputWorkbookXml.address(row, column)); if (constants.containsKey(address)) constants.put(address, cell.raw());
        }))));
        if (!(constants.get(new InputWorkbookCalculation.Address(sheet, "D61")) instanceof Number) || !(constants.get(new InputWorkbookCalculation.Address(sheet, "D72")) instanceof Number)) throw invalid();
        patch.edits.forEach((row, cells) -> cells.forEach((column, edit) -> {
            var address = new InputWorkbookCalculation.Address(sheet, InputWorkbookXml.address(row, column)); if (constants.containsKey(address)) constants.put(address, edit.value());
        }));
        InputWorkbookCalculation.evaluate(constants, formulas, checkpoint).forEach((address, value) -> {
            var ref = new CellReference(address.cell()); int row = ref.getRow() + 1, column = ref.getCol() + 1;
            patch.raw(row, column, InputWorkbookCalculation.cache(patch.raw.get(row).get(column), value));
        });
        return new Result(Set.copyOf(expected.keySet()), List.copyOf(added));
    }

    private static String routeFormula(int row, int column) {
        boolean suez = column == 32 || column == 36;
        String selected = "IF(ISNUMBER(SEARCH(\"" + (suez ? "Suez" : "Panama") + "\",AL" + row + ")),";
        return column < 34 ? selected + "AE" + row + "+$D$" + (suez ? 61 : 72) + "/(AH" + row + "*24),AE" + row + ")"
            : selected + "(" + (suez ? "AF" : "AG") + row + "/AE" + row + ")*AI" + row + ",AI" + row + ")";
    }

    static Set<String> technical(Path source, InputWorkbookXml.SheetPatch patch, InputWorkbookReader reader,
            InputWorkbookXml.Checkpoint checkpoint) throws Exception {
        if (!patch.edits.getOrDefault(83, Map.of()).containsKey(2)) return Set.of();
        String sheet = "ShippingCostInput", formula = "B83*(1-C83)";
        if (!formula.equals(InputWorkbookStructure.formulas(source, sheet, "D83"::equals, checkpoint).get("D83"))) throw invalid();
        Map<Integer, InputWorkbookReader.Cell> inputs = new HashMap<>();
        reader.readLocal(source, List.of(new InputWorkbookReader.Window(sheet, row -> row == 83, Set.of(3), (row, cells) -> inputs.putAll(cells))));
        if (!inputs.containsKey(3) || !(inputs.get(3).raw() instanceof Number)) throw invalid();
        Map<InputWorkbookCalculation.Address, Object> constants = new HashMap<>();
        constants.put(new InputWorkbookCalculation.Address(sheet, "B83"), patch.edits.get(83).get(2).value());
        constants.put(new InputWorkbookCalculation.Address(sheet, "C83"), inputs.get(3).raw());
        var target = new InputWorkbookCalculation.Address(sheet, "D83");
        Object value = InputWorkbookCalculation.evaluate(constants, Map.of(target, formula), checkpoint).get(target);
        String[] template = {null};
        InputWorkbookXml.rows(source, InputWorkbookXml.sheetParts(source).get(sheet), checkpoint, row -> {
            if (row.number() == 83) template[0] = InputWorkbookXml.cells(row.xml()).get(4);
        });
        if (template[0] == null) throw invalid();
        patch.raw(83, 4, InputWorkbookCalculation.cache(template[0], value));
        return Set.of("D83");
    }

    private static InputDraftPreserveChange.Failure invalid() { return new InputDraftPreserveChange.Failure("STRUCTURE_VALIDATION_FAILED"); }
}
