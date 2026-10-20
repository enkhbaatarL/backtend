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

/** ProductionAdjustment의 정상 Helper/입력 확인 수식만 계산한다. 기존 G/L 오류는 보존한다. */
final class InputWorkbookProduction {
    record Result(Set<String> calculated, List<String> added) {}
    private static final String SHEET = "ProductionAdjustment";
    private static final Map<Integer, Set<Integer>> INPUTS = Map.of(8, Set.of(9, 10, 11), 14, Set.of(13), 15, Set.of(9, 10, 11, 13));

    /** 기준 생산량의 A 순번/B 연간 개월수/G 연결키만 처리한다. N:T 참조표/피벗은 보존한다. */
    static Result baseline(Path source, int styleRow, int end, Set<Integer> created,
            InputWorkbookXml.SheetPatch patch, InputWorkbookReader reader, InputWorkbookXml.Checkpoint checkpoint) throws Exception {
        String sheet = "BCProduction";
        Map<Integer, Set<Integer>> inputs = Map.of(2, Set.of(3), 7, Set.of(5, 6));
        Set<String> targets = new java.util.TreeSet<>();
        patch.edits.forEach((row, cells) -> {
            if (row < 8 || row > end) return;
            inputs.forEach((column, precedents) -> {
                if (created.contains(row) || precedents.stream().anyMatch(cells::containsKey)) targets.add(InputWorkbookXml.address(row, column));
            });
        });
        created.forEach(row -> targets.add("A" + row));
        if (targets.isEmpty()) return new Result(Set.of(), List.of());
        Set<String> bases = Set.of("A9", "B8", "G8");
        Map<String, String> sourceFormulas = InputWorkbookStructure.formulas(source, sheet,
            address -> targets.contains(address) || bases.contains(address), checkpoint);
        if (!"A8+1".equals(sourceFormulas.get("A9")) || !"IF(C8=2017,1,IF(C8=2050,1,12))".equals(sourceFormulas.get("B8"))
                || !"LEFT(F8,LEN(F8)-4)&\", \"&E8".equals(sourceFormulas.get("G8"))) throw invalid();
        Map<String, String> raw = new HashMap<>();
        InputWorkbookXml.rows(source, InputWorkbookXml.sheetParts(source).get(sheet), checkpoint, row -> InputWorkbookXml.cells(row.xml()).forEach((column, cell) -> {
            String address = InputWorkbookXml.address(row.number(), column);
            if (targets.contains(address) || row.number() == styleRow && Set.of(1, 2, 7).contains(column)) raw.put(address, cell);
        }));
        Map<InputWorkbookCalculation.Address, String> formulas = new LinkedHashMap<>();
        Map<InputWorkbookCalculation.Address, Object> constants = new LinkedHashMap<>(); List<String> added = new ArrayList<>();
        for (String address : targets) {
            var ref = new CellReference(address); int row = ref.getRow() + 1, column = ref.getCol() + 1;
            String formula = sourceFormulas.get(address), template = raw.get(address);
            if (created.contains(row)) {
                if (formula != null || template != null && java.util.regex.Pattern.compile("<(?:f|v|is)(?=\\s|/?>)").matcher(template).find()) throw invalid();
                String base = column == 1 ? "A9" : column == 2 ? "B8" : "G8";
                formula = InputWorkbookFormula.translate(sourceFormulas.get(base), row - (column == 1 ? 9 : 8), 0);
                template = formulaCell(template == null ? raw.get(InputWorkbookXml.address(styleRow, column)) : template, address, formula); added.add(address);
            }
            if (formula == null || template == null) throw invalid();
            formulas.put(new InputWorkbookCalculation.Address(sheet, address), formula); patch.raw(row, column, template);
            if (column == 1) constants.put(new InputWorkbookCalculation.Address(sheet, "A" + (row - 1)), null);
            else for (int input : inputs.get(column)) constants.put(new InputWorkbookCalculation.Address(sheet, InputWorkbookXml.address(row, input)), null);
        }
        formulas.keySet().forEach(constants::remove);
        Set<Integer> inputRows = new HashSet<>(), inputColumns = new HashSet<>();
        constants.keySet().forEach(address -> { var ref = new CellReference(address.cell()); inputRows.add(ref.getRow() + 1); inputColumns.add(ref.getCol() + 1); });
        reader.readLocal(source, List.of(new InputWorkbookReader.Window(sheet, inputRows::contains, inputColumns, (row, cells) -> cells.forEach((column, cell) -> {
            var address = new InputWorkbookCalculation.Address(sheet, InputWorkbookXml.address(row, column));
            if (constants.containsKey(address)) constants.put(address, cell.raw());
        }))));
        patch.edits.forEach((row, cells) -> cells.forEach((column, edit) -> {
            var address = new InputWorkbookCalculation.Address(sheet, InputWorkbookXml.address(row, column));
            if (constants.containsKey(address)) constants.put(address, edit.value());
        }));
        InputWorkbookCalculation.evaluate(constants, formulas, checkpoint).forEach((address, value) -> {
            var ref = new CellReference(address.cell()); int row = ref.getRow() + 1, column = ref.getCol() + 1;
            patch.raw(row, column, InputWorkbookCalculation.cache(patch.raw.get(row).get(column), value));
        });
        return new Result(Set.copyOf(targets), List.copyOf(added));
    }

