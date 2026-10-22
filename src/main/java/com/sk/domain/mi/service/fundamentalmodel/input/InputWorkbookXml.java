package com.sk.domain.mi.service.fundamentalmodel.input;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.openxml4j.opc.PackageAccess;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.xssf.eventusermodel.XSSFReader;

/** 원본 package를 복제하면서 명시된 셀만 바꾼다. 한 번에 한 XML 행만 읽는다. */
final class InputWorkbookXml {
    private static final int ROW_LIMIT = 16 * 1024 * 1024;
    private static final Pattern CELL = Pattern.compile("<c(?=\\s|>)(?:[^>\"']|\"[^\"]*\"|'[^']*')*?(?:/>|>.*?</c>)", Pattern.DOTALL);
    private static final Pattern CONTENT = Pattern.compile("<(?:v|is|f)(?=\\s|/?>)(?:[^>\"']|\"[^\"]*\"|'[^']*')*?(?:/>|>.*?</(?:v|is|f)>)", Pattern.DOTALL);

    interface Checkpoint { void check(); }
    interface PartPatch { void write(InputStream input, OutputStream output, Checkpoint checkpoint) throws Exception; }
    record Row(int number, String xml) {}
    record Edit(Object value, String template, boolean replaceFormula) {
        static Edit value(Object value) { return new Edit(value, null, false); }
    }

    /** 시스템에서 생성한 좌표만 받는 내부 plan. 사용자 요청의 sheet/path는 직접 전달하지 않는다. */
    static final class SheetPatch {
        final Map<Integer, Map<Integer, Edit>> edits = new TreeMap<>();
        final Map<Integer, Map<Integer, String>> raw = new TreeMap<>();
        final Map<Integer, String> newRowOpenings = new HashMap<>();
        java.util.function.UnaryOperator<String> metadata = java.util.function.UnaryOperator.identity();
        void put(int row, int column, Edit edit) {
            if (row < 1 || row > 1048576 || column < 1 || column > 16384) throw invalid();
            edits.computeIfAbsent(row, ignored -> new TreeMap<>()).put(column, edit);
        }
        void value(int row, int column, Object value) { put(row, column, Edit.value(value)); }
        void rowStyle(int row, String sourceRow) {
            StringBuilder opening = new StringBuilder("<row r=\"").append(row).append("\"");
            String source = sourceRow.substring(0, sourceRow.indexOf('>') + 1);
            for (String name : java.util.List.of("ht", "customHeight", "s", "customFormat")) {
                var match = Pattern.compile("\\s" + name + "=(?:\"([^\"]*)\"|'([^']*)')").matcher(source);
                if (match.find()) opening.append(' ').append(name).append("=\"").append(match.group(1) == null ? match.group(2) : match.group(1)).append('"');
            }
            newRowOpenings.put(row, opening.append('>').toString());
        }
        void raw(int row, int column, String xml) {
            if (row < 1 || row > 1048576 || column < 1 || column > 16384) throw invalid();
            if (xml != null && !address(row, column).equals(attribute(xml, "r"))) throw invalid();
            raw.computeIfAbsent(row, ignored -> new TreeMap<>()).put(column, xml);
            edits.computeIfAbsent(row, ignored -> new TreeMap<>());
        }
    }

    static Map<String, String> sheetParts(Path file) throws Exception {
        Map<String, String> result = new LinkedHashMap<>();
        try (OPCPackage pkg = OPCPackage.open(file.toFile(), PackageAccess.READ)) {
            var sheets = (XSSFReader.SheetIterator) new XSSFReader(pkg).getSheetsData();
            while (sheets.hasNext()) try (InputStream ignored = sheets.next()) {
                if (result.put(sheets.getSheetName(), sheets.getSheetPart().getPartName().getName().substring(1)) != null) throw invalid();
            }
        }
        return result;
    }

    static void rows(Path file, String part, Checkpoint checkpoint, Consumer<Row> accept) throws Exception {
        try (ZipFile zip = new ZipFile(file.toFile())) {
            ZipEntry entry = zip.getEntry(part);
            if (entry == null) throw invalid();
            try (InputStream input = zip.getInputStream(entry)) {
                scan(input, checkpoint, token -> {
                    if (token.startsWith("<row ")) accept.accept(new Row(Integer.parseInt(attribute(token, "r")), token));
                });
            }
        }
    }

    static Map<Integer, String> cells(String row) {
        Map<Integer, String> result = new TreeMap<>();
        var matcher = CELL.matcher(row);
        while (matcher.find()) {
            String cell = matcher.group();
            int column = new CellReference(attribute(cell, "r")).getCol() + 1;
            if (result.putIfAbsent(column, cell) != null) throw invalid();
        }
        return result;
    }

