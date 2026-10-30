package com.sk.domain.mi.service.fundamentalmodel.input;

import com.sk.domain.shared.repository.master.tmm.ModelInputDraftRepository.TableRef;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.poi.ss.util.CellReference;

/** Regas 직접 월 입력에 연결된 연간/국가 합계. 원본의 미변경 설비 수식은 보존 cache로 읽는다. */
final class InputWorkbookRegas {
    /** 기존 master의 입력에 직접 연결된 A/B:F/DM만 계산한다. 월별 용량 계산보다 먼저 반영한다. */
    static Map<InputWorkbookCalculation.Address, Object> masterInputLinks(Path current, Map<Integer, Set<Integer>> changed,
            Map<String, InputWorkbookXml.SheetPatch> patches, InputWorkbookReader reader, InputWorkbookXml.Checkpoint checkpoint) throws Exception {
        String master = "BCRegasification", monthly = "RegasAdjustment";
        Set<String> masterTargets = new HashSet<>(), monthlyTargets = new HashSet<>();
        changed.forEach((row, columns) -> {
            if (columns.contains(5) || columns.contains(6)) masterTargets.add("A" + row);
            for (int column : columns) if (column >= 2 && column <= 6) monthlyTargets.add(InputWorkbookXml.address(row + 3, column));
            if (columns.contains(4)) monthlyTargets.add("DM" + (row + 3));
        });
        Map<InputWorkbookCalculation.Address, String> formulas = new LinkedHashMap<>();
        Map<InputWorkbookCalculation.Address, String> templates = new HashMap<>();
        for (String sheet : List.of(master, monthly)) {
            Set<String> targets = sheet.equals(master) ? masterTargets : monthlyTargets;
            if (targets.isEmpty()) continue;
            var selected = InputWorkbookStructure.formulas(current, sheet, targets::contains, checkpoint);
            if (selected.size() != targets.size()) throw invalid();
            selected.forEach((address, formula) -> {
                var ref = new CellReference(address); int row = ref.getRow() + 1, column = ref.getCol() + 1;
                String expected = sheet.equals(master) ? "E" + row + "&F" + row
                    : column == 117 ? "D" + row : master + "!" + InputWorkbookXml.address(row - 3, column);
                if (!expected.equals(formula.replace("'BCRegasification'!", "BCRegasification!"))) throw invalid();
                formulas.put(new InputWorkbookCalculation.Address(sheet, address), formula);
            });
            InputWorkbookXml.rows(current, InputWorkbookXml.sheetParts(current).get(sheet), checkpoint, row -> InputWorkbookXml.cells(row.xml()).forEach((column, cell) -> {
                String address = InputWorkbookXml.address(row.number(), column);
                if (targets.contains(address)) templates.put(new InputWorkbookCalculation.Address(sheet, address), cell);
            }));
        }
        if (formulas.isEmpty()) return Map.of();
        Map<InputWorkbookCalculation.Address, Object> constants = new LinkedHashMap<>();
        for (int row : changed.keySet()) for (int column = 2; column <= 6; column++)
            constants.put(new InputWorkbookCalculation.Address(master, InputWorkbookXml.address(row, column)), null);
        reader.readLocal(current, List.of(new InputWorkbookReader.Window(master, changed::containsKey, Set.of(2, 3, 4, 5, 6), (row, cells) -> cells.forEach((column, cell) ->
            constants.put(new InputWorkbookCalculation.Address(master, InputWorkbookXml.address(row, column)), cell.raw())))));
        var calculated = InputWorkbookCalculation.evaluate(constants, formulas, checkpoint);
        calculated.forEach((address, value) -> {
            var ref = new CellReference(address.cell());
            patches.get(address.sheet()).raw(ref.getRow() + 1, ref.getCol() + 1, InputWorkbookCalculation.cache(templates.get(address), value));
        });
        return calculated;
    }

