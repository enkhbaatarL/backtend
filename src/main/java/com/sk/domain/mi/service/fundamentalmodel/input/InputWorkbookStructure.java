package com.sk.domain.mi.service.fundamentalmodel.input;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Pattern;
import java.util.zip.ZipFile;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.util.XMLHelper;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.helpers.DefaultHandler;

/** 승인된 논리 표 영역의 이동. 범위 밖 셀/행을 옮기지 않으며 shared formula와 chain을 함께 이동한다. */
final class InputWorkbookStructure {
    record Area(int firstRow, int lastRow, int firstColumn, int lastColumn) {
        Area {
            if (firstRow < 1 || lastRow < firstRow || lastRow > 1048576 || firstColumn < 1 || lastColumn < firstColumn || lastColumn > 16384) throw invalid();
        }
        boolean contains(int row, int column) { return row >= firstRow && row <= lastRow && column >= firstColumn && column <= lastColumn; }
    }
    record Move(Area area, int rows, int columns) {}
    record ChainChange(String sheetId, Map<String, String> moves, Set<String> removed, List<String> added) {}
    record Result(InputWorkbookXml.SheetPatch sheet, ChainChange chainChange) {
        InputWorkbookXml.PartPatch chain() { return InputWorkbookStructure.chain(List.of(chainChange)); }
    }
    private record Formula(String address, String xml, String text, String group) {}
    private record Member(String source, String target, String xml, String text) {}
    private static final Pattern FORMULA = Pattern.compile("<f(?=\\s|/?>)[^>]*?(?:/>|>.*?</f>)", Pattern.DOTALL);