    static void write(Path source, Path target, Map<String, SheetPatch> sheets, Checkpoint checkpoint) throws Exception {
        write(source, target, sheets, Map.of(), checkpoint);
    }

    static void write(Path source, Path target, Map<String, SheetPatch> sheets, Map<String, PartPatch> metadata, Checkpoint checkpoint) throws Exception {
        if (Files.exists(target) && Files.isSameFile(source, target)) throw invalid();
        InputDraftDiagnostics.timedChecked(InputDraftDiagnostics.Stage.WORKBOOK_SOURCE_VALIDATE, null, () -> {
            InputDraftFinalizationFiles.validatePackage(source, checkpoint::check); return null;
        });
        Map<String, String> parts = sheetParts(source);
        if (!parts.keySet().containsAll(sheets.keySet())) throw invalid();
        Map<String, SheetPatch> patches = new HashMap<>();
        sheets.forEach((sheet, patch) -> patches.put(parts.get(sheet), patch));
        Map<String, String> hashes = new HashMap<>();
        InputDraftDiagnostics.timedChecked(InputDraftDiagnostics.Stage.WORKBOOK_ZIP_WRITE, null, () -> {
            try (ZipFile zip = new ZipFile(source.toFile());
                    ZipOutputStream output = new ZipOutputStream(Files.newOutputStream(target))) {
                output.setComment(zip.getComment());
                for (String name : metadata.keySet()) if (zip.getEntry(name) == null) throw invalid();
                var entries = zip.entries();
                while (entries.hasMoreElements()) {
                    checkpoint.check();
                    ZipEntry entry = entries.nextElement();
                    SheetPatch patch = patches.get(entry.getName());
                    ZipEntry next;
                    PartPatch partPatch = metadata.get(entry.getName());
                    if (patch != null && partPatch != null) throw invalid();
                    if (patch == null && partPatch == null) {
                        next = new ZipEntry(entry);
                        if (next.getMethod() == ZipEntry.DEFLATED) next.setCompressedSize(-1);
                    } else {
                        next = new ZipEntry(entry.getName());
                        next.setTime(entry.getTime());
                        next.setComment(entry.getComment());
                        next.setExtra(entry.getExtra());
                        next.setMethod(entry.getMethod());
                        if (entry.getMethod() == ZipEntry.STORED) {
                            // STORED는 entry를 열기 전에 크기/CRC가 필요하다. 파일 전체 buffer 대신 동일 plan을 한 번 순차 계산한다.
                            var crc = new java.util.zip.CRC32(); long[] size = {0};
                            var counter = new OutputStream() {
                                @Override public void write(int value) { crc.update(value); size[0]++; }
                                @Override public void write(byte[] bytes, int offset, int length) { crc.update(bytes, offset, length); size[0] += length; }
                            };
                            try (InputStream input = zip.getInputStream(entry)) {
                                if (partPatch != null) partPatch.write(input, counter, checkpoint);
                                else rewrite(input, counter, patch, checkpoint);
                            }
                            next.setSize(size[0]); next.setCompressedSize(size[0]); next.setCrc(crc.getValue());
                        }
                    }
                    output.putNextEntry(next);
                    var digest = java.security.MessageDigest.getInstance("SHA-256");
                    var sink = new java.security.DigestOutputStream(output, digest);
                    try (InputStream input = zip.getInputStream(entry)) {
                        if (partPatch != null) partPatch.write(input, sink, checkpoint);
                        else if (patch == null) copy(input, sink, checkpoint);
                        else rewrite(input, sink, patch, checkpoint);
                    }
                    hashes.put(entry.getName(), java.util.HexFormat.of().formatHex(digest.digest()));
                    output.closeEntry();
                }
            }
            return null;
        });
        // 게시 전에 저장된 package의 CRC/XML 및 실제 저장 bytes를 확인한다.
        InputDraftDiagnostics.timedChecked(InputDraftDiagnostics.Stage.WORKBOOK_SAVED_VALIDATE, null, () -> {
            InputDraftFinalizationFiles.validatePackage(target, checkpoint::check); return null;
        });
        InputDraftDiagnostics.timedChecked(InputDraftDiagnostics.Stage.WORKBOOK_PRESERVATION_VERIFY, null, () -> {
            try (ZipFile saved = new ZipFile(target.toFile())) {
                if (saved.size() != hashes.size()) throw invalid();
                for (var expected : hashes.entrySet()) {
                    checkpoint.check();
                    try (InputStream input = saved.getInputStream(saved.getEntry(expected.getKey()))) {
                        if (!expected.getValue().equals(InputDraftFinalizationFiles.digest(input, checkpoint::check))) throw invalid();
                    }
                }
            }
            return null;
        });
    }