    static List<String> masterRows(Path source, Path baseline, TableRef master, int styleRow, int maximum, Set<Integer> rows,
            InputWorkbookXml.SheetPatch patch, InputWorkbookReader reader, InputWorkbookXml.Checkpoint checkpoint) throws Exception {
        String sheet = master.sheet(); Map<Integer, Map<Integer, String>> raw = rawRows(source, sheet, rows, styleRow, checkpoint);
        Map<Integer, String> template = raw.getOrDefault(styleRow, Map.of());
        if (!template.containsKey(1) || !template.containsKey(master.endColumn())) template = rawRows(baseline, sheet, Set.of(), 8, checkpoint).get(8);
        if (template == null) throw invalid();
        Map<InputWorkbookCalculation.Address, Object> constants = new LinkedHashMap<>();
        Map<InputWorkbookCalculation.Address, String> formulas = new LinkedHashMap<>();
        List<String> created = new java.util.ArrayList<>();
        Map<String, String> sourceHelpers = InputWorkbookStructure.formulas(source, sheet, address -> {
            var ref = new CellReference(address); return ref.getCol() == 0 && rows.contains(ref.getRow() + 1);
        }, checkpoint);
        for (int row : rows) {
            var values = patch.edits.getOrDefault(row, Map.of());
            for (int column : List.of(5, 6)) {
                if (!values.containsKey(column)) throw invalid();
                constants.put(new InputWorkbookCalculation.Address(sheet, InputWorkbookXml.address(row, column)), values.get(column).value());
            }
            String helper = "E" + row + "&F" + row;
            formulas.put(new InputWorkbookCalculation.Address(sheet, "A" + row), helper);
            String style = raw.getOrDefault(row, Map.of()).getOrDefault(1, template.get(1));
            String existing = patch.raw.getOrDefault(row, Map.of()).getOrDefault(1, raw.getOrDefault(row, Map.of()).get(1));
            if (existing != null && InputWorkbookXml.hasFormula(existing)) {
                if (!helper.equals(sourceHelpers.get("A" + row))) throw invalid();
                patch.raw(row, 1, existing);
            } else { patch.raw(row, 1, formulaCell(style, "A" + row, helper)); created.add("A" + row); }
            String destination = patch.raw.getOrDefault(row, Map.of()).getOrDefault(maximum, raw.getOrDefault(row, Map.of()).get(maximum));
            if (destination != null && java.util.regex.Pattern.compile("<(?:f|v|is)(?=\\s|/?>)").matcher(destination).find()) throw invalid();
            style = destination == null ? template.get(master.endColumn()) : destination;
            patch.raw(row, maximum, formulaCell(style, InputWorkbookXml.address(row, maximum), "MAX(G" + row + ":" + CellReference.convertNumToColString(maximum - 2) + row + ")"));
            created.add(InputWorkbookXml.address(row, maximum));
        }
        var calculated = InputWorkbookCalculation.evaluate(constants, formulas, checkpoint);
        calculated.forEach((address, value) -> {
            int row = new CellReference(address.cell()).getRow() + 1;
            patch.raw(row, 1, InputWorkbookCalculation.cache(patch.raw.get(row).get(1), value));
        });
        return List.copyOf(created);
    }

