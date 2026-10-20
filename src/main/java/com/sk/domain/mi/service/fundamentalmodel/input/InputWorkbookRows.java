package com.sk.domain.mi.service.fundamentalmodel.input;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

/** 승인된 입력 열만 새 논리 행에 생성한다. 옆 표/숨긴 Helper는 복제하지 않는다. */
final class InputWorkbookRows {
    private static final Pattern CONTENT = Pattern.compile("<(?:f|v|is)(?=\\s|/?>)");

    static int lastContentRow(Path source, String sheet, int afterRow, int firstColumn, int lastColumn,
            InputWorkbookXml.Checkpoint checkpoint) throws Exception {
        String part = InputWorkbookXml.sheetParts(source).get(sheet); if (part == null) throw invalid();
        int[] last = {afterRow};
        InputWorkbookXml.rows(source, part, checkpoint, row -> {
            if (row.number() <= afterRow) return;
            for (var cell : InputWorkbookXml.cells(row.xml()).entrySet())
                if (cell.getKey() >= firstColumn && cell.getKey() <= lastColumn && CONTENT.matcher(cell.getValue()).find()) last[0] = Math.max(last[0], row.number());
        });
        return last[0];
    }

    static InputWorkbookXml.SheetPatch append(Path source, String sheet, int styleRow,
            Map<Integer, Map<Integer, Object>> rows, InputWorkbookXml.Checkpoint checkpoint) throws Exception {
        return append(source, sheet, styleRow, rows, null, checkpoint);
    }

    static InputWorkbookXml.SheetPatch append(Path source, String sheet, int styleRow,
            Map<Integer, Map<Integer, Object>> rows, InputWorkbookXml.SheetPatch relocated, InputWorkbookXml.Checkpoint checkpoint) throws Exception {
        return append(source, sheet, styleRow, rows, relocated, Map.of(), checkpoint);
    }

    static InputWorkbookXml.SheetPatch append(Path source, String sheet, int styleRow,
            Map<Integer, Map<Integer, Object>> rows, InputWorkbookXml.SheetPatch relocated,
            Map<Integer, Integer> addedColumnTemplates, InputWorkbookXml.Checkpoint checkpoint) throws Exception {
        if (rows.isEmpty() || rows.size() > 100 || rows.containsKey(styleRow) && relocated == null) throw invalid();
        String part = InputWorkbookXml.sheetParts(source).get(sheet); if (part == null) throw invalid();
        Map<Integer, String> templates = new HashMap<>(); Map<Integer, Map<Integer, String>> existing = new HashMap<>();
        String[] opening = {null};
        InputWorkbookXml.rows(source, part, checkpoint, row -> {
            if (row.number() == styleRow) { opening[0] = row.xml().substring(0, row.xml().indexOf('>') + 1); templates.putAll(InputWorkbookXml.cells(row.xml())); }
            if (rows.containsKey(row.number())) existing.put(row.number(), InputWorkbookXml.cells(row.xml()));
        });
        if (opening[0] == null) throw invalid();
        Map<Integer, String> addedStyles = new HashMap<>();
        addedColumnTemplates.forEach((column, template) -> addedStyles.put(column, templates.get(template)));
        if (relocated != null) templates.putAll(relocated.raw.getOrDefault(styleRow, Map.of()));
        var patch = new InputWorkbookXml.SheetPatch();
        for (var row : rows.entrySet()) {
            checkpoint.check(); if (row.getKey() <= styleRow && relocated == null) throw invalid();
            patch.rowStyle(row.getKey(), opening[0]);
            for (var cell : row.getValue().entrySet()) {
                String destination = existing.getOrDefault(row.getKey(), Map.of()).get(cell.getKey());
                if (relocated != null && relocated.raw.getOrDefault(row.getKey(), Map.of()).containsKey(cell.getKey())) destination = relocated.raw.get(row.getKey()).get(cell.getKey());
                if (destination != null && CONTENT.matcher(destination).find()) throw invalid();
                // 빈 슬롯의 기존 General/서식을 먼저 유지한다. 실제 신규 셀만 이전 입력 셀의 서식을 사용한다.
                String style = destination == null ? addedStyles.getOrDefault(cell.getKey(), templates.get(cell.getKey())) : destination;
                patch.put(row.getKey(), cell.getKey(), new InputWorkbookXml.Edit(cell.getValue(), style, false));
            }
        }
        return patch;
    }

    private static InputDraftPreserveChange.Failure invalid() { return new InputDraftPreserveChange.Failure("STRUCTURE_VALIDATION_FAILED"); }
}
