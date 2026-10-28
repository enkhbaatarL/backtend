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

/** 국가 입력에 연결된 지리 정보만 계산한다. Selected 표와 General 원본은 보존한다. */
final class InputWorkbookWeather {
    record Result(Set<String> calculated, List<String> added) {}
    private static final String SHEET = "WeatherLogicInput";

    static Result geography(Path source, Path baseline, int firstColumn, int end, int styleRow, Set<Integer> created,
            InputWorkbookXml.SheetPatch patch, InputWorkbookReader reader, InputWorkbookXml.Checkpoint checkpoint) throws Exception {
        int countryColumn = firstColumn + 2; Set<Integer> rows = new HashSet<>(created);
        patch.edits.forEach((row, cells) -> { if (row >= 9 && row <= end && cells.containsKey(countryColumn)) rows.add(row); });
        if (rows.isEmpty()) return new Result(Set.of(), List.of());
        Set<String> targets = new HashSet<>(), bases = new HashSet<>();
        for (int column = firstColumn; column <= firstColumn + 1; column++) {
            bases.add(InputWorkbookXml.address(9, column));
            for (int row : rows) targets.add(InputWorkbookXml.address(row, column));
        }
        Map<String, String> sourceFormulas = InputWorkbookStructure.formulas(source, SHEET, a -> targets.contains(a) || bases.contains(a), checkpoint);
        Map<String, String> baseFormulas = InputWorkbookStructure.formulas(baseline, SHEET, bases::contains, checkpoint);
        Map<String, String> raw = new HashMap<>();
        InputWorkbookXml.rows(source, InputWorkbookXml.sheetParts(source).get(SHEET), checkpoint, row -> {
            if (!rows.contains(row.number()) && row.number() != styleRow) return;
            InputWorkbookXml.cells(row.xml()).forEach((column, cell) -> {
                if (column >= firstColumn && column <= firstColumn + 1) raw.put(InputWorkbookXml.address(row.number(), column), cell);
            });
        });
        Map<InputWorkbookCalculation.Address, String> formulas = new LinkedHashMap<>();
        Map<InputWorkbookCalculation.Address, Object> constants = new LinkedHashMap<>(); List<String> added = new ArrayList<>();
        for (String target : targets) {
            var ref = new CellReference(target); int row = ref.getRow() + 1, column = ref.getCol() + 1;
            String base = InputWorkbookXml.address(9, column), lookup = column == firstColumn ? "F" : "G";
            String expected = "INDEX(General!$" + lookup + "$3:$" + lookup + "$86,MATCH(" + InputWorkbookXml.address(9, countryColumn) + ",General!$H$3:$H$86,0))";
            if (!expected.equals(baseFormulas.get(base))) throw invalid();
            expected = InputWorkbookFormula.translate(expected, row - 9, 0);
            String formula = sourceFormulas.get(target), template = raw.get(target);
            if (created.contains(row)) {
                if (formula != null || template != null && java.util.regex.Pattern.compile("<(?:f|v|is)(?=\\s|/?>)").matcher(template).find()) throw invalid();
                if (template == null) template = raw.get(InputWorkbookXml.address(styleRow, column));
                if (template == null) template = InputWorkbookRows.template(baseline, SHEET, 9, checkpoint).get(column);
                if (template == null) throw invalid();
                template = InputWorkbookXml.valueCell(template, target, null).replace("</c>", "<f>" + InputWorkbookXml.escape(expected) + "</f><v>0</v></c>");
                added.add(target); formula = expected;
            }
            if (!expected.equals(formula) || template == null) throw invalid();
            patch.raw(row, column, template); formulas.put(new InputWorkbookCalculation.Address(SHEET, target), formula);
            constants.put(new InputWorkbookCalculation.Address(SHEET, InputWorkbookXml.address(row, countryColumn)), null);
        }
        for (int row = 3; row <= 86; row++) for (int column = 6; column <= 8; column++)
            constants.put(new InputWorkbookCalculation.Address("General", InputWorkbookXml.address(row, column)), null);
        reader.readLocal(source, List.of(
            new InputWorkbookReader.Window("General", row -> row >= 3 && row <= 86, Set.of(6, 7, 8), (row, cells) -> cells.forEach((column, cell) ->
                constants.put(new InputWorkbookCalculation.Address("General", InputWorkbookXml.address(row, column)), cell.raw()))),
            new InputWorkbookReader.Window(SHEET, rows::contains, Set.of(countryColumn), (row, cells) -> cells.forEach((column, cell) ->
                constants.put(new InputWorkbookCalculation.Address(SHEET, InputWorkbookXml.address(row, column)), cell.raw())))));
        patch.edits.forEach((row, cells) -> {
            if (rows.contains(row) && cells.containsKey(countryColumn)) constants.put(new InputWorkbookCalculation.Address(SHEET, InputWorkbookXml.address(row, countryColumn)), cells.get(countryColumn).value());
        });
        InputWorkbookCalculation.evaluate(constants, formulas, checkpoint).forEach((address, value) -> {
            var ref = new CellReference(address.cell()); int row = ref.getRow() + 1, column = ref.getCol() + 1;
            patch.raw(row, column, InputWorkbookCalculation.cache(patch.raw.get(row).get(column), value));
        });
        return new Result(Set.copyOf(targets), List.copyOf(added));
    }

    private static InputDraftPreserveChange.Failure invalid() { return new InputDraftPreserveChange.Failure("STRUCTURE_VALIDATION_FAILED"); }
}
