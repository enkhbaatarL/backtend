package com.sk.domain.mi.service.fundamentalmodel.input;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.poi.ss.formula.FormulaParser;
import org.apache.poi.ss.formula.FormulaType;
import org.apache.poi.ss.formula.ptg.*;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFEvaluationWorkbook;

/** 승인된 셀 집합만 별도 workbook에서 계산한다. 원본 파일/매크로/외부 링크는 열지 않는다. */
final class InputWorkbookCalculation {
    record Address(String sheet, String cell) {}
    /** 호출자가 source를 끝까지 읽고 빈칸도 확인한 범위. 읽지 않은 셀을 0으로 계산하지 않는다. */
    record KnownRange(String sheet, InputWorkbookStructure.Area area) {}
    private static final Set<String> FUNCTIONS = Set.of("IF", "IFERROR", "AND", "OR", "ISBLANK", "ISNUMBER",
            "SUM", "COUNT", "COUNTA", "COUNTBLANK", "COUNTIF", "COUNTIFS", "SUMIF", "SUMIFS", "MAX", "MIN",
            "MATCH", "INDEX", "VLOOKUP", "HLOOKUP", "ROUND", "ABS", "SUMPRODUCT", "LEFT", "LEN", "SEARCH");
    // POI는 이 내장 함수들을 NameXPxg + external-function token으로 표현한다. 파일 외부 참조는 아니다.
    private static final Set<String> NAMED_FUNCTIONS = Set.of("SUMIFS", "COUNTIFS", "IFERROR");

    static boolean supportsExpression(String expression) {
        var calls=java.util.regex.Pattern.compile("([A-Za-z_][A-Za-z0-9_.]*)\\s*\\(").matcher(expression.replaceAll("\"(?:[^\"]|\"\")*\"",""));
        while(calls.find())if(!FUNCTIONS.contains(calls.group(1).toUpperCase(java.util.Locale.ROOT)))return false;return true;
    }

    /** constants에는 보존 cache를 포함할 수 있으나 targets의 이전 cache는 넣지 않는다. */
    static Map<Address, Object> evaluate(Map<Address, Object> constants, Map<Address, String> formulas,
            InputWorkbookXml.Checkpoint checkpoint) {
        return evaluate(constants, formulas, List.of(), checkpoint);
    }