    static Result move(Path source, String sheet, List<Move> moves, List<Area> deleted, InputWorkbookXml.Checkpoint checkpoint) throws Exception {
        String part = InputWorkbookXml.sheetParts(source).get(sheet);
        if (part == null) throw invalid();
        Map<String, Formula> masters = new HashMap<>(); Set<String> touched = new HashSet<>();
        int[] maximumGroup = {-1};
        // 첫 순회에서는 공유 master와 변경에 관계된 group ID만 보관한다.
        InputWorkbookXml.rows(source, part, checkpoint, row -> InputWorkbookXml.cells(row.xml()).forEach((column, xml) -> {
            Formula f = formula(xml);
            if (f != null && Set.of("array", "dataTable").contains(java.util.Objects.toString(optionalAttribute(f.xml, "t"), ""))) {
                String reference = optionalAttribute(f.xml, "ref");
                var range = org.apache.poi.ss.util.CellRangeAddress.valueOf(reference == null ? f.address : reference);
                Area area = new Area(range.getFirstRow() + 1, range.getLastRow() + 1, range.getFirstColumn() + 1, range.getLastColumn() + 1);
                if (moves.stream().anyMatch(move -> intersects(area, move.area)) || deleted.stream().anyMatch(removed -> intersects(area, removed))) throw invalid();
            }
            if (f == null || f.group == null) return;
            maximumGroup[0] = Math.max(maximumGroup[0], Integer.parseInt(f.group));
            if (f.text != null && masters.putIfAbsent(f.group, f) != null) throw invalid();
            if (selected(row.number(), column, moves, deleted)) touched.add(f.group);
        }));
        InputWorkbookXml.SheetPatch patch = new InputWorkbookXml.SheetPatch();
        Map<String, List<Member>> groups = new LinkedHashMap<>();
        Map<String, String> chainMoves = new HashMap<>(); Set<String> chainDeleted = new HashSet<>();
        long[] bytes = {0}; Set<String> destinations = new HashSet<>();
        Map<String, String> relocatedCells = new HashMap<>();
        // 셀 이동은 원본에서 동시에 읽는다. 목적지 덮어쓰기 때문에 다음 셀의 원본을 잃지 않는다.
        InputWorkbookXml.rows(source, part, checkpoint, row -> InputWorkbookXml.cells(row.xml()).forEach((column, xml) -> {
            int r = row.number(); String address = InputWorkbookXml.address(r, column);
            Move move = moves.stream().filter(m -> m.area.contains(r, column)).findFirst().orElse(null);
            if (moves.stream().filter(m -> m.area.contains(r, column)).count() > 1) throw invalid();
            boolean remove = deleted.stream().anyMatch(a -> a.contains(r, column));
            if (move != null && remove) throw invalid();
            Formula f = formula(xml);
            if (move != null || remove) {
                patch.raw(r, column, InputWorkbookXml.valueCell(xml, address, null));
            }
            if (remove) { if (f != null) chainDeleted.add(address); return; }
            if (move == null && (f == null || f.group == null || !touched.contains(f.group))) return;
            int nextRow = r + (move == null ? 0 : move.rows), nextColumn = column + (move == null ? 0 : move.columns);
            if (nextRow < 1 || nextRow > 1048576 || nextColumn < 1 || nextColumn > 16384) throw invalid();
            String target = InputWorkbookXml.address(nextRow, nextColumn);
            if (!destinations.add(target)) throw invalid();
            String relocated = InputWorkbookXml.replaceAttribute(xml, "r", target);
            String text = f == null ? null : expanded(f, masters);
            if (f != null) {
                text = InputWorkbookFormula.translate(text, nextRow - r, nextColumn - column);
                if (!target.equals(address)) chainMoves.put(address, target);
            }
            if (f != null && f.group != null) groups.computeIfAbsent(f.group, ignored -> new ArrayList<>()).add(new Member(address, target, relocated, text));
            else {
                if (f != null) relocated = replaceFormula(relocated, "<f>" + InputWorkbookXml.escape(text) + "</f>");
                relocatedCells.put(target, relocated);
            }
            bytes[0] += relocated.length() * 2L;
            if (bytes[0] > 64L * 1024 * 1024) throw invalid();
        }));
        // 마지막 빈 슬롯보다 생존 셀을 나중에 배치한다.
        relocatedCells.forEach((target, xml) -> put(patch, target, xml));
        for (var group : groups.entrySet()) {
            // 이동으로 끊긴 shared 영역은 같은 열 묶음/연속 행의 사각형들로 분할한다.
            TreeMap<Integer, TreeMap<Integer, Member>> rows = new TreeMap<>();
            for (Member member : group.getValue()) {
                CellReference a = new CellReference(member.target);
                rows.computeIfAbsent(a.getRow() + 1, ignored -> new TreeMap<>()).put((int) a.getCol() + 1, member);
            }
            List<List<Member>> rectangles = new ArrayList<>(); List<Member> rectangle = new ArrayList<>();
            Set<Integer> previousColumns = null; int previousRow = -1;
            for (var row : rows.entrySet()) {
                Set<Integer> columns = row.getValue().keySet();
                if (columns.size() != row.getValue().lastKey() - row.getValue().firstKey() + 1) throw invalid();
                if (!rectangle.isEmpty() && (row.getKey() != previousRow + 1 || !columns.equals(previousColumns))) { rectangles.add(rectangle); rectangle = new ArrayList<>(); }
                rectangle.addAll(row.getValue().values()); previousColumns = columns; previousRow = row.getKey();
            }
            if (!rectangle.isEmpty()) rectangles.add(rectangle);
            for (int i = 0; i < rectangles.size(); i++) {
                List<Member> members = rectangles.get(i); String id = i == 0 ? group.getKey() : Integer.toString(++maximumGroup[0]);
                String ref = members.get(0).target + ":" + members.get(members.size() - 1).target;
                for (int n = 0; n < members.size(); n++) {
                    Member member = members.get(n);
                    var first = new CellReference(members.get(0).target); var current = new CellReference(member.target);
                    if (!member.text.equals(InputWorkbookFormula.translate(members.get(0).text, current.getRow() - first.getRow(), current.getCol() - first.getCol()))) throw invalid();
                    String element = n == 0 ? "<f t=\"shared\" ref=\"" + ref + "\" si=\"" + id + "\">" + InputWorkbookXml.escape(member.text) + "</f>"
                            : "<f t=\"shared\" si=\"" + id + "\"/>";
                    put(patch, member.target, replaceFormula(member.xml, element));
                }
            }
        }
        String sheetId = sheetId(source, sheet);
        return new Result(patch, new ChainChange(sheetId, Map.copyOf(chainMoves), Set.copyOf(chainDeleted), List.of()));
    }

