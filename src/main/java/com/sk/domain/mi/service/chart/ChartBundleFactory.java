package com.sk.domain.mi.service.chart;

import com.sk.common.utils.DateUtil;
import com.sk.common.utils.MathUtil;
import com.sk.domain.mi.conversion.dto.BollingerBandResult;
import com.sk.domain.mi.dto.response.charts.*;
import com.sk.domain.shared.enums.ChartSettingConditionCd;
import com.sk.domain.shared.enums.chart.ChartType;
import com.sk.domain.shared.enums.chart.RenderAs;

import java.math.BigDecimal;
import java.util.ArrayList;
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
    public static ChartBundleRes create(ChartMetadata metadata, ChartRes mainChart, List<IndicatorRes> indicators, List<ChartRes> comparisonCharts) {
        return ChartBundleRes.of(metadata, mainChart, indicators, comparisonCharts);
    }

    public static ChartRes toChart(boolean isCandle, List<Map<String, Object>> rawRows, List<String> targetColumns, Map<String, String> labels) {
        ChartType chartType = isCandle ? ChartType.CANDLE : ChartType.METRIC;

        String xAxis = targetColumns.get(IDX_DATE);
        List<String> yAxisList = targetColumns.subList(1, targetColumns.size());

        List<String> timestamps = buildTimestamps(xAxis, rawRows);
        List<Series> series = buildSeries(yAxisList, chartType, rawRows, labels);

        return ChartRes.of(chartType, timestamps, series);
    }

    private static List<String> buildTimestamps(String xAxis, List<Map<String, Object>> rawRows) {
        return rawRows.stream()
            .map(row -> String.valueOf(row.get(xAxis)))
            .map(DateUtil::fromYmd)
            .map(DateUtil::toYmd)
            .toList();
    }

    private static List<Series> buildSeries(List<String> yAxisList, ChartType chartType, List<Map<String, Object>> rawRows, Map<String, String> labels) {
        return IntStream.range(0, yAxisList.size())
            .mapToObj(index -> {
                String columnName = yAxisList.get(index);
                RenderAs renderAs = chartType.resolveRenderAs(index);
                String label = labels.getOrDefault(columnName, columnName);
                List<BigDecimal> values = rawRows.stream().map(row -> MathUtil.toBigDecimal(row.get(columnName))).toList();
                return Series.of(renderAs.getRenderingId(String.valueOf(index + 1)), label, renderAs, values, true);
            })
            .toList();
    }

    /**
     * 이동평균선 지표 DTO 생성
     */
    public static List<Series> createMovingAverageIndicator(List<BigDecimal> ma5, List<BigDecimal> ma20, List<BigDecimal> ma60, List<BigDecimal> ma120, Map<ChartSettingConditionCd, List<String>> personalizedMap) {

        Map<String, List<BigDecimal>> series = Map.of(
            "5", ma5,
            "20", ma20,
            "60", ma60,
            "120", ma120
        );

        RenderAs renderAs = RenderAs.MA;

        List<Series> seriesList = new ArrayList<>();

        List<String> indicatorValues = personalizedMap.get(ChartSettingConditionCd.INDICATOR);

        series.forEach((key, value) -> {
            String renderingId = renderAs.getRenderingId(key);
            String label = String.format("%s(%s)", renderAs.getDisplayDescription(), key);
            seriesList.add(Series.of(renderingId, label, renderAs, value, indicatorValues.contains(renderingId)));
        });

        return seriesList;
    }

    /**
     * 볼린저 밴드 지표 DTO 생성
     */
    public static List<Series> createBollingerBandsIndicator(List<BollingerBandResult> results, Map<ChartSettingConditionCd, List<String>> personalizedMap) {

        Map<String, List<BigDecimal>> series = Map.of(
            "upper", results.stream().map(BollingerBandResult::upper).toList(),
            "middle", results.stream().map(BollingerBandResult::middle).toList(),
            "lower", results.stream().map(BollingerBandResult::lower).toList()
        );

        RenderAs renderAs = RenderAs.BB;

        List<Series> seriesList = new ArrayList<>();

        List<String> indicatorValues = personalizedMap.get(ChartSettingConditionCd.INDICATOR);

        series.forEach((key, value) -> {
            String renderingId = renderAs.getRenderingId(key);
            String label = String.format("%s(%s)", renderAs.getDisplayDescription(), key);
            seriesList.add(Series.of(renderingId, label, renderAs, value, indicatorValues.contains("bb")));
        });

        return seriesList;
    }

}