    private static void rewrite(InputStream input, OutputStream output, SheetPatch patch, Checkpoint checkpoint) throws Exception {
        TreeMap<Integer, Map<Integer, Edit>> pending = new TreeMap<>(patch.edits);
        Set<Integer> seen = new HashSet<>();
        scan(input, checkpoint, token -> {
            try {
                if (token.startsWith("<dimension ") && !pending.isEmpty()) token = dimension(token, patch);
                if (token.startsWith("<row ")) {
                    int row = Integer.parseInt(attribute(token, "r"));
                    if (!seen.add(row)) throw invalid();
                    while (!pending.isEmpty() && pending.firstKey() < row) {
                        var added = pending.pollFirstEntry();
                        output.write(rewriteRow(emptyRow(patch, added.getKey()), added.getKey(), added.getValue(), patch.raw.getOrDefault(added.getKey(), Map.of())).getBytes(StandardCharsets.UTF_8));
                    }
                    Map<Integer, Edit> changes = pending.remove(row);
                    if (changes != null) token = rewriteRow(token, row, changes, patch.raw.getOrDefault(row, Map.of()));
                } else if (token.equals("</sheetData>")) {
                    while (!pending.isEmpty()) {
                        var added = pending.pollFirstEntry();
                        output.write(rewriteRow(emptyRow(patch, added.getKey()), added.getKey(), added.getValue(), patch.raw.getOrDefault(added.getKey(), Map.of())).getBytes(StandardCharsets.UTF_8));
                    }
                } else if (token.equals("<sheetData/>") && !pending.isEmpty()) throw invalid();
                if (!token.startsWith("<row ")) token = patch.metadata.apply(token);
                output.write(token.getBytes(StandardCharsets.UTF_8));
            } catch (IOException ex) { throw new java.io.UncheckedIOException(ex); }
        });
        if (!pending.isEmpty()) throw invalid();
    }

    private static String emptyRow(SheetPatch patch, int row) { return patch.newRowOpenings.getOrDefault(row, "<row r=\"" + row + "\">") + "</row>"; }

    private static String dimension(String token, SheetPatch patch) {
        String old = attribute(token, "ref");
        var range = org.apache.poi.ss.util.CellRangeAddress.valueOf(old);
        int firstRow = range.getFirstRow(), lastRow = range.getLastRow();
        int firstCol = range.getFirstColumn(), lastCol = range.getLastColumn();
        for (var row : patch.edits.entrySet()) {
            firstRow = Math.min(firstRow, row.getKey() - 1); lastRow = Math.max(lastRow, row.getKey() - 1);
            for (int col : row.getValue().keySet()) { firstCol = Math.min(firstCol, col - 1); lastCol = Math.max(lastCol, col - 1); }
            for (int col : patch.raw.getOrDefault(row.getKey(), Map.of()).keySet()) { firstCol = Math.min(firstCol, col - 1); lastCol = Math.max(lastCol, col - 1); }
        }
        return replaceAttribute(token, "ref", new org.apache.poi.ss.util.CellRangeAddress(firstRow, lastRow, firstCol, lastCol).formatAsString());
    }

    private static String rewriteRow(String original, int row, Map<Integer, Edit> changes, Map<Integer, String> raw) {
        Map<Integer, String> cells = cells(original);
        raw.forEach((col, xml) -> { if (xml == null) cells.remove(col); else cells.put(col, xml); });
        for (var entry : changes.entrySet()) {
            int col = entry.getKey(); Edit edit = entry.getValue();
            String previous = cells.get(col);
            if (!edit.replaceFormula && previous != null && hasFormula(previous)) throw invalid();
            String template = edit.template == null ? previous : edit.template;
            cells.put(col, valueCell(template, address(row, col), edit.value));
        }
        int openingEnd = original.indexOf('>') + 1;
        String opening = original.substring(0, openingEnd).replaceAll("\\s+spans=(?:\"[^\"]*\"|'[^']*')", "");
        if (opening.endsWith("/>")) opening = opening.substring(0, opening.length() - 2) + ">";
        String trailing = original.endsWith("/>") ? "" : CELL.matcher(original.substring(openingEnd, original.lastIndexOf("</row>"))).replaceAll("");
        return opening + String.join("", cells.values()) + trailing + "</row>";
    }

    static boolean hasFormula(String cell) { return Pattern.compile("<f(?=\\s|/?>)").matcher(cell).find(); }