    private static boolean selected(int row, int column, List<Move> moves, List<Area> deleted) {
        return moves.stream().anyMatch(m -> m.area.contains(row, column)) || deleted.stream().anyMatch(a -> a.contains(row, column));
    }
    private static boolean intersects(Area a, Area b) { return a.firstRow <= b.lastRow && a.lastRow >= b.firstRow && a.firstColumn <= b.lastColumn && a.lastColumn >= b.firstColumn; }
    private static void put(InputWorkbookXml.SheetPatch patch, String address, String xml) {
        var ref = new CellReference(address); patch.raw(ref.getRow() + 1, ref.getCol() + 1, xml);
    }
    private static Formula formula(String cell) {
        var match = FORMULA.matcher(cell); if (!match.find()) return null;
        String f = match.group();
        String group = f.contains("t=\"shared\"") || f.contains("t='shared'") ? InputWorkbookXml.attribute(f, "si") : null;
        String text = f.endsWith("/>") ? null : f.substring(f.indexOf('>') + 1, f.lastIndexOf("</f>"));
        if (text != null) {
            var entities = Pattern.compile("&#(x[0-9a-fA-F]+|[0-9]+);").matcher(text); StringBuffer decoded = new StringBuffer();
            while (entities.find()) {
                String digits = entities.group(1); int code = Integer.parseInt(digits.startsWith("x") ? digits.substring(1) : digits, digits.startsWith("x") ? 16 : 10);
                entities.appendReplacement(decoded, java.util.regex.Matcher.quoteReplacement(new String(Character.toChars(code))));
            }
            entities.appendTail(decoded); text = decoded.toString().replace("&lt;", "<").replace("&gt;", ">").replace("&quot;", "\"").replace("&apos;", "'").replace("&amp;", "&");
        }
        return new Formula(InputWorkbookXml.attribute(cell, "r"), f, text, group);
    }
    private static String expanded(Formula f, Map<String, Formula> masters) {
        if (f.text != null) return f.text;
        Formula master = masters.get(f.group); if (master == null) throw invalid();
        var from = new CellReference(master.address); var to = new CellReference(f.address);
        return InputWorkbookFormula.translate(master.text, to.getRow() - from.getRow(), to.getCol() - from.getCol());
    }
    private static String replaceFormula(String cell, String formula) { return FORMULA.matcher(cell).replaceFirst(java.util.regex.Matcher.quoteReplacement(formula)); }

    static Map<String, String> formulas(Path source, String sheet, java.util.function.Predicate<String> selected,
            InputWorkbookXml.Checkpoint checkpoint) throws Exception {
        String part = InputWorkbookXml.sheetParts(source).get(sheet); if (part == null) throw invalid();
        Map<String, Formula> masters = new HashMap<>(), requested = new LinkedHashMap<>();
        InputWorkbookXml.rows(source, part, checkpoint, row -> InputWorkbookXml.cells(row.xml()).forEach((column, xml) -> {
            Formula formula = formula(xml); if (formula == null) return;
            if (formula.group != null && formula.text != null && masters.putIfAbsent(formula.group, formula) != null) throw invalid();
            if (selected.test(formula.address)) {
                if (Set.of("array", "dataTable").contains(java.util.Objects.toString(optionalAttribute(formula.xml, "t"), ""))) throw invalid();
                requested.put(formula.address, formula); if (requested.size() > 250000) throw invalid();
            }
        }));
        Map<String, String> result = new LinkedHashMap<>(); requested.forEach((address, formula) -> result.put(address, expanded(formula, masters)));
        return result;
    }

    private static String optionalAttribute(String element, String attribute) {
        try { return InputWorkbookXml.attribute(element, attribute); }
        catch (InputDraftPreserveChange.Failure ignored) { return null; }
    }

    static InputWorkbookXml.PartPatch chain(String sheetId, Map<String, String> moves, Set<String> removed) {
        return chain(sheetId, moves, removed, List.of());
    }

    static InputWorkbookXml.PartPatch chain(String sheetId, Map<String, String> moves, Set<String> removed, List<String> added) {
        return chain(List.of(new ChainChange(sheetId, moves, removed, added)));
    }

    static InputWorkbookXml.PartPatch chain(List<ChainChange> changes) {
        return chain(changes, null, null);
    }

