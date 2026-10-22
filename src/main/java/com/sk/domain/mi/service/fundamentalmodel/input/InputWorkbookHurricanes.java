package com.sk.domain.mi.service.fundamentalmodel.input;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipFile;
import org.apache.poi.util.XMLHelper;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.helpers.DefaultHandler;

/** 승인된 [1]LUT의 파일 내부 cache 8셀만 읽는다. 외부 relationship 대상은 열지 않는다. */
final class InputWorkbookHurricanes {
    record Adjustment(Set<String> calculated, List<String> added) {}
    private static final String SHEET = "BCHurricanes";
    private static final Set<Integer> ROWS = Set.of(8, 9, 10, 11, 16, 17, 18, 19);
    private static final String EXTERNAL = "[1]LUT!$AH$14:$AI$17";

    static Adjustment adjustment(Path source, int previousEnd, int end, Set<Integer> created,
            InputWorkbookXml.SheetPatch patch, InputWorkbookReader reader, InputWorkbookXml.Checkpoint checkpoint) throws Exception {
        String sheet = "{USA}Hurricanes";
        Set<Integer> affected = new java.util.TreeSet<>();
        patch.edits.forEach((row, cells) -> { if (row >= 13 && row <= end && (created.contains(row) || cells.containsKey(2) || cells.containsKey(4))) affected.add(row); });
        if (affected.isEmpty()) return new Adjustment(Set.of(), List.of());
        Set<String> targets = new java.util.HashSet<>(Set.of("I3")); affected.forEach(row -> targets.add("E" + row));
        Map<String, String> selected = InputWorkbookStructure.formulas(source, sheet, address -> targets.contains(address) || address.equals("E13"), checkpoint);
        String check = "IF(AND(B13<>0,D13=0),\"Error - enter value\",\" \")";
        String total = (previousEnd - 7) + "-COUNTIFS($E$8:$E$" + previousEnd + ",\" \")";
        if (!check.equals(selected.get("E13")) || !total.equals(selected.get("I3"))) throw failed();
        Map<String, String> raw = new HashMap<>();
        InputWorkbookXml.rows(source, InputWorkbookXml.sheetParts(source).get(sheet), checkpoint, row -> InputWorkbookXml.cells(row.xml()).forEach((column, cell) -> {
            String address = InputWorkbookXml.address(row.number(), column); if (targets.contains(address) || address.equals("E13")) raw.put(address, cell);
        }));
        Map<InputWorkbookCalculation.Address, String> formulas = new LinkedHashMap<>();
        Map<InputWorkbookCalculation.Address, Object> constants = new LinkedHashMap<>(); List<String> added = new java.util.ArrayList<>();
        for (int row : affected) {
            String address = "E" + row, formula = selected.get(address), template = raw.get(address);
            if (created.contains(row)) {
                String expected = InputWorkbookFormula.translate(check, row - 13, 0);
                if (formula != null) { if (!expected.equals(formula)) throw failed(); }
                else {
                    if (template != null && java.util.regex.Pattern.compile("<(?:f|v|is)(?=\\s|/?>)").matcher(template).find()) throw failed();
                    template = formulaCell(template == null ? raw.get("E13") : template, address, expected); added.add(address);
                }
                formula = expected;
            }
            if (formula == null || template == null) throw failed();
            formulas.put(new InputWorkbookCalculation.Address(sheet, address), formula); patch.raw(row, 5, template);
            for (int column : List.of(2, 4)) constants.put(new InputWorkbookCalculation.Address(sheet, InputWorkbookXml.address(row, column)), null);
        }
        for (int row = 8; row <= end; row++) if (!affected.contains(row)) constants.put(new InputWorkbookCalculation.Address(sheet, "E" + row), null);
        if (end != previousEnd) total = (end - 7) + "-COUNTIFS($E$8:$E$" + end + ",\" \")";
        formulas.put(new InputWorkbookCalculation.Address(sheet, "I3"), total);
        patch.raw(3, 9, end == previousEnd ? raw.get("I3") : formulaCell(raw.get("I3"), "I3", total));
        reader.readLocal(source, List.of(new InputWorkbookReader.Window(sheet, row -> row >= 8 && row <= end, Set.of(2, 4, 5), (row, cells) -> cells.forEach((column, cell) -> {
            var address = new InputWorkbookCalculation.Address(sheet, InputWorkbookXml.address(row, column)); if (constants.containsKey(address)) constants.put(address, cell.raw());
        }))));
        patch.edits.forEach((row, cells) -> cells.forEach((column, edit) -> {
            var address = new InputWorkbookCalculation.Address(sheet, InputWorkbookXml.address(row, column)); if (constants.containsKey(address)) constants.put(address, edit.value());
        }));
        InputWorkbookCalculation.evaluate(constants, formulas, checkpoint).forEach((address, value) -> {
            var ref = new org.apache.poi.ss.util.CellReference(address.cell()); int row = ref.getRow() + 1, column = ref.getCol() + 1;
            patch.raw(row, column, InputWorkbookCalculation.cache(patch.raw.get(row).get(column), value));
        });
        return new Adjustment(Set.copyOf(targets), List.copyOf(added));
    }

