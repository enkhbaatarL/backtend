package com.sk.domain.mi.service.fundamentalmodel.input;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.poi.ss.util.CellReference;

/** BCInput의 기존 연결 수식만 계산한다. 보존 Helper와 신규 행은 계산/생성하지 않는다. */
final class InputWorkbookBcInput {
    private static final Map<Integer, List<Integer>> PRECEDENTS = Map.of(
        24, List.of(17, 20), 33, List.of(26, 27, 29), 41, List.of(38, 39));
    private static final Map<Integer, Integer> OWNERS = Map.of(24, 17, 33, 26, 41, 37);

    static Set<String> helpers(Path source, Map<Integer, Set<Integer>> originalRows, InputWorkbookXml.SheetPatch patch,
            InputWorkbookReader reader, InputWorkbookXml.Checkpoint checkpoint) throws Exception {
        Set<String> requested = new HashSet<>();
        patch.edits.forEach((row, cells) -> PRECEDENTS.forEach((helper, inputs) -> {
            if (originalRows.getOrDefault(OWNERS.get(helper), Set.of()).contains(row) && inputs.stream().anyMatch(cells::containsKey))
                requested.add(InputWorkbookXml.address(row, helper));
        }));
        if (requested.isEmpty()) return Set.of();
        Map<String, String> selected = InputWorkbookStructure.formulas(source, "BCInput", requested::contains, checkpoint);
        if (selected.isEmpty()) return Set.of();
        Map<InputWorkbookCalculation.Address, String> formulas = new LinkedHashMap<>();
        Map<InputWorkbookCalculation.Address, Object> constants = new LinkedHashMap<>();
        Set<Integer> rows = new HashSet<>(), columns = new HashSet<>();
        selected.forEach((address, formula) -> {
            var ref = new CellReference(address); int row = ref.getRow() + 1, column = ref.getCol() + 1;
            rows.add(row); formulas.put(new InputWorkbookCalculation.Address("BCInput", address), formula);
            for (int input : PRECEDENTS.get(column)) {
                columns.add(input); constants.put(new InputWorkbookCalculation.Address("BCInput", InputWorkbookXml.address(row, input)), null);
            }
        });
        reader.readLocal(source, List.of(new InputWorkbookReader.Window("BCInput", rows::contains, columns, (row, cells) -> cells.forEach((column, cell) -> {
            var address = new InputWorkbookCalculation.Address("BCInput", InputWorkbookXml.address(row, column));
            if (constants.containsKey(address)) constants.put(address, cell.raw());
        }))));
        patch.edits.forEach((row, cells) -> cells.forEach((column, edit) -> {
            var address = new InputWorkbookCalculation.Address("BCInput", InputWorkbookXml.address(row, column));
            if (constants.containsKey(address)) constants.put(address, edit.value());
        }));
        var calculated = InputWorkbookCalculation.evaluate(constants, formulas, checkpoint);
        Map<String, String> templates = new HashMap<>();
        InputWorkbookXml.rows(source, InputWorkbookXml.sheetParts(source).get("BCInput"), checkpoint, row -> {
            if (rows.contains(row.number())) InputWorkbookXml.cells(row.xml()).forEach((column, cell) -> {
                String address = InputWorkbookXml.address(row.number(), column); if (selected.containsKey(address)) templates.put(address, cell);
            });
        });
        calculated.forEach((address, value) -> {
            var ref = new CellReference(address.cell());
            patch.raw(ref.getRow() + 1, ref.getCol() + 1, InputWorkbookCalculation.cache(templates.get(address.cell()), value));
        });
        return Set.copyOf(selected.keySet());
    }
}
