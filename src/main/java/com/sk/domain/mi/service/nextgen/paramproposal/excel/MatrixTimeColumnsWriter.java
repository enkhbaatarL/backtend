package com.sk.domain.mi.service.nextgen.paramproposal.excel;

import com.sk.domain.mi.service.nextgen.paramproposal.excel.ExcelCells.RowScan;
import com.sk.domain.shared.entity.master.tmm.MiModelParam;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Sheet;

/**
 * matrix_time_columns 모드 기입기 — 월별 값이 열 방향으로 있는 matrix 표 (패키지 외부 비공개).
 *
 * 기입 규칙:
 * - 행: 시간축(Year/Month/Period)을 제외한 slot 축을 column roles(B~F 축 컬럼)로 매칭해 설비 행을 찾는다.
 *   없는 설비 행은 만들 수 없으므로 미존재/복수 매칭 모두 실패시킨다
 * - 열: value 컬럼 범위(start~end)에서 year row/month row 값이 payload의 Year/Month와 일치하는 열을 찾는다.
 *   payload의 Period는 열 매칭에 쓰지 않는다 — Agent 출력에서 Period가 서로 모순된 사례가 있어
 *   Year+Month를 정본으로 하고, 시트 period row와 다르면 경고만 남긴다
 * - 교차 셀에 proposedValue를 덮어쓴다 (빈 셀이어도 그대로 기입 — 원래 override 값이 없던 자리)
 */
@Slf4j
final class MatrixTimeColumnsWriter {

    private static final Set<String> TIME_AXES = Set.of("Year", "Month", "Period");

    private record SlotCell(int columnIndex, Object expected) {
    }

    private final Sheet sheet;
    private final MiModelParam param;
    private final ColumnRoleMap roles;
    private final String context;

    MatrixTimeColumnsWriter(Sheet sheet, MiModelParam param, ColumnRoleMap roles) {
        this.sheet = sheet;
        this.param = param;
        this.roles = roles;
        this.context = ValueRows.context(param);
        requireMatrixSpec();
    }

    void apply(List<Map<String, Object>> valueRows) {
        valueRows.forEach(this::applyRow);
    }

    private void applyRow(Map<String, Object> valueRow) {
        Map<String, Object> slotAxes = ValueRows.slotAxes(context, valueRow);

        int rowIndex = findFacilityRow(slotAxes);
        int columnIndex = findTimeColumn(slotAxes);
        verifyPeriod(slotAxes, columnIndex);

        ExcelCells.writeValue(sheet, rowIndex, columnIndex, ValueRows.proposedValue(context, valueRow));
    }

    /** 시간축을 제외한 slot 축으로 설비 행을 특정한다. */
    private int findFacilityRow(Map<String, Object> slotAxes) {
        Map<String, Object> facilityAxes = new LinkedHashMap<>();
        slotAxes.forEach((key, value) -> {
            if (!isTimeAxis(key)) {
                facilityAxes.put(key, value);
            }
        });
        if (facilityAxes.isEmpty()) {
            throw ValueRows.error(context, "행을 특정할 비시간 slot 축이 없습니다: %s".formatted(slotAxes.keySet()));
        }

        List<SlotCell> slots = facilityAxes.entrySet().stream()
            .map(axis -> new SlotCell(resolveColumn(axis.getKey()), axis.getValue()))
            .toList();

        List<Integer> keyColumns = roles.columnIndexes();
        RowScan scan = ExcelCells.scanRows(sheet, param.getBasecaseDataStartRow() - 1, keyColumns,
            candidate -> slots.stream().allMatch(slot -> ExcelCells.matches(slot.expected(), ExcelCells.readValue(sheet, candidate, slot.columnIndex()))));

        if (scan.matchedRowIndexes().isEmpty()) {
            throw ValueRows.error(context, "일치하는 설비 행이 없습니다 (matrix는 신규 행 추가 불가): %s".formatted(facilityAxes));
        }
        if (scan.matchedRowIndexes().size() > 1) {
            throw ValueRows.error(context, "설비 행이 복수로 매칭됩니다 (엑셀 행=%s, axes=%s)".formatted(toExcelRows(scan.matchedRowIndexes()), facilityAxes));
        }
        return scan.matchedRowIndexes().get(0);
    }

