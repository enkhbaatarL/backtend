package com.sk.domain.mi.service.chart;

import com.sk.common.utils.DateUtil;
import com.sk.domain.mi.conversion.dto.BollingerBandResult;
import com.sk.domain.mi.dto.response.chart.*;
import com.sk.domain.shared.enums.ChartRenderType;
import com.sk.domain.shared.enums.ChartSettingConditionCd;
import com.sk.domain.shared.enums.ChartType;
import com.sk.domain.shared.enums.IndicatorType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

/**
 * 차트 데이터와 보조지표 DTO를 생성하는 유틸리티 클래스
 */
public final class ChartBundleFactory {

    private ChartBundleFactory() { /* static utility */ }

    private static final int IDX_DATE = 0;

    /**
     * 차트 객체 생성
     */
    public static ChartBundleRes create(ChartMetadata metadata, ChartData mainChart, List<IndicatorData> indicators, List<ChartData> comparisonCharts) {
        return ChartBundleRes.of(metadata, mainChart, indicators, comparisonCharts);
    }

    /**
     * 원시 집계 결과를 ChartData DTO로 변환
     *
     * @param isCandle     캔들 차트 여부
     * @param rawRows      집계된 원시 행 목록
     * @param labels       컬럼명→레이블 매핑
     */
    public static ChartData toChartData(boolean isCandle, List<Map<String, Object>> rawRows, List<String> selectedColumns, Map<String, String> labels) {
        ChartType chartType = isCandle ? ChartType.CANDLE : ChartType.METRIC;

        List<SchemaEntry> schema = buildSchema(selectedColumns, chartType, labels);
        Map<String, List<?>> data = buildDataMap(rawRows, selectedColumns);

        return ChartData.of(chartType, selectedColumns, schema, data);
    }

    private static List<SchemaEntry> buildSchema(List<String> columns, ChartType chartType, Map<String, String> labels) {
        return IntStream.range(0, columns.size())
            .mapToObj(i -> {
                String name = columns.get(i);
                ChartRenderType renderAs = chartType.resolveRenderType(i);
                String label = labels.getOrDefault(name, name);
                return SchemaEntry.of(name, renderAs, label, Boolean.TRUE);
            })
            .toList();
    }

    private static Map<String, List<?>> buildDataMap(List<Map<String, Object>> rawRows, List<String> columns) {
        Map<String, List<?>> data = new LinkedHashMap<>();
        for (String col : columns) {
            List<Object> values = rawRows.stream()
                .map(row -> row.get(col))
                .toList();
            data.put(col, values);
        }
        return data;
    }

    /**
     * 이동평균선 지표 DTO 생성
     */
    public static IndicatorData createMovingAverageIndicator(List<LocalDate> dates, List<BigDecimal> ma5, List<BigDecimal> ma20, List<BigDecimal> ma60, List<BigDecimal> ma120, Map<ChartSettingConditionCd, List<String>> personalizedMap) {

        Map<String, List<BigDecimal>> series = Map.of(
            "ma5", ma5,
            "ma20", ma20,
            "ma60", ma60,
            "ma120", ma120
        );

        return createIndicator(IndicatorType.MOVING_AVERAGE, dates, series, ChartRenderType.MA, personalizedMap);
    }

    /**
     * 볼린저 밴드 지표 DTO 생성
     */
    public static IndicatorData createBollingerBandsIndicator(List<LocalDate> dates, List<BollingerBandResult> results, Map<ChartSettingConditionCd, List<String>> personalizedMap) {
        Map<String, List<BigDecimal>> series = new LinkedHashMap<>();
        series.put("upper", results.stream().map(BollingerBandResult::upper).toList());
        series.put("middle", results.stream().map(BollingerBandResult::middle).toList());
        series.put("lower", results.stream().map(BollingerBandResult::lower).toList());

        return createIndicator(IndicatorType.BOLLINGER_BANDS, dates, series, ChartRenderType.BB, personalizedMap);
    }

    /**
     * 보조지표 공통 생성 헬퍼
     */
    private static IndicatorData createIndicator(IndicatorType type, List<LocalDate> dates, Map<String, List<BigDecimal>> seriesMap, ChartRenderType renderType, Map<ChartSettingConditionCd, List<String>> personalizedMap) {
        List<String> columns = new ArrayList<>();
        columns.add("date");
        columns.addAll(seriesMap.keySet());

        List<SchemaEntry> schema = new ArrayList<>();
        schema.add(SchemaEntry.of("date", ChartRenderType.DATE, "날짜", Boolean.TRUE));
        for (String key : seriesMap.keySet()) {
            schema.add(SchemaEntry.of(key, renderType, String.format("%s(%s)", renderType.getDisplayDescription(), key), Boolean.TRUE));
        }

        Map<String, List<?>> data = new LinkedHashMap<>();
        data.put("date", dates.stream().map(DateUtil::toYmd).toList());
        data.putAll(seriesMap);

        return IndicatorData.of(type, columns, schema, data);
    }

}
