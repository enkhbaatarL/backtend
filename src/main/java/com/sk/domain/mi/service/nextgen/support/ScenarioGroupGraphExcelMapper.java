package com.sk.domain.mi.service.nextgen.support;

import com.sk.common.model.TableRes;
import com.sk.common.utils.ExcelUtils;
import com.sk.domain.mi.dto.response.nextgen.ScenarioGroupGraphRes;
import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * 시나리오 그룹 그래프 → 엑셀 변환 (인덱스별 시트 1개, 행=시리즈 / 열=기간).
 */
public final class ScenarioGroupGraphExcelMapper {

    private ScenarioGroupGraphExcelMapper() {
    }

    public static ByteArrayInputStream toExcel(List<ScenarioGroupGraphRes> graphs) {
        LinkedHashMap<String, TableRes> sheets = new LinkedHashMap<>();
        for (ScenarioGroupGraphRes graph : graphs) {
            sheets.put(graph.indexName(), toSheet(graph));
        }
        return ExcelUtils.toExcel(sheets);
    }

    /** 한 인덱스 그래프 → 시트. 미실행 시나리오/빠진 구간은 빈 칸 */
    private static TableRes toSheet(ScenarioGroupGraphRes graph) {
        List<String> dates = graph.dates();

        // 헤더(열) = 구분 + 기간들
        List<TableRes.Column> columns = new ArrayList<>();
        columns.add(TableRes.Column.of("series", "구분"));
        for (String date : dates) {
            columns.add(TableRes.Column.of(date, date));
        }

        // 행 = 시리즈별 (Base + 시나리오별 베이스케이스/사용자조정 2행)
        List<TableRes.Row> rows = new ArrayList<>();
        for (ScenarioGroupGraphRes.BaseSeries base : graph.baseSeries()) {
            rows.add(seriesRow(base.seriesName(), base.values(), dates.size()));
        }
        for (ScenarioGroupGraphRes.ScenarioSeries scenario : graph.scenarioSeries()) {
            rows.add(seriesRow("%s (Base Case)".formatted(scenario.scenarioName()), scenario.baseCaseValues(), dates.size()));
            rows.add(seriesRow("%s (사용자 조정)".formatted(scenario.scenarioName()), scenario.userAdjustedValues(), dates.size()));
        }
        return new TableRes(graph.indexName(), columns, rows);
    }

    private static TableRes.Row seriesRow(String name, List<BigDecimal> values, int size) {
        List<String> cells = new ArrayList<>();
        cells.add(name);
        for (int i = 0; i < size; i++) {
            cells.add(valueAt(values, i));
        }
        return TableRes.Row.of(cells);
    }

    /** 값이 없으면 빈 칸 */
    private static String valueAt(List<BigDecimal> values, int i) {
        if (values == null || i >= values.size() || values.get(i) == null) {
            return "";
        }
        return values.get(i).toPlainString();
    }

}