    static Result adjustment(Path source, int previousEnd, int end, Set<Integer> created,
            InputWorkbookXml.SheetPatch patch, InputWorkbookReader reader, InputWorkbookXml.Checkpoint checkpoint) throws Exception {
        Set<String> targets = new HashSet<>();
        patch.edits.forEach((row, cells) -> {
            if (row < 8 || row > end) return;
            INPUTS.forEach((column, inputs) -> {
                if (created.contains(row) || inputs.stream().anyMatch(cells::containsKey)) targets.add(InputWorkbookXml.address(row, column));
            });
        });
        if (targets.isEmpty()) return new Result(Set.of(), List.of());
        boolean total = !created.isEmpty() || targets.stream().anyMatch(address -> address.startsWith("O"));
        if (total) targets.add("M3");
        Map<String, String> sourceFormulas = InputWorkbookStructure.formulas(source, SHEET,
            address -> targets.contains(address) || Set.of("H8", "N8", "O8").contains(address), checkpoint);
        Map<String, String> raw = new HashMap<>();
        InputWorkbookXml.rows(source, InputWorkbookXml.sheetParts(source).get(SHEET), checkpoint, row -> InputWorkbookXml.cells(row.xml()).forEach((column, cell) -> {
            String address = InputWorkbookXml.address(row.number(), column);
            if (targets.contains(address) || Set.of("H8", "N8", "O8").contains(address)) raw.put(address, cell);
        }));
        Map<InputWorkbookCalculation.Address, String> formulas = new LinkedHashMap<>();
        Map<InputWorkbookCalculation.Address, Object> constants = new LinkedHashMap<>();
        List<String> added = new ArrayList<>();
        for (String address : targets) {
            var ref = new CellReference(address); int row = ref.getRow() + 1, column = ref.getCol() + 1;
            String formula = sourceFormulas.get(address), template = raw.get(address);
            if (row == 3) {
                String expected = (2 * (previousEnd - 7)) + "-(COUNTIFS(G8:G" + previousEnd + ",\" \")+COUNTIFS(O8:O" + previousEnd + ",\"\"))";
                if (!expected.equals(formula)) throw invalid();
                if (end != previousEnd) {
                    formula = (2 * (end - 7)) + "-(COUNTIFS(G8:G" + end + ",\" \")+COUNTIFS(O8:O" + end + ",\"\"))";
                    template = formulaCell(template, address, formula);
                }
            } else if (created.contains(row)) {
                String base = InputWorkbookXml.address(8, column);
                if (sourceFormulas.get(base) == null) throw invalid();
                String expected = InputWorkbookFormula.translate(sourceFormulas.get(base), row - 8, 0);
                if (formula != null) { if (!expected.equals(formula)) throw invalid(); }
                else {
                    if (template != null && java.util.regex.Pattern.compile("<(?:f|v|is)(?=\\s|/?>)").matcher(template).find()) throw invalid();
                    template = formulaCell(template == null ? raw.get(base) : template, address, expected); added.add(address);
                }
                formula = expected;
            }
            if (formula == null || template == null) throw invalid();
            formulas.put(new InputWorkbookCalculation.Address(SHEET, address), formula);
            patch.raw(row, column, template);
            if (row >= 8) for (int input : INPUTS.get(column)) constants.put(new InputWorkbookCalculation.Address(SHEET, InputWorkbookXml.address(row, input)), null);
        }
        if (total) for (int row = 8; row <= end; row++) for (int column : List.of(7, 15)) {
            var address = new InputWorkbookCalculation.Address(SHEET, InputWorkbookXml.address(row, column));
            if (!formulas.containsKey(address)) constants.put(address, null);
        }
        Set<Integer> inputRows = new HashSet<>(), inputColumns = new HashSet<>();
        constants.keySet().forEach(address -> { var ref = new CellReference(address.cell()); inputRows.add(ref.getRow() + 1); inputColumns.add(ref.getCol() + 1); });
        reader.readLocal(source, List.of(new InputWorkbookReader.Window(SHEET, inputRows::contains, inputColumns, (row, cells) -> cells.forEach((column, cell) -> {
            var address = new InputWorkbookCalculation.Address(SHEET, InputWorkbookXml.address(row, column));
            if (constants.containsKey(address)) constants.put(address, cell.raw());
        }))));
        patch.edits.forEach((row, cells) -> cells.forEach((column, edit) -> {
            var address = new InputWorkbookCalculation.Address(SHEET, InputWorkbookXml.address(row, column));
            if (constants.containsKey(address)) constants.put(address, edit.value());
        }));
        InputWorkbookCalculation.evaluate(constants, formulas, checkpoint).forEach((address, value) -> {
            var ref = new CellReference(address.cell()); int row = ref.getRow() + 1, column = ref.getCol() + 1;
            patch.raw(row, column, InputWorkbookCalculation.cache(patch.raw.get(row).get(column), value));
        });
        return new Result(Set.copyOf(targets), List.copyOf(added));
    }

    private static String formulaCell(String template, String address, String formula) {
        if (template == null) throw invalid();
        return InputWorkbookXml.valueCell(template, address, null).replace("</c>", "<f>" + InputWorkbookXml.escape(formula) + "</f><v>0</v></c>");
    }
    private static InputDraftPreserveChange.Failure invalid() { return new InputDraftPreserveChange.Failure("STRUCTURE_VALIDATION_FAILED"); }
}
