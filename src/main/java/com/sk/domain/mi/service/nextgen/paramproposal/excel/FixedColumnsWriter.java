package com.sk.domain.mi.service.nextgen.paramproposal.excel;

import com.sk.domain.mi.service.nextgen.paramproposal.excel.ColumnRoleMap.Role;
import com.sk.domain.mi.service.nextgen.paramproposal.excel.ExcelCells.RowScan;
import com.sk.domain.shared.entity.master.tmm.MiModelParam;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Sheet;

/**
 * fixed_columns 모드 기입기 - slot/value가 고정 컬럼에 있는 행 단위 표
 *
 * 기입 규칙:
 * - slot 축 키를 column roles 라벨로 매칭해 slot 컬럼을, 잔여 role 하나로 value 컬럼을 정한다
 * - row filter(예: E=CDD)가 있으면 필터 컬럼 값이 일치하는 행만 후보로 본다 (HDD/CDD 혼재 표 대응)
 * - data start row부터 slot 컬럼 전체가 일치하는 행을 찾아 value 셀에 proposedValue를 덮어쓴다
 * - 일치 행이 없으면(원본에 없던 신규 항목) 데이터 영역 끝에 행을 추가한다. 필터가 있으면 필터 값도 기입한다
 * - 일치 행이 복수면 시트 데이터 정합 문제이므로 실패시킨다 (부분 성공 없음)
 */
@Slf4j
final class FixedColumnsWriter {

    private record SlotCell(int columnIndex, Object expected) {
    }

    private final Sheet sheet;
    private final MiModelParam param;
    private final ColumnRoleMap roles;
    private final RowFilter rowFilter;
    private final String context;

    FixedColumnsWriter(Sheet sheet, MiModelParam param, ColumnRoleMap roles) {
        this.sheet = sheet;
        this.param = param;
        this.roles = roles;
        this.context = ValueRows.context(param);
        this.rowFilter = RowFilter.parse(context, param.getBasecaseRowFilter());
    }

    WriteSummary apply(List<Map<String, Object>> valueRows) {
        int writeCount = 0;
        int appendCount = 0;
        for (Map<String, Object> valueRow : valueRows) {
            if (applyRow(valueRow)) {
                writeCount++;
            } else {
                appendCount++;
            }
        }
        return new WriteSummary(writeCount, appendCount);
    }

    /** 값 1행을 기입한다. 기존 행 덮어쓰기면 true, 신규 행 추가면 false. */
    private boolean applyRow(Map<String, Object> valueRow) {
        Map<String, Object> slotAxes = ValueRows.slotAxes(context, valueRow);

        List<SlotCell> slots = slotAxes.entrySet().stream()
            .map(axis -> new SlotCell(resolveColumn(axis.getKey()), axis.getValue()))
            .toList();
        Role valueRole = roles.valueRole(slotAxes.keySet(), rowFilter);
        double proposedValue = ValueRows.proposedValue(context, valueRow);

        List<Integer> keyColumns = slots.stream().map(SlotCell::columnIndex).toList();
        RowScan scan = ExcelCells.scanRows(sheet, param.getBasecaseDataStartRow() - 1, keyColumns, rowIndex -> matchesRow(rowIndex, slots));

        if (scan.matchedRowIndexes().size() > 1) {
            throw ValueRows.error(context, "slot 축에 일치하는 행이 복수입니다 (엑셀 행=%s, axes=%s)".formatted(toExcelRows(scan.matchedRowIndexes()), slotAxes));
        }

        if (scan.matchedRowIndexes().size() == 1) {
            int rowIndex = scan.matchedRowIndexes().get(0);
            Object previous = ExcelCells.writeValue(sheet, rowIndex, valueRole.columnIndex(), proposedValue);
            log.info("[ModelInput Merge] - WRITE  [{}] cell={} axes={} value: {} -> {}",
                context, ExcelCells.cellAddress(rowIndex, valueRole.columnIndex()), slotAxes,
                ExcelCells.display(previous), ExcelCells.display(proposedValue));
            return true;
        }

        appendRow(scan.appendRowIndex(), slots, valueRole, proposedValue);
        log.info("[ModelInput Merge] - APPEND [{}] cell={} axes={} value: (없음) -> {} — 원본에 없던 신규 행 추가",
            context, ExcelCells.cellAddress(scan.appendRowIndex(), valueRole.columnIndex()), slotAxes,
            ExcelCells.display(proposedValue));
        return false;
    }

    private int resolveColumn(String axisLabel) {
        return roles.byLabel(axisLabel)
            .orElseThrow(() -> ValueRows.error(context, "column roles에 없는 slot 축입니다: %s".formatted(axisLabel)))
            .columnIndex();
    }

    /** slot 축 전체가 일치하고, row filter가 있으면 그 조건까지 만족하는 행인지 판정한다. */
    private boolean matchesRow(int rowIndex, List<SlotCell> slots) {
        if (rowFilter != null && !rowFilter.matches(sheet, rowIndex)) {
            return false;
        }
        return slots.stream()
            .allMatch(slot -> ExcelCells.matches(slot.expected(), ExcelCells.readValue(sheet, rowIndex, slot.columnIndex())));
    }

    /** 신규 행에 slot 값·필터 값·proposedValue를 기입한다. 필터 값이 빠지면 이후 그 행을 다시 찾을 수 없다. */
    private void appendRow(int rowIndex, List<SlotCell> slots, Role valueRole, double proposedValue) {
        slots.forEach(slot -> ExcelCells.writeValue(sheet, rowIndex, slot.columnIndex(), slot.expected()));
        if (rowFilter != null) {
            ExcelCells.writeValue(sheet, rowIndex, rowFilter.columnIndex(), rowFilter.expected());
        }
        ExcelCells.writeValue(sheet, rowIndex, valueRole.columnIndex(), proposedValue);
    }

    /** POI 0-based 행 인덱스를 엑셀 표기(1-based)로 변환 — 에러/로그는 항상 엑셀 행 번호로 말한다. */
    private static List<Integer> toExcelRows(List<Integer> rowIndexes) {
        return rowIndexes.stream().map(rowIndex -> rowIndex + 1).toList();
    }

}
