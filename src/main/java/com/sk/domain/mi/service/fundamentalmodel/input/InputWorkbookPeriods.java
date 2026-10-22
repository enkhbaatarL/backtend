package com.sk.domain.mi.service.fundamentalmodel.input;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.poi.ss.util.CellReference;

/** 기간 열의 실제 셀 생성. 허용 표/입력 범위는 호출자가 고정 catalog와 대조한 뒤 전달한다. */
final class InputWorkbookPeriods {
    static final Set<Integer> LNG_TOTALS = Set.of(4, 5, 10, 15, 24, 25, 27, 34, 35, 43, 64, 76, 89, 100, 104, 109);
    record Added(int column, Map<Integer, Object> headers) {}
    record Plan(InputWorkbookXml.SheetPatch sheet, List<Added> added) {}

    /** 같은 worksheet의 다른 표가 이미 예약한 열도 포함한다. A/Helper/CHECK 셀은 이동하지 않는다. */
    static int nextFreeColumn(Path source, String sheet, Set<Integer> reserved, InputWorkbookXml.Checkpoint checkpoint) throws Exception {
        String part = InputWorkbookXml.sheetParts(source).get(sheet); if (part == null) throw invalid();
        int[] last = {reserved.stream().mapToInt(Integer::intValue).max().orElse(0)};
        InputWorkbookXml.rows(source, part, checkpoint, row -> {
            var cells = InputWorkbookXml.cells(row.xml());
            for (int column : cells.keySet()) last[0] = Math.max(last[0], column);
        });
        if (last[0] >= 16384) throw invalid(); return last[0] + 1;
    }

    /** 연도형7표 및 LiqProd의 확정 literal 헤더/빈 본문 생성에 공통으로 쓰는 작은 primitive. */
    static Plan literals(Path source, String sheet, int templateColumn, List<Integer> columns,
            List<Map<Integer, Object>> headers, List<InputReadDefinition.Range> body, List<Integer> copiedHeaderRows,
            Object bodyInitial, InputWorkbookXml.Checkpoint checkpoint) throws Exception {
        return literals(source, sheet, templateColumn, columns, headers, body, copiedHeaderRows, bodyInitial, null, checkpoint);
    }
    static Plan literals(Path source, String sheet, int templateColumn, List<Integer> columns,
            List<Map<Integer, Object>> headers, List<InputReadDefinition.Range> body, List<Integer> copiedHeaderRows,
            Object bodyInitial, InputWorkbookXml.SheetPatch relocated, InputWorkbookXml.Checkpoint checkpoint) throws Exception {
        if (columns.isEmpty() || columns.size() != headers.size() || Set.copyOf(columns).size() != columns.size()) throw invalid();
        String part = InputWorkbookXml.sheetParts(source).get(sheet); if (part == null) throw invalid();
        Map<Integer, String> templates = new HashMap<>();
        InputWorkbookXml.rows(source, part, checkpoint, row -> {
            var cells = InputWorkbookXml.cells(row.xml());
            if (body.stream().anyMatch(range -> row.number() >= range.startRow() && row.number() <= range.endRow())
                    || copiedHeaderRows.contains(row.number()) || headers.stream().anyMatch(values -> values.containsKey(row.number()))) {
                for (int column : columns) if (cells.containsKey(column)) {
                    String destination = relocated == null ? cells.get(column) : relocated.raw.getOrDefault(row.number(), Map.of()).getOrDefault(column, cells.get(column));
                    if (destination != null && java.util.regex.Pattern.compile("<(?:f|v|is)(?=\\s|/?>)").matcher(destination).find()) throw invalid();
                }
                if (cells.containsKey(templateColumn)) templates.put(row.number(), cells.get(templateColumn));
            }
        });
        InputWorkbookXml.SheetPatch patch = new InputWorkbookXml.SheetPatch(); List<Added> added = new ArrayList<>();
        for (int i = 0; i < columns.size(); i++) {
            int column = columns.get(i); if (column < 1 || column <= templateColumn && relocated == null || column > 16384) throw invalid();
            for (var range : body) for (int row = range.startRow(); row <= range.endRow(); row++) {
                if ((row & 1023) == 0) checkpoint.check();
                patch.put(row, column, new InputWorkbookXml.Edit(bodyInitial, templates.get(row), false));
            }
            for (int row : copiedHeaderRows) {
                String template = templates.get(row); if (template == null || InputWorkbookXml.hasFormula(template)) throw invalid();
                patch.raw(row, column, InputWorkbookXml.replaceAttribute(template, "r", InputWorkbookXml.address(row, column)));
            }
            for (var header : headers.get(i).entrySet()) patch.put(header.getKey(), column, new InputWorkbookXml.Edit(header.getValue(), templates.get(header.getKey()), false));
            added.add(new Added(column, Map.copyOf(headers.get(i))));
        }
        return new Plan(patch, List.copyOf(added));
    }