    static Map<InputWorkbookCalculation.Address, Object> monthlyRows(Path source, Path baseline, Path current, TableRef previousCountries, int maximum,
            int styleRow, Set<Integer> rows, InputWorkbookXml.SheetPatch patch, InputWorkbookReader reader,
            InputWorkbookXml.Checkpoint checkpoint) throws Exception {
        return monthlyRows(source,baseline,current,previousCountries,maximum,styleRow,rows,patch,reader,checkpoint,Set.of());
    }
    static Map<InputWorkbookCalculation.Address,Object> monthlyRows(Path source,Path baseline,Path current,TableRef previousCountries,int maximum,
            int styleRow,Set<Integer> rows,InputWorkbookXml.SheetPatch patch,InputWorkbookReader reader,InputWorkbookXml.Checkpoint checkpoint,Set<Integer> restored) throws Exception {
        String sheet = "RegasAdjustment", master = "BCRegasification";
        Path templatesFrom = source; int templateRow = styleRow;
        Map<String, String> selected = rowFormulas(source, sheet, templateRow, checkpoint);
        if (selected.size() != 100) { templatesFrom = baseline; templateRow = 11; selected = rowFormulas(baseline, sheet, templateRow, checkpoint); }
        if (selected.size() != 100) throw invalid();
        Map<Integer, String> templates = rawRows(templatesFrom, sheet, Set.of(), templateRow, checkpoint).get(templateRow);
        Map<Integer, Map<Integer, String>> destinations = rawRows(source, sheet, rows, styleRow, checkpoint);
        Map<InputWorkbookCalculation.Address, Object> constants = new LinkedHashMap<>();
        Set<Integer> masterColumns = new HashSet<>(); for (int column = 2; column <= maximum; column++) masterColumns.add(column);
        Set<Integer> monthlyColumns = new HashSet<>(); for (int column = 8; column <= 126; column++) monthlyColumns.add(column);
        reader.readLocal(current, List.of(
            new InputWorkbookReader.Window(master, row -> true, masterColumns, (row, cells) -> cells.forEach((column, cell) -> constants.put(new InputWorkbookCalculation.Address(master, InputWorkbookXml.address(row, column)), cell.raw()))),
            new InputWorkbookReader.Window(sheet, row -> row == 9 || row == 10, monthlyColumns, (row, cells) -> cells.forEach((column, cell) -> {
                if (row == 9 && column <= 115 || row == 10 && column >= 118) constants.put(new InputWorkbookCalculation.Address(sheet, InputWorkbookXml.address(row, column)), cell.raw());
            }))));
        Map<InputWorkbookCalculation.Address, String> formulas = new LinkedHashMap<>();
        Map<String,String> restoredLinks=restored.isEmpty()?Map.of():InputWorkbookStructure.formulas(current,sheet,a->{var ref=new CellReference(a);return restored.contains(ref.getRow()+1)&&(ref.getCol()+1>=2&&ref.getCol()+1<=6||ref.getCol()+1==117);},checkpoint);
        for (int row : rows) {
            boolean reclaimed = row >= previousCountries.startRow() && row <= previousCountries.endRow();
            for (int column = 8; column <= 31; column++) {
                var originalInput=patch.edits.getOrDefault(row,Map.of()).get(column);
                if(restored.contains(row)&&originalInput==null)throw invalid();
                constants.put(new InputWorkbookCalculation.Address(sheet, InputWorkbookXml.address(row, column)), restored.contains(row)?originalInput.value():null);
                if (reclaimed) {
                    var edit = patch.edits.getOrDefault(row, Map.of()).get(column);
                    if (edit == null || templates.get(column) == null) throw invalid();
                    patch.put(row, column, new InputWorkbookXml.Edit(edit.value(), templates.get(column), edit.replaceFormula()));
                }
            }
            for (var formula : selected.entrySet()) {
                int column = new CellReference(formula.getKey()).getCol() + 1; String address = InputWorkbookXml.address(row, column);
                String destination = patch.raw.getOrDefault(row, Map.of()).getOrDefault(column, destinations.getOrDefault(row, Map.of()).get(column));
                String text = InputWorkbookFormula.translate(formula.getValue(), row - templateRow, 0);
                if (destination != null && java.util.regex.Pattern.compile("<(?:f|v|is)(?=\\s|/?>)").matcher(destination).find()
                    && !(restored.contains(row)&&(column>=2&&column<=6||column==117)&&text.equals(restoredLinks.get(address)))) throw invalid();
                formulas.put(new InputWorkbookCalculation.Address(sheet, address), text);
                patch.raw(row, column, formulaCell(destination == null || reclaimed && column >= 7 && column <= 115 ? templates.get(column) : destination, address, text));
            }
        }
        List<InputWorkbookCalculation.KnownRange> ranges = new java.util.ArrayList<>();
        ranges.add(new InputWorkbookCalculation.KnownRange(master, new InputWorkbookStructure.Area(1, 1048576, 2, maximum)));
        ranges.add(new InputWorkbookCalculation.KnownRange(sheet, new InputWorkbookStructure.Area(9, 9, 8, 115)));
        ranges.add(new InputWorkbookCalculation.KnownRange(sheet, new InputWorkbookStructure.Area(10, 10, 118, 126)));
        for (int row : rows) ranges.add(new InputWorkbookCalculation.KnownRange(sheet, new InputWorkbookStructure.Area(row, row, 8, 115)));
        var calculated = InputWorkbookCalculation.evaluate(constants, formulas, ranges, checkpoint);
        calculated.forEach((address, value) -> {
            var ref = new CellReference(address.cell()); int row = ref.getRow() + 1, column = ref.getCol() + 1;
            patch.raw(row, column, InputWorkbookCalculation.cache(patch.raw.get(row).get(column), value));
        });
        return calculated;
    }

