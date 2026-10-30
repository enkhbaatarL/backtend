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

/** 수요 입력의 연결값만 갱신한다. 별도 A 선택표는 원본을 보존한다. */
final class InputWorkbookDemandLogic {
    record Result(Set<String> calculated, List<String> added) {}
    private static final String MACRO = "MacroParametersLogicInput";

    static Result macro(Path source, Path baseline, int end, int styleRow, Set<Integer> created,
            InputWorkbookXml.SheetPatch patch, InputWorkbookReader reader, InputWorkbookXml.Checkpoint checkpoint) throws Exception {
        Set<Integer> rows = new HashSet<>(created);
        patch.edits.forEach((row, cells) -> { if (row >= 9 && row <= end && (cells.containsKey(9) || cells.containsKey(12))) rows.add(row); });
        if (rows.isEmpty()) return new Result(Set.of(), List.of());
        Set<String> targets = new HashSet<>(); rows.forEach(row -> targets.add("Q" + row));
        var originals = InputWorkbookStructure.formulas(source, MACRO, a -> targets.contains(a) || a.equals("Q9"), checkpoint);
        if (!"I9&L9".equals(InputWorkbookStructure.formulas(baseline, MACRO, "Q9"::equals, checkpoint).get("Q9"))) throw invalid();
        Map<Integer, String> templates = new HashMap<>();
        InputWorkbookXml.rows(source, InputWorkbookXml.sheetParts(source).get(MACRO), checkpoint, row -> {
            if (rows.contains(row.number()) || row.number() == styleRow) {
                String cell = InputWorkbookXml.cells(row.xml()).get(17); if (cell != null) templates.put(row.number(), cell);
            }
        });
        Map<InputWorkbookCalculation.Address, String> formulas = new LinkedHashMap<>();
        Map<InputWorkbookCalculation.Address, Object> constants = new LinkedHashMap<>(); List<String> added = new ArrayList<>();
        for (int row : rows) {
            String target = "Q" + row, expected = "I" + row + "&L" + row, template = templates.get(row);
            if (created.contains(row)) {
                if (originals.containsKey(target) || template != null && java.util.regex.Pattern.compile("<(?:f|v|is)(?=\\s|/?>)").matcher(template).find()) throw invalid();
                if (template == null) template = templates.get(styleRow);
                if (template == null) template = InputWorkbookRows.template(baseline, MACRO, 9, checkpoint).get(17);
                if (template == null) throw invalid();
                template = InputWorkbookXml.valueCell(template, target, null).replace("</c>", "<f>" + InputWorkbookXml.escape(expected) + "</f><v>0</v></c>");
                added.add(target);
            } else if (!expected.equals(originals.get(target)) || template == null) throw invalid();
            patch.raw(row, 17, template); formulas.put(address(MACRO, row, 17), expected);
            constants.put(address(MACRO, row, 9), null); constants.put(address(MACRO, row, 12), null);
        }
        reader.readLocal(source, List.of(new InputWorkbookReader.Window(MACRO, rows::contains, Set.of(9, 12), (row, cells) -> cells.forEach((column, cell) ->
            constants.put(address(MACRO, row, column), cell.raw())))));
        patch.edits.forEach((row, cells) -> cells.forEach((column, edit) -> {
            var key = address(MACRO, row, column); if (constants.containsKey(key)) constants.put(key, edit.value());
        }));
        InputWorkbookCalculation.evaluate(constants, formulas, checkpoint).forEach((address, value) -> {
            int row = new CellReference(address.cell()).getRow() + 1;
            patch.raw(row, 17, InputWorkbookCalculation.cache(patch.raw.get(row).get(17), value));
        });
        return new Result(Set.copyOf(targets), List.copyOf(added));
    }

    private static InputWorkbookCalculation.Address address(String sheet, int row, int column) { return new InputWorkbookCalculation.Address(sheet, InputWorkbookXml.address(row, column)); }
    private static InputDraftPreserveChange.Failure invalid() { return new InputDraftPreserveChange.Failure("STRUCTURE_VALIDATION_FAILED"); }
}
