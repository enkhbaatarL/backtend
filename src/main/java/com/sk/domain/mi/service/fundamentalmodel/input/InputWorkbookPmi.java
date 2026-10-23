package com.sk.domain.mi.service.fundamentalmodel.input;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** 새 Selected 행의 원본 SUMIFS만 생성한다. 기존 선택표/지리정보는 계산하지 않는다. */
final class InputWorkbookPmi {
    private static final String SHEET = "PMILogicInput";
    static String formula(int row) { return "SUMIFS($E$9:$E$1048576,$D$9:$D$1048576,N" + row + ")"; }

    static void append(Path source, int styleRow, Set<Integer> created, InputWorkbookXml.SheetPatch patch,
            InputWorkbookReader reader, InputWorkbookXml.Checkpoint checkpoint) throws Exception {
        var originals = InputWorkbookStructure.formulas(source, SHEET,
            address -> Set.of("O9", "O73", "O" + styleRow).contains(address), checkpoint);
        if (!formula(9).equals(originals.get("O9")) || !formula(73).equals(originals.get("O73"))
                || !formula(styleRow).equals(originals.get("O" + styleRow))) throw invalid();
        Map<Integer, Map<Integer, Object>> inputs = new LinkedHashMap<>();
        int[] last = {9};
        // 끝까지 스캔해 참조 열의 모든 실제 값을 읽는다. 뒤쪽의 확인된 빈 범위만 계산용 식에서 줄인다.
        reader.readLocal(source, List.of(new InputWorkbookReader.Window(SHEET, row -> row >= 9, Set.of(4, 5), (row, cells) -> {
            Map<Integer, Object> values = new HashMap<>();
            cells.forEach((column, cell) -> { if (cell.formula()) throw invalid(); if (cell.raw() != null) values.put(column, cell.raw()); });
            if (!values.isEmpty()) { inputs.put(row, values); last[0] = Math.max(last[0], row); }
        })));
        patch.edits.forEach((row, edits) -> {
            if (row < 9) return;
            for (int column : List.of(4, 5)) if (edits.containsKey(column)) {
                inputs.computeIfAbsent(row, ignored -> new HashMap<>()).put(column, edits.get(column).value()); last[0] = Math.max(last[0], row);
            }
        });
        Map<Integer, String> templates = new HashMap<>(); String part = InputWorkbookXml.sheetParts(source).get(SHEET);
        InputWorkbookXml.rows(source, part, checkpoint, row -> {
            if (row.number() == styleRow) templates.putAll(InputWorkbookXml.cells(row.xml()));
            if (created.contains(row.number())) {
                String existing = InputWorkbookXml.cells(row.xml()).get(15);
                if (existing != null && java.util.regex.Pattern.compile("<(?:f|v|is)(?=\\s|/?>)").matcher(existing).find()) throw invalid();
            }
        });
        if (templates.get(15) == null) throw invalid();
        Map<InputWorkbookCalculation.Address, Object> constants = new LinkedHashMap<>();
        inputs.forEach((row, cells) -> cells.forEach((col, value) -> constants.put(new InputWorkbookCalculation.Address(SHEET, InputWorkbookXml.address(row, col)), value)));
        Map<InputWorkbookCalculation.Address, String> formulas = new LinkedHashMap<>();
        for (int row : created) {
            var country = patch.edits.getOrDefault(row, Map.of()).get(14); if (country == null) throw invalid();
            constants.put(new InputWorkbookCalculation.Address(SHEET, "N" + row), country.value());
            formulas.put(new InputWorkbookCalculation.Address(SHEET, "O" + row), formula(row).replace("1048576", Integer.toString(last[0])));
        }
        var values = InputWorkbookCalculation.evaluate(constants, formulas,
            List.of(new InputWorkbookCalculation.KnownRange(SHEET, new InputWorkbookStructure.Area(9, last[0], 4, 5))), checkpoint);
        for (int row : created) {
            String address = "O" + row;
            String template = InputWorkbookXml.valueCell(templates.get(15), address, null)
                .replace("</c>", "<f>" + formula(row) + "</f><v>0</v></c>");
            patch.raw(row, 15, InputWorkbookCalculation.cache(template, values.get(new InputWorkbookCalculation.Address(SHEET, address))));
        }
    }
    private static InputDraftPreserveChange.Failure invalid() { return new InputDraftPreserveChange.Failure("STRUCTURE_VALIDATION_FAILED"); }
}