    private static Map<String, String> rowFormulas(Path file, String sheet, int row, InputWorkbookXml.Checkpoint checkpoint) throws Exception {
        return InputWorkbookStructure.formulas(file, sheet, address -> {
            var ref = new CellReference(address); int column = ref.getCol() + 1;
            return ref.getRow() + 1 == row && (column >= 2 && column <= 7 || column >= 32 && column <= 115 || column >= 117 && column <= 126);
        }, checkpoint);
    }

    private static Map<Integer, Map<Integer, String>> rawRows(Path file, String sheet, Set<Integer> rows, int styleRow, InputWorkbookXml.Checkpoint checkpoint) throws Exception {
        Map<Integer, Map<Integer, String>> result = new HashMap<>();
        InputWorkbookXml.rows(file, InputWorkbookXml.sheetParts(file).get(sheet), checkpoint, row -> {
            if (row.number() == styleRow || rows.contains(row.number())) result.put(row.number(), InputWorkbookXml.cells(row.xml()));
        });
        return result;
    }

    private static String formulaCell(String template, String address, String formula) {
        if (template == null) throw invalid();
        return InputWorkbookXml.valueCell(template, address, null).replace("</c>", "<f>" + InputWorkbookXml.escape(formula) + "</f><v>0</v></c>");
    }

    /** 첫 일치 대상이 같은 원본 설비인지 비교한다. 이름 비교도 Excel MATCH 의미를 따른다. */
    static Set<Integer> changedLookupRows(Path source, Path current, Set<Integer> activeRows,
            Map<Integer, Integer> monthlySources, Map<Integer, Integer> masterSources, InputWorkbookReader reader,
            InputWorkbookXml.Checkpoint checkpoint) throws Exception {
        return changedLookupRows(source, current, activeRows, monthlySources, masterSources, reader, checkpoint, Set.of(), false);
    }

    static Set<Integer> changedLookupRows(Path source, Path current, Set<Integer> activeRows,
            Map<Integer, Integer> monthlySources, Map<Integer, Integer> masterSources, InputWorkbookReader reader,
            InputWorkbookXml.Checkpoint checkpoint, Set<Integer> changedCapacities, boolean currentNames) throws Exception {
        Map<InputWorkbookCalculation.Address, Object> constants = new LinkedHashMap<>();
        Map<Integer, Object> names = new HashMap<>();
        reader.readLocal(source, List.of(
            new InputWorkbookReader.Window("BCRegasification", row -> true, Set.of(5), (row, cells) -> {
                if (cells.containsKey(5)) constants.put(new InputWorkbookCalculation.Address("Before", "E" + row), cells.get(5).raw());
            }),
            new InputWorkbookReader.Window("RegasAdjustment", row -> monthlySources.containsValue(row), Set.of(5), (row, cells) -> {
                if (cells.containsKey(5)) names.put(row, cells.get(5).raw());
            })));
        reader.readLocal(current, List.of(new InputWorkbookReader.Window("BCRegasification", row -> true, Set.of(5), (row, cells) -> {
            if (cells.containsKey(5)) constants.put(new InputWorkbookCalculation.Address("After", "E" + row), cells.get(5).raw());
        })));
        Map<Integer, Object> afterNames = new HashMap<>();
        if (currentNames) reader.readLocal(current, List.of(new InputWorkbookReader.Window("RegasAdjustment", activeRows::contains, Set.of(5), (row, cells) -> {
            if (cells.containsKey(5)) afterNames.put(row, cells.get(5).raw());
        })));
        Map<InputWorkbookCalculation.Address, String> formulas = new LinkedHashMap<>();
        for (int row : activeRows) {
            Integer original = monthlySources.get(row); if (original == null) continue;
            if (!names.containsKey(original)) throw invalid();
            constants.put(new InputWorkbookCalculation.Address("Lookup", "E" + row), names.get(original));
            constants.put(new InputWorkbookCalculation.Address("Lookup", "F" + row), currentNames ? afterNames.get(row) : names.get(original));
            formulas.put(new InputWorkbookCalculation.Address("Lookup", "A" + row), "IFERROR(MATCH(E" + row + ",Before!E:E,0),0)");
            formulas.put(new InputWorkbookCalculation.Address("Lookup", "B" + row), "IFERROR(MATCH(F" + row + ",After!E:E,0),0)");
        }
        var matches = InputWorkbookCalculation.evaluate(constants, formulas, List.of(
            new InputWorkbookCalculation.KnownRange("Before", new InputWorkbookStructure.Area(1, 1048576, 5, 5)),
            new InputWorkbookCalculation.KnownRange("After", new InputWorkbookStructure.Area(1, 1048576, 5, 5))), checkpoint);
        Set<Integer> changed = new HashSet<>();
        for (int row : activeRows) if (monthlySources.get(row) != null) {
            int before = ((java.math.BigDecimal) matches.get(new InputWorkbookCalculation.Address("Lookup", "A" + row))).intValueExact();
            int after = ((java.math.BigDecimal) matches.get(new InputWorkbookCalculation.Address("Lookup", "B" + row))).intValueExact();
            if (!java.util.Objects.equals(before, masterSources.getOrDefault(after, after)) || changedCapacities.contains(after)) changed.add(row);
        }
        return Set.copyOf(changed);
    }