    static Map<Integer, Object> productionHeaders(BigDecimal year, BigDecimal month) {
        try {
            var y = year.toBigIntegerExact(); int m = month.intValueExact(); if (m < 1 || m > 12) throw invalid();
            var period = y.subtract(java.math.BigInteger.valueOf(2025)).multiply(java.math.BigInteger.valueOf(12)).add(java.math.BigInteger.valueOf(84L + m));
            return Map.of(7, year, 8, month, 9, new BigDecimal(period));
        } catch (ArithmeticException ex) { throw invalid(); }
    }

    static Map<Integer, Object> nextUtilizationHeaders(BigDecimal previousYear, BigDecimal previousMonth, BigDecimal previousPeriod) {
        try {
            var year = previousYear.toBigIntegerExact(); int month = previousMonth.intValueExact();
            var period = previousPeriod.toBigIntegerExact().add(java.math.BigInteger.ONE);
            if (month < 1 || month > 12) throw invalid();
            if (++month == 13) { month = 1; year = year.add(java.math.BigInteger.ONE); }
            return Map.of(7, new BigDecimal(year), 8, BigDecimal.valueOf(month), 9, new BigDecimal(period));
        } catch (ArithmeticException ex) { throw invalid(); }
    }

    static Map<Integer, Map<Integer, Object>> monthHeaders(Path source, String sheet, Set<Integer> columns,
            InputWorkbookReader reader, InputWorkbookXml.Checkpoint checkpoint) throws Exception {
        if (columns.isEmpty()) return Map.of();
        var selected = InputWorkbookStructure.formulas(source, sheet, address -> {
            var ref = new CellReference(address); return ref.getRow() == 6 && columns.contains((int) ref.getCol() + 1);
        }, checkpoint);
        Map<InputWorkbookCalculation.Address, Object> inputs = new LinkedHashMap<>();
        Map<Integer, Map<Integer, Object>> result = new LinkedHashMap<>();
        reader.readLocal(source, List.of(new InputWorkbookReader.Window(sheet, row -> row >= 7 && row <= 9, columns, (row, cells) -> cells.forEach((column, cell) -> {
            String address = InputWorkbookXml.address(row, column);
            if (selected.containsKey(address)) return;
            if (cell.formula() || !(cell.raw() instanceof BigDecimal)) throw invalid();
            inputs.put(new InputWorkbookCalculation.Address(sheet, address), cell.raw());
            result.computeIfAbsent(column, ignored -> new LinkedHashMap<>()).put(row, cell.raw());
        }))));
        Map<InputWorkbookCalculation.Address, String> formulas = new LinkedHashMap<>(); selected.forEach((address, formula) -> formulas.put(new InputWorkbookCalculation.Address(sheet, address), formula));
        InputWorkbookCalculation.evaluate(inputs, formulas, checkpoint).forEach((address, value) -> result.computeIfAbsent(new CellReference(address.cell()).getCol() + 1, ignored -> new LinkedHashMap<>()).put(7, value));
        for (int column : columns) if (!result.containsKey(column) || !result.get(column).keySet().equals(Set.of(7, 8, 9))) throw invalid();
        return result;
    }

