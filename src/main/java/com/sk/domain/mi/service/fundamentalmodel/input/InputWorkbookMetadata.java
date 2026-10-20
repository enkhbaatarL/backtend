package com.sk.domain.mi.service.fundamentalmodel.input;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.UnaryOperator;
import java.util.zip.ZipFile;

/** 새 물리 열에 이전 입력 열의 폭/숨김/기본 style을 복제하고 나머지 열 구간은 보존한다. */
final class InputWorkbookMetadata {
    record Copy(InputWorkbookStructure.Area source, int rows, int columns) {}

    /** 두 Regas 논리 표의 확인된 숨김 필터 이름만 조정한다. 다른 이름/식은 원문을 유지한다. */
    static InputWorkbookXml.PartPatch filterNames(Integer previousLast, Integer last, Integer masterLast, Map<String, Map.Entry<String, String>> appended) {
        return (input, output, checkpoint) -> {
            boolean[] filter = {false}; int[] monthly = {0};
            InputWorkbookXml.scan(input, checkpoint, token -> {
                if (token.startsWith("<definedName ")) filter[0] = "_xlnm._FilterDatabase".equals(InputWorkbookXml.attribute(token, "name"));
                else if (token.equals("</definedName>")) filter[0] = false;
                else if (filter[0] && previousLast != null && token.startsWith("RegasAdjustment!")) {
                    if (++monthly[0] > 1 || !token.equals("RegasAdjustment!$B$10:$G$" + previousLast)) throw invalid();
                    token = "RegasAdjustment!$B$10:$G$" + last;
                } else if (filter[0] && masterLast != null && token.startsWith("BCRegasification!")) {
                    var range = java.util.regex.Pattern.compile("BCRegasification!\\$B\\$7:\\$F\\$([0-9]+)").matcher(token);
                    if (!range.matches()) throw invalid();
                    token = "BCRegasification!$B$7:$F$" + Math.max(Integer.parseInt(range.group(1)), masterLast);
                } else if (filter[0]) {
                    for (var entry : appended.entrySet()) if (token.startsWith(entry.getKey() + "!")) {
                        if (!token.equals(entry.getKey() + "!" + entry.getValue().getKey())) throw invalid();
                        token = entry.getKey() + "!" + entry.getValue().getValue(); break;
                    }
                }
                try { output.write(token.getBytes(java.nio.charset.StandardCharsets.UTF_8)); } catch (java.io.IOException ex) { throw new java.io.UncheckedIOException(ex); }
            });
        };
    }

    static UnaryOperator<String> appendFilter(String previous, String next) {
        return token -> {
            if (!token.startsWith("<autoFilter ")) return token;
            if (!previous.equals(InputWorkbookXml.attribute(token, "ref"))) throw invalid();
            return InputWorkbookXml.replaceAttribute(token, "ref", next);
        };
    }

    static UnaryOperator<String> regasMasterFilter(int last) {
        return token -> {
            if (!token.startsWith("<autoFilter ")) return token;
            var range = java.util.regex.Pattern.compile("B7:F([0-9]+)").matcher(InputWorkbookXml.attribute(token, "ref"));
            if (!range.matches()) throw invalid();
            int previous = Integer.parseInt(range.group(1));
            return last <= previous ? token : InputWorkbookXml.replaceAttribute(token, "ref", "B7:F" + last);
        };
    }