    /** 삭제로 lookup 대상이 바뀐 생존 행의 용량·미래 월·연간 합계만 계산한다. 직접 입력은 보존한다. */
    static Map<InputWorkbookCalculation.Address, Object> changedLookups(Path current, int maximum, Set<Integer> rows,
            InputWorkbookXml.SheetPatch patch, InputWorkbookReader reader, InputWorkbookXml.Checkpoint checkpoint) throws Exception {
        if (rows.isEmpty()) return Map.of();
        String sheet = "RegasAdjustment", master = "BCRegasification";
        Map<InputWorkbookCalculation.Address, String> formulas = new LinkedHashMap<>();
        InputWorkbookStructure.formulas(current, sheet, address -> {
            var ref = new CellReference(address); int column = ref.getCol() + 1;
            return rows.contains(ref.getRow() + 1) && (column == 7 || column >= 32 && column <= 115 || column >= 118 && column <= 126);
        }, checkpoint).forEach((address, formula) -> formulas.put(new InputWorkbookCalculation.Address(sheet, address), formula));
        if (formulas.size() != rows.size() * 94) throw invalid();
        Map<InputWorkbookCalculation.Address, Object> constants = new LinkedHashMap<>();
        Set<Integer> masterColumns = new HashSet<>(); for (int column = 2; column <= maximum; column++) masterColumns.add(column);
        Set<Integer> monthlyColumns = new HashSet<>(); for (int column = 2; column <= 126; column++) monthlyColumns.add(column);
        reader.readLocal(current, List.of(
            new InputWorkbookReader.Window(master, row -> true, masterColumns, (row, cells) -> cells.forEach((column, cell) -> constants.put(new InputWorkbookCalculation.Address(master, InputWorkbookXml.address(row, column)), cell.raw()))),
            new InputWorkbookReader.Window(sheet, row -> row == 9 || row == 10 || rows.contains(row), monthlyColumns, (row, cells) -> cells.forEach((column, cell) -> {
                var address = new InputWorkbookCalculation.Address(sheet, InputWorkbookXml.address(row, column));
                if (!formulas.containsKey(address)) constants.put(address, cell.raw());
            }))));
        List<InputWorkbookCalculation.KnownRange> ranges = new java.util.ArrayList<>();
        ranges.add(new InputWorkbookCalculation.KnownRange(master, new InputWorkbookStructure.Area(1, 1048576, 2, maximum)));
        ranges.add(new InputWorkbookCalculation.KnownRange(sheet, new InputWorkbookStructure.Area(9, 10, 8, 126)));
        for (int row : rows) ranges.add(new InputWorkbookCalculation.KnownRange(sheet, new InputWorkbookStructure.Area(row, row, 8, 115)));
        var calculated = InputWorkbookCalculation.evaluate(constants, formulas, ranges, checkpoint);
        var raw = rawRows(current, sheet, rows, 0, checkpoint);
        calculated.forEach((address, value) -> {
            var ref = new CellReference(address.cell()); int row = ref.getRow() + 1, column = ref.getCol() + 1;
            patch.raw(row, column, InputWorkbookCalculation.cache(raw.get(row).get(column), value));
        });
        return calculated;
    }

