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

/** 비용 입력에 연결된 선택값·연결키만 계산한다. 옆 순위표와 외부 참조는 보존한다. */
final class InputWorkbookCost {
    record Result(Set<String> calculated, List<String> added) {}
    private static final String SHEET = "CostInput";

    static Result ranking(Path source, Path baseline, int previousEnd, int end, int styleRow, Set<Integer> active, Set<Integer> created, Set<Object> removedYears,
            InputWorkbookXml.SheetPatch patch, InputWorkbookReader reader, InputWorkbookXml.Checkpoint checkpoint) throws Exception {
        Map<Integer, Map<Integer, Object>> values = new LinkedHashMap<>();
        int referenceEnd = Math.max(1000, end), previousReferenceEnd = Math.max(1000, previousEnd);
        reader.readLocal(source, List.of(new InputWorkbookReader.Window(SHEET, row -> row >= 3 && row <= referenceEnd, Set.of(37, 40, 41, 42, 43), (row, cells) -> {
            Map<Integer, Object> data = new HashMap<>(); cells.forEach((column, cell) -> data.put(column, cell.raw())); values.put(row, data);
        })));
        Set<String> years = new HashSet<>(); removedYears.forEach(value -> years.add(year(value)));
        Set<Integer> changed = new HashSet<>(created);
        patch.edits.forEach((row, cells) -> {
            if (active.contains(row) && (cells.containsKey(37) || cells.containsKey(40))) changed.add(row);
        });
        if (changed.isEmpty() && years.isEmpty() && referenceEnd == previousReferenceEnd) return new Result(Set.of(), List.of());
        for (int row : changed) {
            Map<Integer, Object> data = values.computeIfAbsent(row, ignored -> new HashMap<>()); years.add(year(data.get(37)));
            patch.edits.getOrDefault(row, Map.of()).forEach((column, edit) -> { if (column == 37 || column == 40) data.put(column, edit.value()); });
            years.add(year(data.get(37)));
        }
        Set<Integer> affected = new HashSet<>(created);
        for (int row : active) if (years.contains(year(values.getOrDefault(row, Map.of()).get(37)))) affected.add(row);
        // 참조 범위를 넓힐 때는 생존 순위식 모두 같은 현재 범위를 사용한다.
        if (referenceEnd != previousReferenceEnd) affected.addAll(active);
        Set<String> targets = new HashSet<>(); for (int row : affected) for (int column = 41; column <= 44; column++) targets.add(InputWorkbookXml.address(row, column));
        Map<String, String> original = InputWorkbookStructure.formulas(source, SHEET,
            a -> targets.contains(a) || Set.of("AO3", "AP3", "AQ3", "AR3").contains(a), checkpoint);
        Map<String, String> raw = new HashMap<>();
        InputWorkbookXml.rows(source, InputWorkbookXml.sheetParts(source).get(SHEET), checkpoint, row -> {
            if (!affected.contains(row.number()) && row.number() != styleRow) return;
            InputWorkbookXml.cells(row.xml()).forEach((column, cell) -> {
                if (column >= 41 && column <= 44) raw.put(InputWorkbookXml.address(row.number(), column), cell);
            });
        });
        var baseFormulas = InputWorkbookStructure.formulas(baseline, SHEET, Set.of("AO3", "AP3", "AQ3", "AR3")::contains, checkpoint);
        for (int column = 41; column <= 44; column++) {
            String anchor = InputWorkbookXml.address(3, column);
            if (original.containsKey(anchor) ? !rankFormula(3, column, previousReferenceEnd).equals(original.get(anchor))
                    : !rankFormula(3, column, 1000).equals(baseFormulas.get(anchor))) throw invalid();
        }
        Map<InputWorkbookCalculation.Address, String> formulas = new LinkedHashMap<>(); List<String> added = new ArrayList<>();
        for (String target : targets) {
            var ref = new CellReference(target); int row = ref.getRow() + 1, column = ref.getCol() + 1;
            String expected = rankFormula(row, column, referenceEnd), template = raw.get(target), formula = original.get(target);
            if (created.contains(row) && formula == null) {
                if (template != null && java.util.regex.Pattern.compile("<(?:v|is)(?=\\s|/?>)").matcher(template).find()) throw invalid();
                if (template == null) template = raw.get(InputWorkbookXml.address(styleRow, column));
                if (template == null) template = InputWorkbookRows.template(baseline, SHEET, 3, checkpoint).get(column);
                if (template == null) throw invalid();
                template = InputWorkbookXml.valueCell(template, target, null).replace("</c>", "<f>" + InputWorkbookXml.escape(expected) + "</f><v>0</v></c>");
                added.add(target);
            } else {
                if (!rankFormula(row, column, previousReferenceEnd).equals(formula) || template == null) throw invalid();
                if (!expected.equals(formula)) template = InputWorkbookXml.valueCell(template, target, null)
                    .replace("</c>", "<f>" + InputWorkbookXml.escape(expected) + "</f><v>0</v></c>");
            }
            patch.raw(row, column, template); formulas.put(new InputWorkbookCalculation.Address(SHEET, target), expected);
        }
        Map<InputWorkbookCalculation.Address, Object> constants = new LinkedHashMap<>();
        for (int row = 3; row <= referenceEnd; row++) for (int column : List.of(37, 40, 41, 42, 43)) {
            var address = new InputWorkbookCalculation.Address(SHEET, InputWorkbookXml.address(row, column));
            if (!formulas.containsKey(address)) constants.put(address, values.getOrDefault(row, Map.of()).get(column));
        }
        InputWorkbookCalculation.evaluate(constants, formulas, checkpoint).forEach((address, value) -> {
            var ref = new CellReference(address.cell()); int row = ref.getRow() + 1, column = ref.getCol() + 1;
            patch.raw(row, column, InputWorkbookCalculation.cache(patch.raw.get(row).get(column), value));
        });
        return new Result(Set.copyOf(targets), List.copyOf(added));
    }