    /** 기존 rule의 기준점/수식은 유지하고 새 입력 셀에 해당하는 적용 범위만 덧붙인다. */
    static UnaryOperator<String> rules(List<Copy> copies) {
        return token -> {
            if ((!token.startsWith("<conditionalFormatting ") && !token.startsWith("<dataValidation ")) || copies.isEmpty()) return token;
            String value = InputWorkbookXml.attribute(token, "sqref");
            List<org.apache.poi.ss.util.CellRangeAddress> original = new ArrayList<>();
            for (String part : value.trim().split("\\s+")) original.add(org.apache.poi.ss.util.CellRangeAddress.valueOf(part));
            java.util.LinkedHashSet<String> added = new java.util.LinkedHashSet<>();
            for (Copy copy : copies) for (var range : original) {
                int firstRow = Math.max(range.getFirstRow() + 1, copy.source.firstRow()), lastRow = Math.min(range.getLastRow() + 1, copy.source.lastRow());
                int firstColumn = Math.max(range.getFirstColumn() + 1, copy.source.firstColumn()), lastColumn = Math.min(range.getLastColumn() + 1, copy.source.lastColumn());
                if (firstRow > lastRow || firstColumn > lastColumn) continue;
                int r1 = firstRow + copy.rows - 1, r2 = lastRow + copy.rows - 1, c1 = firstColumn + copy.columns - 1, c2 = lastColumn + copy.columns - 1;
                if (r1 < 0 || r2 >= 1048576 || c1 < 0 || c2 >= 16384) throw invalid();
                if (original.stream().anyMatch(old -> old.getFirstRow() <= r1 && old.getLastRow() >= r2 && old.getFirstColumn() <= c1 && old.getLastColumn() >= c2)) continue;
                added.add(new org.apache.poi.ss.util.CellRangeAddress(r1, r2, c1, c2).formatAsString());
            }
            return added.isEmpty() ? token : InputWorkbookXml.replaceAttribute(token, "sqref", value + " " + String.join(" ", added));
        };
    }
    static UnaryOperator<String> columns(Path source, String sheet, Map<Integer, Integer> copies,
            InputWorkbookXml.Checkpoint checkpoint) throws Exception {
        if (copies.isEmpty()) return UnaryOperator.identity();
        String part = InputWorkbookXml.sheetParts(source).get(sheet); if (part == null) throw invalid();
        List<String> original = new ArrayList<>(); Map<Integer, String> templates = new HashMap<>(); boolean[] present = {false};
        try (ZipFile zip = new ZipFile(source.toFile()); var input = zip.getInputStream(zip.getEntry(part))) {
            InputWorkbookXml.scan(input, checkpoint, token -> {
                if (token.equals("<cols>")) present[0] = true;
                if (!token.startsWith("<col ")) return;
                original.add(token); int first = number(token, "min"), last = number(token, "max");
                for (int from : copies.values()) if (from >= first && from <= last) templates.put(from, token);
            });
        }
        TreeMap<Integer, String> output = new TreeMap<>();
        for (String column : original) {
            int first = number(column, "min"), last = number(column, "max"), begin = first;
            for (int destination : copies.keySet().stream().sorted().toList()) if (destination >= first && destination <= last) {
                if (begin < destination) output.put(begin, range(column, begin, destination - 1));
                begin = destination + 1;
            }
            if (begin <= last) output.put(begin, begin == first ? column : range(column, begin, last));
        }
        copies.forEach((to, from) -> {
            if (to < 1 || to > 16384 || from < 1 || from > 16384) throw invalid();
            output.put(to, range(templates.getOrDefault(from, "<col min=\"1\" max=\"1\"/>"), to, to));
        });
        String replacement = "<cols>" + String.join("", output.values()) + "</cols>";
        boolean[] inside = {false};
        return token -> {
            if (token.equals("<cols>")) { inside[0] = true; return replacement; }
            if (token.equals("</cols>")) { inside[0] = false; return ""; }
            if (inside[0]) return "";
            if (!present[0] && (token.equals("<sheetData>") || token.equals("<sheetData/>"))) return replacement + token;
            return token;
        };
    }
    private static int number(String xml, String name) { return Integer.parseInt(InputWorkbookXml.attribute(xml, name)); }
    private static String range(String xml, int first, int last) {
        return InputWorkbookXml.replaceAttribute(InputWorkbookXml.replaceAttribute(xml, "min", Integer.toString(first)), "max", Integer.toString(last));
    }
    private static InputDraftPreserveChange.Failure invalid() { return new InputDraftPreserveChange.Failure("STRUCTURE_VALIDATION_FAILED"); }
}