    /** 미변경 생존 설비의 cache를 보존하고 국가 합계는 현재 행 집합으로 다시 계산한다. */
    static Set<String> countriesAfterRows(Path source, TableRef monthly, TableRef countries, Set<Integer> activeRows,
            Map<Integer, Integer> sourceRows, int countryShift, Map<InputWorkbookCalculation.Address, Object> generated, InputWorkbookXml.SheetPatch patch,
            InputWorkbookReader reader, InputWorkbookXml.Checkpoint checkpoint) throws Exception {
        String sheet = monthly.sheet();
        int last = activeRows.stream().mapToInt(Integer::intValue).max().orElse(monthly.dataStart());
        if (countries.startRow() + countryShift <= last || countries.endColumn() != 115) throw invalid();
        Map<Integer, Map<Integer, InputWorkbookReader.Cell>> values = new HashMap<>();
        Set<Integer> columns = new HashSet<>(); columns.add(4); columns.add(7);
        for (int column = 8; column <= 115; column++) columns.add(column);
        reader.readLocal(source, List.of(new InputWorkbookReader.Window(sheet,
            row -> row >= monthly.dataStart() && row <= monthly.endRow() || row >= countries.dataStart() && row <= countries.endRow(), columns, values::put)));
        Map<InputWorkbookCalculation.Address, Object> constants = new LinkedHashMap<>();
        for (int row : activeRows) {
            Integer original = sourceRows.get(row);
            if (original == null) continue;
            values.getOrDefault(original, Map.of()).forEach((column, cell) -> {
                if (column == 4 || column >= 8) constants.put(new InputWorkbookCalculation.Address(sheet, InputWorkbookXml.address(row, column)), cell.raw());
            });
        }
        generated.forEach((address, value) -> {
            var ref = new CellReference(address.cell()); int column = ref.getCol() + 1;
            if (column == 4 || column >= 8 && column <= 115) constants.put(address, value);
        });
        patch.edits.forEach((row, cells) -> {
            if (activeRows.contains(row)) cells.forEach((column, edit) -> {
                if (column == 4 || column >= 8 && column <= 115) constants.put(new InputWorkbookCalculation.Address(sheet, InputWorkbookXml.address(row, column)), edit.value());
            });
        });
        Map<String, String> selected = InputWorkbookStructure.formulas(source, sheet, address -> {
            var ref = new CellReference(address);
            return ref.getRow() + 1 >= countries.dataStart() && ref.getRow() + 1 <= countries.endRow() && ref.getCol() >= 7 && ref.getCol() <= 114;
        }, checkpoint);
        if (selected.size() != (countries.endRow() - countries.dataStart() + 1) * 108) throw invalid();
        Map<InputWorkbookCalculation.Address, String> formulas = new LinkedHashMap<>();
        Map<String, String> templates = new HashMap<>();
        InputWorkbookXml.rows(source, InputWorkbookXml.sheetParts(source).get(sheet), checkpoint, row -> {
            if (row.number() >= countries.dataStart() && row.number() <= countries.endRow()) InputWorkbookXml.cells(row.xml()).forEach((column, cell) -> templates.put(InputWorkbookXml.address(row.number(), column), cell));
        });
        for (int row = countries.dataStart(); row <= countries.endRow(); row++) {
            var label = values.getOrDefault(row, Map.of()).get(7); if (label == null) throw invalid();
            constants.put(new InputWorkbookCalculation.Address(sheet, InputWorkbookXml.address(row + countryShift, 7)), label.raw());
            for (int column = 8; column <= 115; column++) {
                String oldAddress = InputWorkbookXml.address(row, column), address = InputWorkbookXml.address(row + countryShift, column);
                String letter = CellReference.convertNumToColString(column - 1);
                String oldFormula = countryFormula(letter, monthly.dataStart(), monthly.endRow(), row);
                if (!oldFormula.equals(selected.get(oldAddress))) throw invalid();
                String formula = countryFormula(letter, monthly.dataStart(), last, row + countryShift);
                formulas.put(new InputWorkbookCalculation.Address(sheet, address), formula);
                String raw = patch.raw.getOrDefault(row + countryShift, Map.of()).getOrDefault(column, templates.get(oldAddress));
                raw = InputWorkbookXml.replaceAttribute(raw, "r", address);
                patch.raw(row + countryShift, column, raw.replaceFirst("<f(?=\\s|/?>)[^>]*?(?:/>|>.*?</f>)", java.util.regex.Matcher.quoteReplacement("<f>" + InputWorkbookXml.escape(formula) + "</f>")));
            }
        }
        var calculated = InputWorkbookCalculation.evaluate(constants, formulas,
            List.of(new InputWorkbookCalculation.KnownRange(sheet, new InputWorkbookStructure.Area(monthly.dataStart(), last, 4, 4)),
                new InputWorkbookCalculation.KnownRange(sheet, new InputWorkbookStructure.Area(monthly.dataStart(), last, 8, 115))), checkpoint);
        for (var entry : calculated.entrySet()) {
            var ref = new CellReference(entry.getKey().cell()); int row = ref.getRow() + 1, column = ref.getCol() + 1;
            patch.raw(row, column, InputWorkbookCalculation.cache(patch.raw.get(row).get(column), entry.getValue()));
        }
        return formulas.keySet().stream().map(InputWorkbookCalculation.Address::cell).collect(java.util.stream.Collectors.toSet());
    }