    private static String formulaCell(String template, String address, String formula) {
        if (template == null) throw failed();
        return InputWorkbookXml.valueCell(template, address, null).replace("</c>", "<f>" + InputWorkbookXml.escape(formula) + "</f><v>0</v></c>");
    }

    static Set<Integer> calculate(Path source, InputWorkbookXml.SheetPatch patch, InputWorkbookReader reader,
            InputWorkbookXml.Checkpoint checkpoint) throws Exception {
        Map<InputWorkbookCalculation.Address, Object> constants = lut(source, checkpoint);
        for (int row : ROWS) for (int column = 3; column <= 6; column++) constants.put(new InputWorkbookCalculation.Address(SHEET, InputWorkbookXml.address(row, column)), null);
        reader.readLocal(source, List.of(new InputWorkbookReader.Window(SHEET, ROWS::contains, Set.of(3, 4, 5, 6), (row, cells) -> cells.forEach((column, cell) ->
            constants.put(new InputWorkbookCalculation.Address(SHEET, InputWorkbookXml.address(row, column)), cell.raw())))));
        patch.edits.forEach((row, cells) -> cells.forEach((column, edit) -> {
            var address = new InputWorkbookCalculation.Address(SHEET, InputWorkbookXml.address(row, column));
            if (constants.containsKey(address)) constants.put(address, edit.value());
        }));
        Map<String, String> raw = new HashMap<>();
        InputWorkbookXml.rows(source, InputWorkbookXml.sheetParts(source).get(SHEET), checkpoint, row -> {
            if (ROWS.contains(row.number())) raw.put("G" + row.number(), InputWorkbookXml.cells(row.xml()).get(7));
        });
        Map<String, String> selected = InputWorkbookStructure.formulas(source, SHEET, raw::containsKey, checkpoint);
        Map<InputWorkbookCalculation.Address, String> formulas = new LinkedHashMap<>();
        for (int row : ROWS) {
            String expected = "D" + row + "*VLOOKUP(C" + row + "," + EXTERNAL + ",2,FALSE)+F" + row + "*VLOOKUP(E" + row + "," + EXTERNAL + ",2,FALSE)";
            if (!expected.equals(selected.get("G" + row)) || raw.get("G" + row) == null) throw failed();
            formulas.put(new InputWorkbookCalculation.Address(SHEET, "G" + row), expected.replace(EXTERNAL, "LUT!$AH$14:$AI$17"));
        }
        InputWorkbookCalculation.evaluate(constants, formulas, checkpoint).forEach((address, value) -> {
            int row = Integer.parseInt(address.cell().substring(1)); patch.raw(row, 7, InputWorkbookCalculation.cache(raw.get(address.cell()), value));
        });
        return ROWS;
    }

