package com.sk.domain.mi.service.fundamentalmodel.input;

import java.util.regex.Pattern;
import org.apache.poi.ss.util.CellReference;

/** 논리 표 안에서 복사/이동하는 수식의 상대 A1 참조만 옮긴다. 계산/권한 판단은 호출자가 한다. */
final class InputWorkbookFormula {
    private static final Pattern REFERENCE = Pattern.compile("(?<![\\p{L}\\p{N}_.$])(?<ac>\\$?)(?<col>[A-Za-z]{1,3})(?<ar>\\$?)(?<row>[1-9][0-9]*)(?![\\p{L}\\p{N}_.(!])");
    private static final Pattern WHOLE_COLUMN = Pattern.compile("(?<![\\p{L}\\p{N}_.$])(?<a>\\$?[A-Za-z]{1,3}):(?<b>\\$?[A-Za-z]{1,3})(?![\\p{L}\\p{N}_.(!])");
    private static final Pattern WHOLE_ROW = Pattern.compile("(?<![\\p{L}\\p{N}_.$])(?<a>\\$?[1-9][0-9]{0,6}):(?<b>\\$?[1-9][0-9]{0,6})(?![\\p{L}\\p{N}_.(!])");

    static String translate(String formula, int rows, int columns) {
        if (rows == 0 && columns == 0) return formula;
        StringBuilder result = new StringBuilder();
        int begin = 0;
        for (int i = 0; i < formula.length(); i++) {
            char ch = formula.charAt(i);
            if (ch != '"' && ch != '\'' && ch != '[') continue;
            result.append(references(formula.substring(begin, i), rows, columns));
            int end = i + 1; char close = ch == '[' ? ']' : ch; int depth = 1;
            for (; end < formula.length(); end++) {
                if (close == ']' && formula.charAt(end) == '[') { depth++; continue; }
                if (formula.charAt(end) != close) continue;
                if (close == ']' && --depth != 0) continue;
                if (close != ']' && end + 1 < formula.length() && formula.charAt(end + 1) == close) { end++; continue; }
                break;
            }
            if (end == formula.length()) throw invalid();
            result.append(formula, i, end + 1); i = end; begin = end + 1;
        }
        return result.append(references(formula.substring(begin), rows, columns)).toString();
    }

    private static String references(String input, int rows, int columns) {
        input = whole(input, WHOLE_COLUMN, columns, true);
        input = whole(input, WHOLE_ROW, rows, false);
        var match = REFERENCE.matcher(input); StringBuilder result = new StringBuilder(); int begin = 0;
        while (match.find()) {
            int column = CellReference.convertColStringToIndex(match.group("col")) + 1;
            if (match.group("row").length() > 7) continue;
            long row = Long.parseLong(match.group("row"));
            // A1처럼 보이는 named identifier라도 Excel 좌표가 아니면 원문을 유지한다.
            if (column > 16384 || row > 1048576) continue;
            long nextRow = row + (match.group("ar").isEmpty() ? rows : 0);
            int nextColumn = column + (match.group("ac").isEmpty() ? columns : 0);
            if (nextRow < 1 || nextRow > 1048576 || nextColumn < 1 || nextColumn > 16384) throw invalid();
            result.append(input, begin, match.start()).append(match.group("ac"))
                    .append(CellReference.convertNumToColString(nextColumn - 1)).append(match.group("ar")).append(nextRow);
            begin = match.end();
        }
        return result.append(input.substring(begin)).toString();
    }

    private static String whole(String input, Pattern pattern, int offset, boolean column) {
        if (offset == 0) return input;
        var matcher = pattern.matcher(input); StringBuffer result = new StringBuffer();
        while (matcher.find()) {
            String first = matcher.group("a"), last = matcher.group("b");
            int a = coordinate(first, column), b = coordinate(last, column), maximum = column ? 16384 : 1048576;
            if (a > maximum || b > maximum) continue;
            a += first.startsWith("$") ? 0 : offset; b += last.startsWith("$") ? 0 : offset;
            if (a < 1 || b < 1 || a > maximum || b > maximum) throw invalid();
            String replacement = (first.startsWith("$") ? "$" : "") + format(a, column) + ":"
                    + (last.startsWith("$") ? "$" : "") + format(b, column);
            matcher.appendReplacement(result, java.util.regex.Matcher.quoteReplacement(replacement));
        }
        return matcher.appendTail(result).toString();
    }
    private static int coordinate(String value, boolean column) {
        String raw = value.replace("$", ""); return column ? CellReference.convertColStringToIndex(raw) + 1 : Integer.parseInt(raw);
    }
    private static String format(int value, boolean column) { return column ? CellReference.convertNumToColString(value - 1) : Integer.toString(value); }
    private static InputDraftPreserveChange.Failure invalid() { return new InputDraftPreserveChange.Failure("STRUCTURE_VALIDATION_FAILED"); }
}