    private static String countryFormula(String column, int first, int last, int row) {
        return "SUMIFS(" + column + "$" + first + ":" + column + "$" + last + ",$D$" + first + ":$D$" + last + ",$G" + row + ")";
    }

    /** 이름/계산 cache 대신 고정 원본의 B:F 연결 수식과 master 활성 행을 대조한다. */
    static Set<Integer> linkedSourceRows(Path source, TableRef master, TableRef monthly, Set<Integer> masterRows,
            InputWorkbookXml.Checkpoint checkpoint) throws Exception {
        if (!master.role().equals("SUPPLY") || !master.sheet().equals("BCRegasification") || master.dataStart() != 8
                || !monthly.role().equals("SUPPLY") || !monthly.sheet().equals("RegasAdjustment") || monthly.dataStart() != 11
                || monthly.endRow() != master.endRow() + 3) throw invalid();
        Map<String, String> formulas = InputWorkbookStructure.formulas(source, monthly.sheet(), address -> {
            var ref = new CellReference(address);
            return ref.getRow() + 1 >= monthly.dataStart() && ref.getRow() + 1 <= monthly.endRow() && ref.getCol() >= 1 && ref.getCol() <= 5;
        }, checkpoint);
        if (formulas.size() != masterRows.size() * 5) throw invalid();
        Set<Integer> linked = new HashSet<>();
        for (int row : masterRows) {
            for (int column = 2; column <= 6; column++) {
                String formula = formulas.get(InputWorkbookXml.address(row + 3, column));
                if (formula == null || !formula.replace("'BCRegasification'!", "BCRegasification!")
                        .equals("BCRegasification!" + InputWorkbookXml.address(row, column))) throw invalid();
            }
            linked.add(row + 3);
        }
        return Set.copyOf(linked);
    }