    private static Map<InputWorkbookCalculation.Address, Object> lut(Path source, InputWorkbookXml.Checkpoint checkpoint) throws Exception {
        try (ZipFile zip = new ZipFile(source.toFile())) {
            List<String> references = new java.util.ArrayList<>();
            read(zip, "xl/workbook.xml", new DefaultHandler() {
                @Override public void startElement(String uri, String local, String qName, Attributes attributes) {
                    checkpoint.check(); if (local.equals("externalReference")) references.add(attributes.getValue("http://schemas.openxmlformats.org/officeDocument/2006/relationships", "id"));
                }
            });
            if (references.isEmpty() || references.get(0) == null) throw failed();
            String[] part = {null};
            read(zip, "xl/_rels/workbook.xml.rels", new DefaultHandler() {
                @Override public void startElement(String uri, String local, String qName, Attributes attributes) {
                    checkpoint.check(); if (!local.equals("Relationship") || !references.get(0).equals(attributes.getValue("Id"))) return;
                    if (part[0] != null || !"http://schemas.openxmlformats.org/officeDocument/2006/relationships/externalLink".equals(attributes.getValue("Type"))
                            || "External".equals(attributes.getValue("TargetMode"))) throw failed();
                    String target = attributes.getValue("Target");
                    if (target == null || !target.matches("(?:/xl/)?externalLinks/[A-Za-z0-9_.-]+\\.xml")) throw failed();
                    part[0] = target.startsWith("/") ? target.substring(1) : "xl/" + target;
                }
            });
            if (part[0] == null) throw failed();
            Map<InputWorkbookCalculation.Address, Object> result = new LinkedHashMap<>();
            read(zip, part[0], new DefaultHandler() {
                int index, lut = -1; boolean selected, value; String address, type; StringBuilder text;
                @Override public void startElement(String uri, String local, String qName, Attributes attributes) {
                    checkpoint.check();
                    if (local.equals("sheetName")) { if ("LUT".equals(attributes.getValue("val"))) { if (lut != -1) throw failed(); lut = index; } index++; }
                    else if (local.equals("sheetData")) selected = lut >= 0 && Integer.toString(lut).equals(attributes.getValue("sheetId"));
                    else if (local.equals("cell") && selected) { String candidate = attributes.getValue("r"); address = candidate != null && candidate.matches("A[HI]1[4-7]") ? candidate : null; type = attributes.getValue("t"); text = new StringBuilder(); }
                    else if (local.equals("v") && address != null) value = true;
                }
                @Override public void characters(char[] characters, int start, int length) { if (value) text.append(characters, start, length); }
                @Override public void endElement(String uri, String local, String qName) {
                    if (local.equals("v")) value = false;
                    else if (local.equals("cell") && address != null) {
                        Object cached;
                        if (address.startsWith("AH")) { if (!"str".equals(type) || text.isEmpty()) throw failed(); cached = text.toString(); }
                        else { if (type != null && !type.equals("n") || text.isEmpty()) throw failed(); cached = new BigDecimal(text.toString()); }
                        if (result.putIfAbsent(new InputWorkbookCalculation.Address("LUT", address), cached) != null) throw failed(); address = null;
                    } else if (local.equals("sheetData")) selected = false;
                }
            });
            if (result.size() != 8) throw failed();
            return result;
        }
    }
    private static void read(ZipFile zip, String part, DefaultHandler handler) throws Exception {
        var entry = zip.getEntry(part); if (entry == null) throw failed();
        try (var input = zip.getInputStream(entry)) { var parser = XMLHelper.newXMLReader(); parser.setContentHandler(handler); parser.parse(new InputSource(input)); }
    }
    private static InputDraftPreserveChange.Failure failed() { return new InputDraftPreserveChange.Failure("CALCULATION_FAILED"); }
}