    /** 월 삭제 후에도 생존 월의 연월을 유지한다. 삭제된 Year 참조는 입력으로 계산한 값으로 보존한다. */
    static void monthlyYears(Path source, String sheet, Map<Integer, Integer> positions, InputWorkbookXml.SheetPatch patch,
            InputWorkbookReader reader, InputWorkbookXml.Checkpoint checkpoint) throws Exception {
        Set<Integer> columns = positions.keySet().stream().filter(c -> c >= 7).collect(java.util.stream.Collectors.toSet());
        Map<String, String> original = InputWorkbookStructure.formulas(source, sheet, address -> {
            var ref = new CellReference(address); return ref.getRow() == 6 && columns.contains((int) ref.getCol() + 1);
        }, checkpoint);
        Map<InputWorkbookCalculation.Address, Object> inputs = new LinkedHashMap<>();
        reader.readLocal(source, List.of(new InputWorkbookReader.Window(sheet, row -> row == 7 || row == 8, columns, (row, cells) -> cells.forEach((col, cell) -> {
            String address = InputWorkbookXml.address(row, col);
            if (!original.containsKey(address)) {
                if (cell.formula() || !(cell.raw() instanceof BigDecimal)) throw invalid();
                inputs.put(new InputWorkbookCalculation.Address(sheet, address), cell.raw());
            }
        }))));
        Map<InputWorkbookCalculation.Address, String> formulas = new LinkedHashMap<>(); original.forEach((address, formula) -> formulas.put(new InputWorkbookCalculation.Address(sheet, address), formula));
        var computed = InputWorkbookCalculation.evaluate(inputs, formulas, checkpoint);
        Map<InputWorkbookCalculation.Address, Object> nextInputs = new LinkedHashMap<>();
        for (var input : inputs.entrySet()) {
            var ref = new CellReference(input.getKey().cell()); Integer column = positions.get((int) ref.getCol() + 1);
            if (column != null) nextInputs.put(new InputWorkbookCalculation.Address(sheet, InputWorkbookXml.address(ref.getRow() + 1, column)), input.getValue());
        }
        Map<InputWorkbookCalculation.Address, String> nextFormulas = new LinkedHashMap<>();
        Map<InputWorkbookCalculation.Address, Object> expected = new LinkedHashMap<>();
        var shape = java.util.regex.Pattern.compile("IF\\(([A-Z]+)8=1,([A-Z]+7|\\(-?[0-9]+(?:\\.[0-9]+)?\\))\\+1,\\2\\)");
        for (var formula : formulas.entrySet()) {
            int from = new CellReference(formula.getKey().cell()).getCol() + 1; Integer to = positions.get(from); if (to == null) continue;
            var match = shape.matcher(formula.getValue()); if (!match.matches()) throw invalid();
            int monthColumn = CellReference.convertColStringToIndex(match.group(1)) + 1;
            if (monthColumn != from) throw invalid();
            String reference = match.group(2);
            if (!reference.startsWith("(")) {
                int yearColumn = new CellReference(reference).getCol() + 1;
                if (!positions.containsKey(yearColumn)) throw invalid();
                Integer previous = positions.get(yearColumn);
                if (previous != null) reference = InputWorkbookXml.address(7, previous);
                else {
                var address = new InputWorkbookCalculation.Address(sheet, InputWorkbookXml.address(7, yearColumn));
                Object value = computed.containsKey(address) ? computed.get(address) : inputs.get(address);
                if (!(value instanceof BigDecimal year)) throw invalid();
                reference = "(" + year.toPlainString() + ")";
                // 보통의 중간 월 삭제는 바로 앞 생존 Year를 계속 읽는다. 연도가 달라지는 경계만 상수로 보존한다.
                var earlier = positions.entrySet().stream().filter(e -> e.getKey() >= 7 && e.getValue() != null && e.getValue() < to).max(Map.Entry.comparingByValue()).orElse(null);
                if (earlier != null) {
                    var previousAddress = new InputWorkbookCalculation.Address(sheet, InputWorkbookXml.address(7, earlier.getKey()));
                    Object previousYear = computed.containsKey(previousAddress) ? computed.get(previousAddress) : inputs.get(previousAddress);
                    if (previousYear instanceof BigDecimal number && number.compareTo(year) == 0) reference = InputWorkbookXml.address(7, earlier.getValue());
                }
                }
            }
            var address = new InputWorkbookCalculation.Address(sheet, InputWorkbookXml.address(7, to));
            nextFormulas.put(address, "IF(" + InputWorkbookXml.address(8, to) + "=1," + reference + "+1," + reference + ")");
            expected.put(address, computed.get(formula.getKey()));
        }
        var calculated = InputWorkbookCalculation.evaluate(nextInputs, nextFormulas, checkpoint);
        for (var formula : nextFormulas.entrySet()) {
            if (((BigDecimal) calculated.get(formula.getKey())).compareTo((BigDecimal) expected.get(formula.getKey())) != 0) throw invalid();
            int column = new CellReference(formula.getKey().cell()).getCol() + 1;
            String cell = patch.raw.getOrDefault(7, Map.of()).get(column); if (cell == null) continue;
            cell = cell.replaceAll("<f(?=\\s|/?>)[^>]*?(?:/>|>.*?</f>)", java.util.regex.Matcher.quoteReplacement("<f>" + InputWorkbookXml.escape(formula.getValue()) + "</f>"));
            patch.raw(7, column, InputWorkbookCalculation.cache(cell, calculated.get(formula.getKey())));
        }
    }