    static Map<Address, Object> evaluate(Map<Address, Object> constants, Map<Address, String> formulas,
            List<KnownRange> knownRanges, InputWorkbookXml.Checkpoint checkpoint) {
        if (constants.size() + formulas.size() > 250000 || formulas.keySet().stream().anyMatch(constants::containsKey)) throw failed();
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            for (Address address : constants.keySet()) sheet(workbook, address.sheet());
            for (Address address : formulas.keySet()) sheet(workbook, address.sheet());
            for (KnownRange range : knownRanges) sheet(workbook, range.sheet());
            for (var entry : constants.entrySet()) {
                checkpoint.check(); XSSFCell cell = cell(workbook, entry.getKey()); Object value = entry.getValue();
                if (value == null) cell.setBlank();
                else if (value instanceof String text) cell.setCellValue(text);
                else if (value instanceof Boolean bool) cell.setCellValue(bool);
                else if (value instanceof Number number) {
                    double decimal = number.doubleValue(); if (!Double.isFinite(decimal)) throw failed(); cell.setCellValue(decimal);
                } else throw failed();
            }
            for (var entry : formulas.entrySet()) {
                checkpoint.check();
                // 외부 참조 예외는 별도 고정 cache 입력 계약을 가진 경로에서만 구현한다.
                if (entry.getValue().contains("[") || entry.getValue().contains("]")) throw failed();
                cell(workbook, entry.getKey()).setCellFormula(entry.getValue());
            }
            var parsing = XSSFEvaluationWorkbook.create(workbook);
            for (var entry : formulas.entrySet()) {
                checkpoint.check();
                int namedFunctions = 0;
                for (Ptg token : FormulaParser.parse(entry.getValue(), parsing, FormulaType.CELL, workbook.getSheetIndex(entry.getKey().sheet()))) {
                    String sheet = entry.getKey().sheet();
                    if (token instanceof NameXPxg name) {
                        if (name.getExternalWorkbookNumber() >= 0 || name.getSheetName() != null || !NAMED_FUNCTIONS.contains(name.getNameName())) throw failed();
                        namedFunctions++;
                    } else if (token instanceof Ref3DPxg ref) {
                        if (ref.getExternalWorkbookNumber() >= 0 || ref.getLastSheetName() != null) throw failed(); sheet = ref.getSheetName();
                    } else if (token instanceof Area3DPxg ref) {
                        if (ref.getExternalWorkbookNumber() >= 0 || ref.getLastSheetName() != null) throw failed(); sheet = ref.getSheetName();
                    } else if (token instanceof NamePtg || token instanceof NameXPtg
                            || token instanceof Ref3DPtg || token instanceof Area3DPtg || token instanceof ErrPtg
                            || token instanceof RefErrorPtg || token instanceof AreaErrPtg) throw failed();
                    if (token instanceof RefPtgBase ref) known(sheet, ref.getRow() + 1, ref.getRow() + 1, ref.getColumn() + 1, ref.getColumn() + 1, constants, formulas, knownRanges, checkpoint);
                    else if (token instanceof AreaPtgBase ref) known(sheet, ref.getFirstRow() + 1, ref.getLastRow() + 1, ref.getFirstColumn() + 1, ref.getLastColumn() + 1, constants, formulas, knownRanges, checkpoint);
                    else if (token instanceof AbstractFunctionPtg function) {
                        if (function.isExternalFunction()) { if (namedFunctions-- <= 0) throw failed(); }
                        else if (!FUNCTIONS.contains(function.getName())) throw failed();
                    }
                }
                if (namedFunctions != 0) throw failed();
            }
            var evaluator = workbook.getCreationHelper().createFormulaEvaluator();
            evaluator.setIgnoreMissingWorkbooks(false);
            Map<Address, Object> results = new LinkedHashMap<>();
            for (Address address : formulas.keySet()) {
                checkpoint.check(); var result = evaluator.evaluate(cell(workbook, address));
                if (result == null) throw failed();
                Object value = switch (result.getCellType()) {
                    case NUMERIC -> {
                        if (!Double.isFinite(result.getNumberValue())) throw failed();
                        yield BigDecimal.valueOf(result.getNumberValue());
                    }
                    case STRING -> result.getStringValue();
                    case BOOLEAN -> result.getBooleanValue();
                    default -> throw failed();
                };
                results.put(address, value);
            }
            return results;
        } catch (InputDraftPreserveChange.Failure ex) { throw ex; }
        catch (Exception ex) { throw failed(); }
    }

    private static void known(String sheet, int firstRow, int lastRow, int firstCol, int lastCol,
            Map<Address, Object> constants, Map<Address, String> formulas, List<KnownRange> ranges, InputWorkbookXml.Checkpoint checkpoint) {
        if (ranges.stream().anyMatch(range -> range.sheet().equals(sheet) && range.area().contains(firstRow, firstCol) && range.area().contains(lastRow, lastCol))) return;
        if ((long) (lastRow - firstRow + 1) * (lastCol - firstCol + 1) > 250000) throw failed();
        for (int row = firstRow; row <= lastRow; row++) {
            checkpoint.check();
            for (int col = firstCol; col <= lastCol; col++) {
                Address address = new Address(sheet, InputWorkbookXml.address(row, col));
                if (!constants.containsKey(address) && !formulas.containsKey(address)) throw failed();
            }
        }
    }

    /** 수식과 style은 유지하고 cache만 교체한다. 빈 문자열과 실제 빈 셀을 구분한다. */
    static String cache(String cell, Object value) {
        if (!InputWorkbookXml.hasFormula(cell)) throw failed();
        int end = cell.indexOf('>');
        String opening = cell.substring(0, end).replaceAll("\\s+t=(?:\"[^\"]*\"|'[^']*')", "");
        String raw;
        if (value instanceof String text) { opening += " t=\"str\""; raw = InputWorkbookXml.escape(text); }
        else if (value instanceof Boolean bool) { opening += " t=\"b\""; raw = bool ? "1" : "0"; }
        else if (value instanceof BigDecimal decimal) raw = decimal.toPlainString();
        else throw failed();
        String body = cell.substring(end + 1, cell.lastIndexOf("</c>"));
        var existing = java.util.regex.Pattern.compile("<v(?:\\s[^>]*?)?(?:/>|>.*?</v>)", java.util.regex.Pattern.DOTALL).matcher(body);
        if (existing.find()) body = existing.replaceFirst(java.util.regex.Matcher.quoteReplacement("<v>" + raw + "</v>"));
        else {
            int formulaEnd = body.indexOf("</f>");
            if (formulaEnd >= 0) formulaEnd += 4;
            else { formulaEnd = body.indexOf("/>"); if (formulaEnd < 0) throw failed(); formulaEnd += 2; }
            body = body.substring(0, formulaEnd) + "<v>" + raw + "</v>" + body.substring(formulaEnd);
        }
        return opening + ">" + body + "</c>";
    }

    private static void sheet(XSSFWorkbook workbook, String name) {
        if (name == null || name.isBlank()) throw failed();
        if (workbook.getSheet(name) == null) workbook.createSheet(name);
    }
    private static XSSFCell cell(XSSFWorkbook workbook, Address address) {
        var ref = new CellReference(address.cell());
        if (ref.getRow() < 0 || ref.getRow() > 1048575 || ref.getCol() < 0 || ref.getCol() > 16383 || ref.getSheetName() != null) throw failed();
        var sheet = workbook.getSheet(address.sheet()); var row = sheet.getRow(ref.getRow());
        if (row == null) row = sheet.createRow(ref.getRow());
        XSSFCell cell = row.getCell(ref.getCol()); return cell == null ? row.createCell(ref.getCol(), CellType.BLANK) : cell;
    }
    private static InputDraftPreserveChange.Failure failed() { return new InputDraftPreserveChange.Failure("CALCULATION_FAILED"); }
}
