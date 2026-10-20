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

/** 배분 입력의 조정 비율만 계산한다. 연결된 A 기준표는 저장값을 읽고 보존한다. */
final class InputWorkbookCapacity {
    record Result(Set<String> calculated, List<String> added) {}
    private static final String SHEET = "MidstreamCapacityInput";
    private static final String BASE = "IF(OR(DZ4=\"Austria\", DZ4=\"Czech Republic\"), INDEX($FL$4:$FM$11, MATCH(DX4, $FK$4:$FK$11, 0), MATCH(EA4, $FL$3:$FM$3, 0)), EB4)";

    static Result shares(Path source, int end, int styleRow, Set<Integer> created, InputWorkbookXml.SheetPatch patch,
            InputWorkbookReader reader, InputWorkbookXml.Checkpoint checkpoint) throws Exception {
        Set<Integer> rows = new HashSet<>(created);
        patch.edits.forEach((row, cells) -> { if (row >= 4 && row <= end && Set.of(128, 130, 131, 132).stream().anyMatch(cells::containsKey)) rows.add(row); });
        if (rows.isEmpty()) return new Result(Set.of(), List.of());
        Set<String> targets = new HashSet<>(); rows.forEach(row -> targets.add("EC" + row));
        var originals = InputWorkbookStructure.formulas(source, SHEET, a -> a.equals("EC4") || targets.contains(a), checkpoint);
        if (!BASE.equals(originals.get("EC4"))) throw invalid();
        Map<Integer, String> templates = new HashMap<>();
        InputWorkbookXml.rows(source, InputWorkbookXml.sheetParts(source).get(SHEET), checkpoint, row -> {
            if (rows.contains(row.number()) || row.number() == styleRow) {
                String cell = InputWorkbookXml.cells(row.xml()).get(133); if (cell != null) templates.put(row.number(), cell);
            }
        });
        Map<InputWorkbookCalculation.Address, Object> constants = new LinkedHashMap<>();
        Map<InputWorkbookCalculation.Address, String> formulas = new LinkedHashMap<>(); List<String> added = new ArrayList<>();
        for (int row : rows) {
            String target = "EC" + row, expected = InputWorkbookFormula.translate(BASE, row - 4, 0), template = templates.get(row);
            if (created.contains(row)) {
                if (originals.containsKey(target) || template != null && java.util.regex.Pattern.compile("<(?:f|v|is)(?=\\s|/?>)").matcher(template).find()) throw invalid();
                if (template == null) template = templates.get(styleRow);
                if (template == null) throw invalid();
                template = InputWorkbookXml.valueCell(template, target, null).replace("</c>", "<f>" + InputWorkbookXml.escape(expected) + "</f><v>0</v></c>"); added.add(target);
            } else if (!expected.equals(originals.get(target)) || template == null) throw invalid();
            patch.raw(row, 133, template); formulas.put(address(row, 133), expected);
            for (int column : List.of(128, 130, 131, 132)) constants.put(address(row, column), null);
        }
        for (int row = 3; row <= 11; row++) for (int column = 167; column <= 169; column++) constants.put(address(row, column), null);
        reader.readLocal(source, List.of(new InputWorkbookReader.Window(SHEET, row -> rows.contains(row) || row >= 3 && row <= 11,
            Set.of(128, 130, 131, 132, 167, 168, 169), (row, cells) -> cells.forEach((column, cell) -> {
                var address = address(row, column); if (constants.containsKey(address)) constants.put(address, cell.raw());
            }))));
        patch.edits.forEach((row, cells) -> cells.forEach((column, edit) -> {
            var address = address(row, column); if (constants.containsKey(address)) constants.put(address, edit.value());
        }));
        InputWorkbookCalculation.evaluate(constants, formulas, checkpoint).forEach((address, value) -> {
            int row = new CellReference(address.cell()).getRow() + 1; patch.raw(row, 133, InputWorkbookCalculation.cache(patch.raw.get(row).get(133), value));
        });
        return new Result(Set.copyOf(targets), List.copyOf(added));
    }

    /** 기존 외부/Helper cache는 보존한다. 원본 예약 C가 없는 신규 행만 R&S 식을 만든다. */
    static Result infrastructure(Path source, int styleRow, Set<Integer> created, InputWorkbookXml.SheetPatch patch,
            InputWorkbookReader reader, InputWorkbookXml.Checkpoint checkpoint) throws Exception {
        if (created.isEmpty()) return new Result(Set.of(), List.of());
        Set<String> targets = new HashSet<>(); created.forEach(row -> targets.add("C" + row));
        var originals = InputWorkbookStructure.formulas(source, SHEET, a -> a.equals("C11") || targets.contains(a), checkpoint);
        if (!"R11&S11".equals(originals.get("C11"))) throw invalid();
        Map<Integer, String> cells = new HashMap<>();
        InputWorkbookXml.rows(source, InputWorkbookXml.sheetParts(source).get(SHEET), checkpoint, row -> {
            if (created.contains(row.number()) || row.number() == styleRow) {
                String cell = InputWorkbookXml.cells(row.xml()).get(3); if (cell != null) cells.put(row.number(), cell);
            }
        });
        Map<InputWorkbookCalculation.Address, String> formulas = new LinkedHashMap<>();
        Map<InputWorkbookCalculation.Address, Object> constants = new LinkedHashMap<>(); List<String> added = new ArrayList<>();
        for (int row : created) {
            String target = "C" + row, expected = "R" + row + "&S" + row;
            if (originals.containsKey(target)) { if (!expected.equals(originals.get(target))) throw invalid(); continue; }
            String template = cells.get(row);
            if (template != null && java.util.regex.Pattern.compile("<(?:f|v|is)(?=\\s|/?>)").matcher(template).find()) throw invalid();
            if (template == null) template = cells.get(styleRow); if (template == null) throw invalid();
            String cell = InputWorkbookXml.valueCell(template, target, null).replace("</c>", "<f>" + InputWorkbookXml.escape(expected) + "</f><v>0</v></c>");
            patch.raw(row, 3, cell); formulas.put(address(row, 3), expected); added.add(target);
            constants.put(address(row, 18), null); constants.put(address(row, 19), null);
        }
        if (formulas.isEmpty()) return new Result(Set.of(), List.of());
        reader.readLocal(source, List.of(new InputWorkbookReader.Window(SHEET, created::contains, Set.of(18, 19), (row, values) -> values.forEach((column, cell) -> {
            var key = address(row, column); if (constants.containsKey(key)) constants.put(key, cell.raw());
        }))));
        InputWorkbookCalculation.evaluate(constants, formulas, checkpoint).forEach((address, value) -> {
            int row = new CellReference(address.cell()).getRow() + 1; patch.raw(row, 3, InputWorkbookCalculation.cache(patch.raw.get(row).get(3), value));
        });
        return new Result(Set.copyOf(added), List.copyOf(added));
    }

    private static InputWorkbookCalculation.Address address(int row, int column) { return new InputWorkbookCalculation.Address(SHEET, InputWorkbookXml.address(row, column)); }
    private static InputDraftPreserveChange.Failure invalid() { return new InputDraftPreserveChange.Failure("STRUCTURE_VALIDATION_FAILED"); }
}