    static List<String> utilizationYears(Map<Integer, Map<Integer, Object>> headers, Set<Integer> added,
            InputWorkbookXml.SheetPatch patch, InputWorkbookXml.Checkpoint checkpoint) {
        String sheet = "LiqUtilAdjustment"; var ordered = new java.util.TreeMap<>(headers);
        Map<InputWorkbookCalculation.Address, Object> inputs = new LinkedHashMap<>();
        Map<InputWorkbookCalculation.Address, String> formulas = new LinkedHashMap<>();
        for (int column : added.stream().sorted().toList()) {
            var current = headers.get(column); var previous = ordered.lowerEntry(column);
            if (previous == null) continue; // 전체 삭제 뒤 첫 월은 고정 원본 첫 Year literal로 다시 시작한다.
            BigDecimal year = (BigDecimal) current.get(7), month = (BigDecimal) current.get(8);
            BigDecimal base = month.intValueExact() == 1 ? year.subtract(BigDecimal.ONE) : year;
            String reference = "(" + base.toPlainString() + ")";
            if (((BigDecimal) previous.getValue().get(7)).compareTo(base) == 0) {
                reference = InputWorkbookXml.address(7, previous.getKey());
                if (!added.contains(previous.getKey()) || ordered.lowerKey(previous.getKey()) == null)
                    inputs.put(new InputWorkbookCalculation.Address(sheet, reference), previous.getValue().get(7));
            }
            String address = InputWorkbookXml.address(7, column), monthCell = InputWorkbookXml.address(8, column);
            inputs.put(new InputWorkbookCalculation.Address(sheet, monthCell), month);
            formulas.put(new InputWorkbookCalculation.Address(sheet, address), "IF(" + monthCell + "=1," + reference + "+1," + reference + ")");
        }
        var calculated = InputWorkbookCalculation.evaluate(inputs, formulas, checkpoint);
        for (var formula : formulas.entrySet()) {
            int column = new CellReference(formula.getKey().cell()).getCol() + 1;
            if (((BigDecimal) calculated.get(formula.getKey())).compareTo((BigDecimal) headers.get(column).get(7)) != 0) throw invalid();
            var edit = patch.edits.get(7).remove(column);
            String cell = InputWorkbookXml.valueCell(edit.template(), formula.getKey().cell(), null)
                .replace("</c>", "<f>" + InputWorkbookXml.escape(formula.getValue()) + "</f></c>");
            patch.raw(7, column, InputWorkbookCalculation.cache(cell, calculated.get(formula.getKey())));
        }
        return formulas.keySet().stream().map(InputWorkbookCalculation.Address::cell).toList();
    }