    /** 신규 선행 수식이 지정 합계보다 먼저 오도록 삽입한다. 기존 항목의 상대 순서는 유지한다. */
    static InputWorkbookXml.PartPatch chain(List<ChainChange> changes, String beforeSheet, Area beforeArea) {
        Map<String, Map<String, String>> moves = new LinkedHashMap<>();
        Map<String, Set<String>> removed = new LinkedHashMap<>();
        Map<String, List<String>> added = new LinkedHashMap<>();
        for (ChainChange change : changes) {
            change.moves.forEach((from, to) -> { if (moves.computeIfAbsent(change.sheetId, ignored -> new HashMap<>()).putIfAbsent(from, to) != null) throw invalid(); });
            removed.computeIfAbsent(change.sheetId, ignored -> new HashSet<>()).addAll(change.removed);
            added.computeIfAbsent(change.sheetId, ignored -> new ArrayList<>()).addAll(change.added);
        }
        return (input, output, checkpoint) -> {
            String[] current = {null}, saved = {null};
            Set<String> existing = new HashSet<>(), created = new HashSet<>(); boolean[] inserted = {false};
            java.util.function.Supplier<String> newEntries = () -> {
                StringBuilder result = new StringBuilder();
                for (var entry : added.entrySet()) for (String address : entry.getValue()) {
                    String key = entry.getKey() + "/" + address;
                    if (!existing.add(key) || !created.add(key)) throw invalid();
                    result.append("<c r=\"").append(InputWorkbookXml.escape(address)).append("\" i=\"").append(entry.getKey()).append("\"/>");
                    saved[0] = entry.getKey();
                }
                inserted[0] = true; return result.toString();
            };
            InputWorkbookXml.scan(input, checkpoint, token -> {
                if (token.startsWith("<c ")) {
                    String explicit = null;
                    try { explicit = InputWorkbookXml.attribute(token, "i"); } catch (InputDraftPreserveChange.Failure ignored) { }
                    if (explicit != null) current[0] = explicit;
                    if (current[0] == null) throw invalid();
                    String address = InputWorkbookXml.attribute(token, "r");
                    if (removed.getOrDefault(current[0], Set.of()).contains(address)) return;
                    String destination = moves.getOrDefault(current[0], Map.of()).get(address);
                    if (destination != null) token = InputWorkbookXml.replaceAttribute(token, "r", destination);
                    String position = destination == null ? address : destination;
                    if (!inserted[0] && beforeSheet != null && beforeSheet.equals(current[0])) {
                        var ref = new CellReference(position);
                        if (beforeArea.contains(ref.getRow() + 1, ref.getCol() + 1)) {
                            try { output.write(newEntries.get().getBytes(StandardCharsets.UTF_8)); } catch (java.io.IOException ex) { throw new java.io.UncheckedIOException(ex); }
                        }
                    }
                    if (created.contains(current[0] + "/" + position)) throw invalid();
                    if (explicit == null && !current[0].equals(saved[0])) token = token.replaceFirst("<c ", "<c i=\"" + current[0] + "\" ");
                    saved[0] = current[0];
                    if (!added.getOrDefault(current[0], List.of()).isEmpty()) existing.add(current[0] + "/" + InputWorkbookXml.attribute(token, "r"));
                }
                if (token.equals("</calcChain>")) {
                    if (!inserted[0] && beforeSheet != null && added.values().stream().anyMatch(list -> !list.isEmpty())) throw invalid();
                    if (!inserted[0]) token = newEntries.get() + token;
                }
                try { output.write(token.getBytes(StandardCharsets.UTF_8)); } catch (java.io.IOException ex) { throw new java.io.UncheckedIOException(ex); }
            });
        };
    }
    static String sheetId(Path file, String sheet) throws Exception {
        String[] id = {null};
        try (ZipFile zip = new ZipFile(file.toFile()); InputStream input = zip.getInputStream(zip.getEntry("xl/workbook.xml"))) {
            var parser = XMLHelper.newXMLReader(); parser.setContentHandler(new DefaultHandler() {
                @Override public void startElement(String uri, String local, String name, Attributes a) {
                    if (local.equals("sheet") && sheet.equals(a.getValue("name"))) id[0] = a.getValue("sheetId");
                }
            }); parser.parse(new InputSource(input));
        }
        if (id[0] == null) throw invalid(); return id[0];
    }
    private static InputDraftPreserveChange.Failure invalid() { return new InputDraftPreserveChange.Failure("STRUCTURE_VALIDATION_FAILED"); }
}
