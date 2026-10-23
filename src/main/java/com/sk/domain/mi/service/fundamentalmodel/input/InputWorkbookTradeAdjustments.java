package com.sk.domain.mi.service.fundamentalmodel.input;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.poi.ss.util.CellReference;

/** 수급 조정 입력의 로컬 파생값만 갱신한다. 외부 생산량과 고정 기준값은 보존한다. */
final class InputWorkbookTradeAdjustments {
    private static final String SHEET = "UpstreamAdjLogicInput";

    static Set<String> supply(Path source, InputWorkbookXml.SheetPatch patch, InputWorkbookReader reader,
            InputWorkbookXml.Checkpoint checkpoint) throws Exception {
        Map<String, String> expected = new LinkedHashMap<>();
        Map<InputWorkbookCalculation.Address, Object> constants = new LinkedHashMap<>();
        patch.edits.forEach((row, cells) -> {
            if (row < 3 || row > 674) return;
            if (cells.containsKey(16)) {
                expected.put("Q" + row, "IF(ISNUMBER(P" + row + "),P" + row + ",AZ" + row + ")");
                constants.put(address(row, 16), null); constants.put(address(row, 52), null);
            }
            if (cells.containsKey(9) || cells.containsKey(10)) {
                expected.put("R" + row, "I" + row + "&J" + row);
                constants.put(address(row, 9), null); constants.put(address(row, 10), null);
            }
        });
        return calculate(source, SHEET, expected, expected, expected.keySet(), constants, patch, reader, checkpoint);
    }

    static Set<String> countries(Path source, int previousEnd, int end, InputWorkbookXml.SheetPatch patch,
            InputWorkbookReader reader, InputWorkbookXml.Checkpoint checkpoint) throws Exception {
        if (patch.edits.entrySet().stream().noneMatch(e -> e.getKey() >= 4 && e.getKey() <= end && e.getValue().containsKey(20))) return Set.of();
        Map<InputWorkbookCalculation.Address, Object> constants = new LinkedHashMap<>();
        for (int row = 3; row <= Math.max(100, end); row++) constants.put(address(row, 20), null);
        return calculate(source, SHEET, Map.of("U2", "COUNTA(T3:T" + Math.max(100, previousEnd) + ")"),
            Map.of("U2", "COUNTA(T3:T" + Math.max(100, end) + ")"), Set.of("U2"), constants, patch, reader, checkpoint);
    }

    static void demandInputs(Path source, InputWorkbookXml.SheetPatch patch, InputWorkbookReader reader,
            InputWorkbookXml.Checkpoint checkpoint) throws Exception {
        String sheet = "DemandAdjLogicInput";
        Map<String, String> expected = new LinkedHashMap<>();
        Set<String> changed = new HashSet<>();
        Map<InputWorkbookCalculation.Address, Object> constants = new LinkedHashMap<>();
        for (int inputRow : List.of(2, 3)) {
            var edits = patch.edits.getOrDefault(inputRow, Map.of());
            boolean parametersChanged = edits.containsKey(4) || edits.containsKey(6);
            Set<Integer> affected = new HashSet<>();
            for (int row : List.of(3, 4, 6, 7))
                if (parametersChanged || patch.edits.getOrDefault(row, Map.of()).containsKey(10)) affected.add(row);
            if (affected.isEmpty()) continue;
            String column = inputRow == 2 ? "L" : "M";
            expected.put(column + "5", "$D$" + inputRow);
            if (edits.containsKey(4)) changed.add(column + "5");
            constants.put(new InputWorkbookCalculation.Address(sheet, "D" + inputRow), null);
            constants.put(new InputWorkbookCalculation.Address(sheet, "F" + inputRow), null);
            for (int row : affected) {
                expected.put(column + row, "MAX(0,$" + column + "$5" + (row < 5 ? "-" : "+") + "$F$" + inputRow + "*$J" + row + ")");
                changed.add(column + row);
                constants.put(new InputWorkbookCalculation.Address(sheet, "J" + row), null);
            }
        }
        for (int row = 3; row <= 7; row++) if (patch.edits.getOrDefault(row, Map.of()).containsKey(8)
                || patch.edits.getOrDefault(row, Map.of()).containsKey(9)) {
            String target = "K" + row;
            expected.put(target, "H" + row + "& \"" + (row == 7 ? " and " : " to ") + "\" &I" + row);
            changed.add(target);
            constants.put(new InputWorkbookCalculation.Address(sheet, "H" + row), null);
            constants.put(new InputWorkbookCalculation.Address(sheet, "I" + row), null);
        }
        calculate(source, sheet, expected, expected, changed, constants, patch, reader, checkpoint);
    }