    private static String year(Object value) {
        // COUNTIFS는 빈 기준 셀을 숫자0으로 비교하므로 두 그룹의 영향을 함께 계산한다.
        return value == null ? "0" : value instanceof java.math.BigDecimal number ? number.stripTrailingZeros().toPlainString() : value.toString();
    }

    private static String rankFormula(int row, int column, int end) {
        return switch (column) {
            case 41 -> "COUNTIFS($AN$3:$AN$" + end + ",\"<\"&AN" + row + ",$AK$3:$AK$" + end + ",AK" + row + ")+1";
            case 42 -> "IF(COUNTIFS($AO$3:$AO$" + end + ",AO" + row + ",$AK$3:$AK$" + end + ",AK" + row + ")>1,\"Repeated\",\"Unique\")";
            case 43 -> "IF(AP" + row + "=\"Repeated\",COUNTIFS($AK$3:$AK" + row + ",AK" + row + ",$AO$3:$AO" + row + ",AO" + row + ")-1+AO" + row + ",AO" + row + ")";
            case 44 -> "AK" + row + "&AQ" + row;
            default -> throw invalid();
        };
    }

    static Result inputs(Path source, Path baseline, int first, int inputLast, int end, int styleRow, Set<Integer> created,
            InputWorkbookXml.SheetPatch patch, InputWorkbookReader reader, InputWorkbookXml.Checkpoint checkpoint) throws Exception {
        Set<String> targets = new HashSet<>();
        for (int row : created) for (int column = inputLast + 1; column <= inputLast + 2; column++) targets.add(InputWorkbookXml.address(row, column));
        patch.edits.forEach((row, cells) -> {
            if (row < 3 || row > end) return;
            if (cells.keySet().stream().anyMatch(c -> c == inputLast || c == inputLast - 1)) targets.add(InputWorkbookXml.address(row, inputLast + 1));
            if (cells.keySet().stream().anyMatch(c -> c >= first && c < inputLast - 1)) targets.add(InputWorkbookXml.address(row, inputLast + 2));
        });
        if (targets.isEmpty()) return new Result(Set.of(), List.of());
        Set<Integer> rows = new HashSet<>(); targets.forEach(a -> rows.add(new CellReference(a).getRow() + 1));
        String selectedBase = InputWorkbookXml.address(3, inputLast + 1), helperBase = InputWorkbookXml.address(3, inputLast + 2);
        Map<String, String> original = InputWorkbookStructure.formulas(source, SHEET,
            a -> targets.contains(a) || a.equals(selectedBase) || a.equals(helperBase), checkpoint);
        String user = "$" + CellReference.convertNumToColString(inputLast - 1), base = "$" + CellReference.convertNumToColString(inputLast - 2);
        String selected = "IF(OR(" + user + "3=\"\"," + user + "3=0)," + base + "3," + user + "3)";
        List<String> keys = new ArrayList<>();
        for (int column = first; column < inputLast - 1; column++) keys.add("$" + CellReference.convertNumToColString(column - 1) + "3");
        String helper = String.join("&", keys);
        var baseFormulas = InputWorkbookStructure.formulas(baseline, SHEET, a -> a.equals(selectedBase) || a.equals(helperBase), checkpoint);
        if (!selected.equals(baseFormulas.get(selectedBase)) || !helper.equals(baseFormulas.get(helperBase))) throw invalid();
        Map<String, String> raw = new HashMap<>();
        InputWorkbookXml.rows(source, InputWorkbookXml.sheetParts(source).get(SHEET), checkpoint, row -> {
            if (!rows.contains(row.number()) && row.number() != styleRow) return;
            InputWorkbookXml.cells(row.xml()).forEach((column, cell) -> {
                if (column > inputLast && column <= inputLast + 2) raw.put(InputWorkbookXml.address(row.number(), column), cell);
            });
        });
        Map<InputWorkbookCalculation.Address, String> formulas = new LinkedHashMap<>();
        Map<InputWorkbookCalculation.Address, Object> constants = new LinkedHashMap<>(); List<String> added = new ArrayList<>();
        for (String target : targets) {
            var ref = new CellReference(target); int row = ref.getRow() + 1, column = ref.getCol() + 1;
            String expected = InputWorkbookFormula.translate(column == inputLast + 1 ? selected : helper, row - 3, 0);
            String formula = original.get(target), template = raw.get(target);
            if (created.contains(row) && formula == null) {
                if (template != null && java.util.regex.Pattern.compile("<(?:v|is)(?=\\s|/?>)").matcher(template).find()) throw invalid();
                if (template == null) template = raw.get(InputWorkbookXml.address(styleRow, column));
                if (template == null) template = InputWorkbookRows.template(baseline, SHEET, 3, checkpoint).get(column);
                if (template == null) throw invalid();
                template = InputWorkbookXml.valueCell(template, target, null).replace("</c>", "<f>" + InputWorkbookXml.escape(expected) + "</f><v>0</v></c>");
                added.add(target); formula = expected;
            }
            if (!expected.equals(formula) || template == null) throw invalid();
            patch.raw(row, column, template); formulas.put(new InputWorkbookCalculation.Address(SHEET, target), formula);
        }
        Set<Integer> inputs = new HashSet<>();
        for (int column = first; column <= inputLast; column++) {
            inputs.add(column);
            for (int row : rows) constants.put(new InputWorkbookCalculation.Address(SHEET, InputWorkbookXml.address(row, column)), null);
        }
        reader.readLocal(source, List.of(new InputWorkbookReader.Window(SHEET, rows::contains, inputs, (row, cells) -> cells.forEach((column, cell) ->
            constants.put(new InputWorkbookCalculation.Address(SHEET, InputWorkbookXml.address(row, column)), cell.raw())))));
        patch.edits.forEach((row, cells) -> {
            if (rows.contains(row)) cells.forEach((column, edit) -> {
                if (inputs.contains(column)) constants.put(new InputWorkbookCalculation.Address(SHEET, InputWorkbookXml.address(row, column)), edit.value());
            });
        });
        InputWorkbookCalculation.evaluate(constants, formulas, checkpoint).forEach((address, value) -> {
            var ref = new CellReference(address.cell()); int row = ref.getRow() + 1, column = ref.getCol() + 1;
            patch.raw(row, column, InputWorkbookCalculation.cache(patch.raw.get(row).get(column), value));
        });
        return new Result(Set.copyOf(targets), List.copyOf(added));
    }

    private static InputDraftPreserveChange.Failure invalid() { return new InputDraftPreserveChange.Failure("STRUCTURE_VALIDATION_FAILED"); }
}