    /** BCRegas의 연도는 MAX 앞에 추가한다. RegasAdjustment의 기존 R 참조는 명시 보존 예외다. */
    static InputWorkbookStructure.Result regasYear(Path source, int lastYearColumn, int lastRow, BigDecimal year,
            Map<Integer, BigDecimal> explicitValues, InputWorkbookReader reader, InputWorkbookXml.Checkpoint checkpoint) throws Exception {
        try { year.toBigIntegerExact(); } catch (ArithmeticException ex) { throw invalid(); }
        String sheet = "BCRegasification"; int added = lastYearColumn + 1, maximum = added + 1;
        if (lastYearColumn < 17 || maximum > 16384 || lastRow < 8 || explicitValues.keySet().stream().anyMatch(r -> r < 8 || r > lastRow)) throw invalid();
        var moved = InputWorkbookStructure.move(source, sheet,
            List.of(new InputWorkbookStructure.Move(new InputWorkbookStructure.Area(7, lastRow, added, added), 0, 1)), List.of(), checkpoint);
        var patch = moved.sheet(); String part = InputWorkbookXml.sheetParts(source).get(sheet);
        Map<Integer, String> previous = new HashMap<>(), maximumCells = new HashMap<>();
        InputWorkbookXml.rows(source, part, checkpoint, row -> {
            if (row.number() < 7 || row.number() > lastRow) return;
            var cells = InputWorkbookXml.cells(row.xml()); previous.put(row.number(), cells.get(lastYearColumn)); maximumCells.put(row.number(), cells.get(added));
            String destination=cells.get(maximum);
            if(destination!=null&&java.util.regex.Pattern.compile("<(?:f|v|is)(?=\\s|/?>)").matcher(destination).find())throw invalid();
        });
        if (maximumCells.get(7) == null) throw invalid();
        patch.put(7, added, new InputWorkbookXml.Edit(year, previous.get(7), false));
        Map<InputWorkbookCalculation.Address, Object> inputs = new LinkedHashMap<>(); Set<Integer> columns = new java.util.HashSet<>();
        for (int column = 7; column <= lastYearColumn; column++) columns.add(column);
        reader.readLocal(source, List.of(new InputWorkbookReader.Window(sheet, row -> row >= 8 && row <= lastRow, columns,
            (row, values) -> values.forEach((col, value) -> {
                if (value.formula()) throw invalid(); inputs.put(new InputWorkbookCalculation.Address(sheet, InputWorkbookXml.address(row, col)), value.raw());
            }))));
        Map<InputWorkbookCalculation.Address, String> formulas = new LinkedHashMap<>();
        String last = CellReference.convertNumToColString(added - 1);
        for (int row = 8; row <= lastRow; row++) {
            checkpoint.check(); String old = maximumCells.get(row);
            if (old == null || !InputWorkbookXml.hasFormula(old)) throw invalid();
            BigDecimal value = explicitValues.getOrDefault(row, BigDecimal.ZERO);
            patch.put(row, added, new InputWorkbookXml.Edit(value, previous.get(row), false));
            inputs.put(new InputWorkbookCalculation.Address(sheet, InputWorkbookXml.address(row, added)), value);
            formulas.put(new InputWorkbookCalculation.Address(sheet, InputWorkbookXml.address(row, maximum)), "MAX(G" + row + ":" + last + row + ")");
        }
        var calculated = InputWorkbookCalculation.evaluate(inputs, formulas,
            List.of(new InputWorkbookCalculation.KnownRange(sheet, new InputWorkbookStructure.Area(8, lastRow, 7, added))), checkpoint);
        for (var formula : formulas.entrySet()) {
            int row = new CellReference(formula.getKey().cell()).getRow() + 1;
            String cell = InputWorkbookXml.valueCell(maximumCells.get(row), formula.getKey().cell(), null)
                .replace("</c>", "<f>" + InputWorkbookXml.escape(formula.getValue()) + "</f></c>");
            patch.raw(row, maximum, InputWorkbookCalculation.cache(cell, calculated.get(formula.getKey())));
        }
        return moved;
    }

    /** master의 MAX만 계산한다. 연도 추가의 RegasAdjustment 보존 예외를 유지한다. */
    static void regasMaximum(Path source, int previousMaximum, int maximum, Set<Integer> rows,
            InputWorkbookXml.SheetPatch patch, InputWorkbookReader reader, InputWorkbookXml.Checkpoint checkpoint) throws Exception {
        regasMaximum(source, previousMaximum, maximum, rows, null, patch, reader, checkpoint);
    }