    static String valueCell(String template, String address, Object value) {
        String cell = template == null ? "<c r=\"" + address + "\"/>" : replaceAttribute(template, "r", address);
        int openingEnd = cell.indexOf('>') + 1;
        String opening = cell.substring(0, openingEnd).replaceAll("\\s+t=(?:\"[^\"]*\"|'[^']*')", "");
        if (opening.endsWith("/>")) opening = opening.substring(0, opening.length() - 2) + ">";
        String body = cell.endsWith("/>") ? "" : CONTENT.matcher(cell.substring(openingEnd, cell.lastIndexOf("</c>"))).replaceAll("");
        String content = "";
        if (value instanceof String text) {
            opening = opening.substring(0, opening.length() - 1) + " t=\"inlineStr\">";
            content = "<is><t xml:space=\"preserve\">" + escape(text) + "</t></is>";
        } else if (value instanceof BigDecimal decimal) content = "<v>" + decimal.toPlainString() + "</v>";
        else if (value instanceof java.math.BigInteger integer) content = "<v>" + integer + "</v>";
        else if (value instanceof Integer || value instanceof Long) content = "<v>" + value + "</v>";
        else if (value != null) throw invalid();
        return opening + content + body + "</c>";
    }

    static String address(int row, int column) { return CellReference.convertNumToColString(column - 1) + row; }
    static String attribute(String xml, String name) {
        var match = Pattern.compile("\\s" + name + "=(?:\"([^\"]*)\"|'([^']*)')").matcher(xml.substring(0, xml.indexOf('>') + 1));
        if (!match.find()) throw invalid();
        return match.group(1) == null ? match.group(2) : match.group(1);
    }
    static String replaceAttribute(String xml, String name, String value) {
        var match = Pattern.compile("(\\s" + name + "=)(?:\"[^\"]*\"|'[^']*')").matcher(xml);
        if (!match.find()) throw invalid();
        return xml.substring(0, match.start()) + match.group(1) + "\"" + escape(value) + "\"" + xml.substring(match.end());
    }
    static String escape(String value) {
        for (int i = 0; i < value.length(); i++) {
            int cp = value.codePointAt(i);
            if (!(cp == 9 || cp == 10 || cp == 13 || cp >= 32 && cp <= 0xd7ff || cp >= 0xe000 && cp <= 0xfffd || cp >= 0x10000 && cp <= 0x10ffff)) throw invalid();
            if (Character.isSupplementaryCodePoint(cp)) i++;
        }
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("\r", "&#13;");
    }

    /** XML lexical fragments를 그대로 전달한다. worksheet/행 전체 DOM과 전체 수식 적재를 피한다. */
    static void scan(InputStream input, Checkpoint checkpoint, Consumer<String> accept) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8.newDecoder()
                .onMalformedInput(java.nio.charset.CodingErrorAction.REPORT).onUnmappableCharacter(java.nio.charset.CodingErrorAction.REPORT)), 65536)) {
            StringBuilder token = new StringBuilder(); StringBuilder row = null;
            boolean tag = false; char quote = 0; int count = 0;
            for (int n; (n = reader.read()) != -1;) {
                if ((++count & 65535) == 0) checkpoint.check();
                char c = (char) n;
                if (c == '<' && !tag) {
                    if (!token.isEmpty()) { if (row == null) accept.accept(token.toString()); else row.append(token); token.setLength(0); }
                    tag = true;
                }
                token.append(c);
                if (tag) {
                    if (quote != 0) { if (c == quote) quote = 0; }
                    else if (c == '\'' || c == '"') quote = c;
                    else if (c == '>') {
                        tag = false; String part = token.toString(); token.setLength(0);
                        if (part.startsWith("<!")) throw invalid(); // OOXML에 불필요한 DTD/entity는 허용하지 않는다.
                        if (part.startsWith("<row ") && row == null) row = new StringBuilder();
                        if (row == null) accept.accept(part);
                        else { row.append(part); if (part.equals("</row>") || part.startsWith("<row ") && part.endsWith("/>")) { accept.accept(row.toString()); row = null; } }
                    }
                }
                if (token.length() + (row == null ? 0 : row.length()) > ROW_LIMIT) throw invalid();
            }
            if (row != null || tag) throw invalid();
            if (!token.isEmpty()) accept.accept(token.toString());
        }
    }
    private static void copy(InputStream input, OutputStream output, Checkpoint checkpoint) throws IOException {
        byte[] buffer = new byte[65536]; int n;
        while ((n = input.read(buffer)) != -1) { checkpoint.check(); output.write(buffer, 0, n); }
    }
    private static InputDraftPreserveChange.Failure invalid() { return new InputDraftPreserveChange.Failure("STRUCTURE_VALIDATION_FAILED"); }
}