    private static Set<String> calculate(Path source, String sheet, Map<String, String> originalExpected, Map<String, String> expected,
            Set<String> changed, Map<InputWorkbookCalculation.Address, Object> constants, InputWorkbookXml.SheetPatch patch,
            InputWorkbookReader reader, InputWorkbookXml.Checkpoint checkpoint) throws Exception {
        if (expected.isEmpty()) return Set.of();
        Map<String, String> originals = InputWorkbookStructure.formulas(source, sheet, expected::containsKey, checkpoint);
        if (!originals.equals(originalExpected)) throw invalid();
        Map<String, String> templates = new HashMap<>();
        InputWorkbookXml.rows(source, InputWorkbookXml.sheetParts(source).get(sheet), checkpoint, row -> InputWorkbookXml.cells(row.xml()).forEach((column, cell) -> {
            String address = InputWorkbookXml.address(row.number(), column); if (expected.containsKey(address)) templates.put(address, cell);
        }));
        Set<Integer> rows = new HashSet<>(), columns = new HashSet<>();
        constants.keySet().forEach(address -> { var ref = new CellReference(address.cell()); rows.add(ref.getRow() + 1); columns.add(ref.getCol() + 1); });
        reader.readLocal(source, List.of(new InputWorkbookReader.Window(sheet, rows::contains, columns, (row, cells) -> cells.forEach((column, cell) -> {
            var address = new InputWorkbookCalculation.Address(sheet, InputWorkbookXml.address(row, column)); if (constants.containsKey(address)) constants.put(address, cell.raw());
        }))));
        patch.edits.forEach((row, cells) -> cells.forEach((column, edit) -> {
            var address = new InputWorkbookCalculation.Address(sheet, InputWorkbookXml.address(row, column)); if (constants.containsKey(address)) constants.put(address, edit.value());
        }));
        Map<InputWorkbookCalculation.Address, String> formulas = new LinkedHashMap<>(); expected.forEach((address, formula) -> formulas.put(new InputWorkbookCalculation.Address(sheet, address), formula));
        InputWorkbookCalculation.evaluate(constants, formulas, checkpoint).forEach((address, value) -> {
            if (!changed.contains(address.cell())) return;
            String template = templates.get(address.cell()); if (template == null) throw invalid();
            if (!expected.get(address.cell()).equals(originalExpected.get(address.cell()))) template = InputWorkbookXml.valueCell(template, address.cell(), null)
                .replace("</c>", "<f>" + InputWorkbookXml.escape(expected.get(address.cell())) + "</f><v>0</v></c>");
            var ref = new CellReference(address.cell()); patch.raw(ref.getRow() + 1, ref.getCol() + 1, InputWorkbookCalculation.cache(template, value));
        });
        return Set.copyOf(changed);
    }

    private static InputWorkbookCalculation.Address address(int row, int column) { return new InputWorkbookCalculation.Address(SHEET, InputWorkbookXml.address(row, column)); }
    private static InputDraftPreserveChange.Failure invalid() { return new InputDraftPreserveChange.Failure("STRUCTURE_VALIDATION_FAILED"); }
}