    static void regasMaximum(Path source, int previousMaximum, int maximum, Set<Integer> rows, Map<Integer, Integer> sourceRows,
            InputWorkbookXml.SheetPatch patch, InputWorkbookReader reader, InputWorkbookXml.Checkpoint checkpoint) throws Exception {
        if (rows.isEmpty()) return;
        if (previousMaximum < 18 || maximum < previousMaximum || maximum > 16384 || rows.stream().anyMatch(row -> row < 8)) throw invalid();
        String sheet = "BCRegasification";
        Set<Integer> columns = new java.util.HashSet<>(); for (int column = 7; column < previousMaximum; column++) columns.add(column);
        Map<InputWorkbookCalculation.Address, Object> inputs = new LinkedHashMap<>();
        Map<Integer, Integer> destinations = new HashMap<>();
        for (int row : rows) {
            Integer original = sourceRows == null ? Integer.valueOf(row) : sourceRows.get(row);
            if (original != null && destinations.putIfAbsent(original, row) != null) throw invalid();
        }
        reader.readLocal(source, List.of(new InputWorkbookReader.Window(sheet, destinations::containsKey, columns, (row, cells) -> cells.forEach((column, cell) -> {
            if (cell.formula()) throw invalid();
            inputs.put(new InputWorkbookCalculation.Address(sheet, InputWorkbookXml.address(destinations.get(row), column)), cell.raw());
        }))));
        patch.edits.forEach((row, cells) -> {
            if (rows.contains(row)) cells.forEach((column, edit) -> {
                if (column >= 7 && column < maximum) inputs.put(new InputWorkbookCalculation.Address(sheet, InputWorkbookXml.address(row, column)), edit.value());
            });
        });
        Map<Integer, String> templates = new HashMap<>();
        if (sourceRows != null) for (int row : rows) if (sourceRows.get(row) == null) templates.put(row, patch.raw.getOrDefault(row, Map.of()).get(maximum));
        InputWorkbookXml.rows(source, InputWorkbookXml.sheetParts(source).get(sheet), checkpoint, row -> {
            if (destinations.containsKey(row.number()) || row.number() == 7) {
                var cells = InputWorkbookXml.cells(row.xml());
                if (maximum != previousMaximum && cells.get(maximum) != null
                        && java.util.regex.Pattern.compile("<(?:f|v|is)(?=\\s|/?>)").matcher(cells.get(maximum)).find()) throw invalid();
                if (destinations.containsKey(row.number())) templates.put(destinations.get(row.number()), cells.get(previousMaximum));
            }
        });
        String last = CellReference.convertNumToColString(maximum - 2);
        Map<InputWorkbookCalculation.Address, String> formulas = new LinkedHashMap<>();
        for (int row : rows.stream().sorted().toList()) {
            if (templates.get(row) == null || !InputWorkbookXml.hasFormula(templates.get(row))) throw invalid();
            formulas.put(new InputWorkbookCalculation.Address(sheet, InputWorkbookXml.address(row, maximum)), "MAX(G" + row + ":" + last + row + ")");
        }
        var ranges = rows.stream().map(row -> new InputWorkbookCalculation.KnownRange(sheet, new InputWorkbookStructure.Area(row, row, 7, maximum - 1))).toList();
        var calculated = InputWorkbookCalculation.evaluate(inputs, formulas, ranges, checkpoint);
        for (var formula : formulas.entrySet()) {
            int row = new CellReference(formula.getKey().cell()).getRow() + 1;
            String cell = InputWorkbookXml.valueCell(templates.get(row), formula.getKey().cell(), null)
                .replace("</c>", "<f>" + InputWorkbookXml.escape(formula.getValue()) + "</f></c>");
            patch.raw(row, maximum, InputWorkbookCalculation.cache(cell, calculated.get(formula.getKey())));
        }
    }

    /** LNG 새 연도의16 합계. 신규 국가90칸은 호출자의 literal plan에서 실제 빈칸으로 생성한다. */
    static void lngTotals(Path source, String sheet, int templateColumn, int column,
            InputWorkbookXml.SheetPatch patch, InputWorkbookXml.Checkpoint checkpoint) throws Exception {
        Set<Integer> totals = LNG_TOTALS;
        Map<String, String> formulas = InputWorkbookStructure.formulas(source, sheet, address -> {
            var ref = new CellReference(address); return ref.getCol() + 1 == templateColumn && totals.contains(ref.getRow() + 1);
        }, checkpoint);
        String col = CellReference.convertNumToColString(column - 1), part = InputWorkbookXml.sheetParts(source).get(sheet);
        Map<Integer, String> templates = new HashMap<>();
        InputWorkbookXml.rows(source, part, checkpoint, row -> {
            if (totals.contains(row.number())) templates.put(row.number(), InputWorkbookXml.cells(row.xml()).get(templateColumn));
        });
        Map<InputWorkbookCalculation.Address, String> selected = new LinkedHashMap<>();
        for (int row : totals.stream().sorted().toList()) {
            String formula = switch (row) {
                case 24 -> "SUM(" + col + "25," + col + "27)";
                case 25 -> "SUM(" + col + "26)";
                case 27 -> "SUM(" + col + "28:" + col + "33)";
                default -> InputWorkbookFormula.translate(formulas.get(InputWorkbookXml.address(row, templateColumn)), 0, column - templateColumn);
            };
            selected.put(new InputWorkbookCalculation.Address(sheet, InputWorkbookXml.address(row, column)), formula);
        }
        Map<InputWorkbookCalculation.Address, Object> inputs = new LinkedHashMap<>();
        for (int row = 4; row <= 109; row++) {
            var edit = patch.edits.getOrDefault(row, Map.of()).get(column);
            if (edit != null && !totals.contains(row)) inputs.put(new InputWorkbookCalculation.Address(sheet, InputWorkbookXml.address(row, column)), edit.value());
        }
        var values = InputWorkbookCalculation.evaluate(inputs, selected,
                List.of(new InputWorkbookCalculation.KnownRange(sheet, new InputWorkbookStructure.Area(4, 109, column, column))), checkpoint);
        for (var formula : selected.entrySet()) {
            int row = new CellReference(formula.getKey().cell()).getRow() + 1;
            String cell = InputWorkbookXml.valueCell(templates.get(row), formula.getKey().cell(), null);
            cell = cell.replace("</c>", "<f>" + InputWorkbookXml.escape(formula.getValue()) + "</f></c>");
            patch.raw(row, column, InputWorkbookCalculation.cache(cell, values.get(formula.getKey())));
            patch.edits.get(row).remove(column);
        }
    }