    /** value 컬럼 범위에서 Year+Month 헤더가 일치하는 열을 찾는다. 연도 헤더 공백은 직전 값을 이어받는다. */
    private int findTimeColumn(Map<String, Object> slotAxes) {
        Double year = requireTimeAxis(slotAxes, "Year");
        Double month = requireTimeAxis(slotAxes, "Month");

        int yearRowIndex = param.getBasecaseYearRow() - 1;
        int monthRowIndex = param.getBasecaseMonthRow() - 1;
        int startColumn = ExcelCells.columnIndex(param.getBasecaseValueStartCol());
        int endColumn = ExcelCells.columnIndex(param.getBasecaseValueEndCol());

        Double currentYear = null;
        for (int column = startColumn; column <= endColumn; column++) {
            Double headerYear = ExcelCells.asNumber(ExcelCells.readValue(sheet, yearRowIndex, column));
            if (headerYear != null) {
                currentYear = headerYear;
            }
            Double headerMonth = ExcelCells.asNumber(ExcelCells.readValue(sheet, monthRowIndex, column));
            if (year.equals(currentYear) && month.equals(headerMonth)) {
                return column;
            }
        }
        throw ValueRows.error(context, "Year=%s, Month=%s에 해당하는 열이 없습니다 (값 영역 %s~%s열)"
            .formatted(year.intValue(), month.intValue(), param.getBasecaseValueStartCol(), param.getBasecaseValueEndCol()));
    }

    /** payload Period는 신뢰하지 않는다 — 시트 period row와 어긋나면 경고만 남기고 진행한다. */
    private void verifyPeriod(Map<String, Object> slotAxes, int columnIndex) {
        Double payloadPeriod = ExcelCells.asNumber(slotAxes.get("Period"));
        if (payloadPeriod == null || param.getBasecasePeriodRow() == null) {
            return;
        }
        Double sheetPeriod = ExcelCells.asNumber(ExcelCells.readValue(sheet, param.getBasecasePeriodRow() - 1, columnIndex));
        if (!payloadPeriod.equals(sheetPeriod)) {
            log.warn("[ModelInput Merge] Period 불일치 (Year/Month 기준으로 기입): [{}] payload={}, sheet={}",
                context, payloadPeriod, sheetPeriod);
        }
    }

    private int resolveColumn(String axisLabel) {
        return roles.byLabel(axisLabel)
            .orElseThrow(() -> ValueRows.error(context, "column roles에 없는 slot 축입니다: %s".formatted(axisLabel)))
            .columnIndex();
    }

    private Double requireTimeAxis(Map<String, Object> slotAxes, String axisName) {
        Double value = ExcelCells.asNumber(slotAxes.get(axisName));
        if (value == null) {
            throw ValueRows.error(context, "matrix 열 매칭에 필요한 %s 축이 없거나 숫자가 아닙니다".formatted(axisName));
        }
        return value;
    }

    private boolean isTimeAxis(String key) {
        return TIME_AXES.stream().anyMatch(axis -> axis.equalsIgnoreCase(key.trim()));
    }

    private void requireMatrixSpec() {
        if (param.getBasecaseYearRow() == null || param.getBasecaseMonthRow() == null
            || param.getBasecaseValueStartCol() == null || param.getBasecaseValueEndCol() == null) {
            throw ValueRows.error(context, "matrix 좌표 스펙(year/month row, value 컬럼 범위)이 마스터에 없습니다");
        }
    }

    /** POI 0-based 행 인덱스를 엑셀 표기(1-based)로 변환 — 에러/로그는 항상 엑셀 행 번호로 말한다. */
    private static List<Integer> toExcelRows(List<Integer> rowIndexes) {
        return rowIndexes.stream().map(rowIndex -> rowIndex + 1).toList();
    }

}