    static Set<String> direct(Path source, TableRef monthly, TableRef countries, InputWorkbookXml.SheetPatch patch,
            InputWorkbookReader reader, InputWorkbookXml.Checkpoint checkpoint) throws Exception {
        String sheet = monthly.sheet(); Set<Integer> rows = new HashSet<>(), columns = new HashSet<>();
        patch.edits.forEach((row, cells) -> {
            if (row >= monthly.dataStart() && row <= monthly.endRow()) cells.forEach((column, edit) -> {
                if (column >= 8 && column <= 31) { rows.add(row); columns.add(column); }
            });
        });
        if (rows.isEmpty()) return Set.of();
        if (countries.dataStart() <= monthly.endRow() || countries.endColumn() != 115 || countries.dataStart() > countries.endRow()) throw invalid();
        Map<String, String> selected = InputWorkbookStructure.formulas(source, sheet, address -> {
            var ref = new CellReference(address); int row = ref.getRow() + 1, column = ref.getCol() + 1;
            return rows.contains(row) && column >= 118 && column <= 126
                || row >= countries.dataStart() && row <= countries.endRow() && columns.contains(column);
        }, checkpoint);
        if (selected.size() != rows.size() * 9 + (countries.endRow() - countries.dataStart() + 1) * columns.size()) throw invalid();
        Map<InputWorkbookCalculation.Address, Object> inputs = new LinkedHashMap<>();
        Set<Integer> neededColumns = new HashSet<>(columns); neededColumns.addAll(Set.of(4, 7));
        for (int column = 8; column <= 126; column++) neededColumns.add(column);
        reader.readLocal(source, List.of(new InputWorkbookReader.Window(sheet,
            row -> row == 9 || row == 10 || row >= monthly.dataStart() && row <= monthly.endRow() || row >= countries.dataStart() && row <= countries.endRow(),
            neededColumns, (row, cells) -> cells.forEach((column, cell) -> {
                boolean needed = row == 9 && column >= 8 && column <= 115 || row == 10 && column >= 118 && column <= 126
                    || row >= monthly.dataStart() && row <= monthly.endRow() && (column == 4 || columns.contains(column) || rows.contains(row) && column >= 8 && column <= 115)
                    || row >= countries.dataStart() && row <= countries.endRow() && column == 7;
                if (needed) inputs.put(new InputWorkbookCalculation.Address(sheet, InputWorkbookXml.address(row, column)), cell.raw());
            }))));
        patch.edits.forEach((row, cells) -> {
            if (rows.contains(row)) cells.forEach((column, edit) -> {
                if (columns.contains(column)) inputs.put(new InputWorkbookCalculation.Address(sheet, InputWorkbookXml.address(row, column)), edit.value());
            });
        });
        Map<InputWorkbookCalculation.Address, String> formulas = new LinkedHashMap<>();
        selected.forEach((address, formula) -> formulas.put(new InputWorkbookCalculation.Address(sheet, address), formula));
        List<InputWorkbookCalculation.KnownRange> ranges = new java.util.ArrayList<>();
        ranges.add(new InputWorkbookCalculation.KnownRange(sheet, new InputWorkbookStructure.Area(monthly.dataStart(), monthly.endRow(), 4, 4)));
        for (int column : columns) ranges.add(new InputWorkbookCalculation.KnownRange(sheet, new InputWorkbookStructure.Area(monthly.dataStart(), monthly.endRow(), column, column)));
        for (int row : rows) ranges.add(new InputWorkbookCalculation.KnownRange(sheet, new InputWorkbookStructure.Area(row, row, 8, 115)));
        var calculated = InputWorkbookCalculation.evaluate(inputs, formulas, ranges, checkpoint);
        Map<String, String> templates = new HashMap<>();
        InputWorkbookXml.rows(source, InputWorkbookXml.sheetParts(source).get(sheet), checkpoint, row -> InputWorkbookXml.cells(row.xml()).forEach((column, cell) -> {
            String address = InputWorkbookXml.address(row.number(), column);
            if (selected.containsKey(address)) templates.put(address, cell);
        }));
        for (var result : calculated.entrySet()) {
            var ref = new CellReference(result.getKey().cell());
            patch.raw(ref.getRow() + 1, ref.getCol() + 1, InputWorkbookCalculation.cache(templates.get(result.getKey().cell()), result.getValue()));
        }
        return Set.copyOf(selected.keySet());
    }
    private static InputDraftPreserveChange.Failure invalid() { return new InputDraftPreserveChange.Failure("STRUCTURE_VALIDATION_FAILED"); }
}