    /** 기존 E:N의13합계 또는 새 연도의16합계만 현재 입력으로 다시 계산한다. 이전 합계 cache는 입력으로 쓰지 않는다. */
    static Set<String> lngExisting(Path source, Set<Integer> columns, InputWorkbookXml.SheetPatch patch,
            InputWorkbookReader reader, InputWorkbookXml.Checkpoint checkpoint) throws Exception {
        if (columns.isEmpty()) return Set.of();
        String sheet = "LNG Demand";
        Map<String, String> selected = InputWorkbookStructure.formulas(source, sheet, address -> {
            var ref = new CellReference(address); return columns.contains((int) ref.getCol() + 1) && LNG_TOTALS.contains(ref.getRow() + 1);
        }, checkpoint);
        for (int column : columns) {
            long count = selected.keySet().stream().filter(a -> new CellReference(a).getCol() + 1 == column).count();
            if (count != (column <= 14 ? 13 : 16)) throw invalid();
        }
        Map<InputWorkbookCalculation.Address, Object> inputs = new LinkedHashMap<>();
        reader.readLocal(source, List.of(new InputWorkbookReader.Window(sheet, r -> r >= 4 && r <= 109, columns, (row, cells) -> cells.forEach((column, cell) -> {
            String address = InputWorkbookXml.address(row, column);
            if (!selected.containsKey(address)) {
                if (cell.formula()) throw invalid();
                inputs.put(new InputWorkbookCalculation.Address(sheet, address), cell.raw());
            }
        }))));
        patch.edits.forEach((row, cells) -> cells.forEach((column, edit) -> {
            if (row >= 4 && row <= 109 && columns.contains(column)) inputs.put(new InputWorkbookCalculation.Address(sheet, InputWorkbookXml.address(row, column)), edit.value());
        }));
        Map<InputWorkbookCalculation.Address, String> formulas = new LinkedHashMap<>(); selected.forEach((address, formula) -> formulas.put(new InputWorkbookCalculation.Address(sheet, address), formula));
        var ranges = columns.stream().map(column -> new InputWorkbookCalculation.KnownRange(sheet, new InputWorkbookStructure.Area(4, 109, column, column))).toList();
        var calculated = InputWorkbookCalculation.evaluate(inputs, formulas, ranges, checkpoint);
        InputWorkbookXml.rows(source, InputWorkbookXml.sheetParts(source).get(sheet), checkpoint, row -> InputWorkbookXml.cells(row.xml()).forEach((column, cell) -> {
            var address = new InputWorkbookCalculation.Address(sheet, InputWorkbookXml.address(row.number(), column));
            if (calculated.containsKey(address)) patch.raw(row.number(), column, InputWorkbookCalculation.cache(cell, calculated.get(address)));
        }));
        return Set.copyOf(selected.keySet());
    }

    private static InputDraftPreserveChange.Failure invalid() { return new InputDraftPreserveChange.Failure("STRUCTURE_VALIDATION_FAILED"); }
}
