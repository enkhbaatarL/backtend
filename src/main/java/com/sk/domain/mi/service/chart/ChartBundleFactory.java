package com.sk.domain.mi.service.chart;

import com.sk.common.utils.DateUtil;
import com.sk.common.utils.MathUtil;
import com.sk.common.utils.StringUtil;
import com.sk.domain.mi.conversion.dto.BollingerBandResult;
import com.sk.domain.mi.dto.response.charts.*;
import com.sk.domain.shared.entity.master.tmd.GoodsDataSourceDetail;
import com.sk.domain.shared.enums.chart.ChartType;
import com.sk.domain.shared.enums.chart.RenderAs;
import io.micrometer.common.util.StringUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * 차트 데이터와 보조지표 DTO를 생성하는 유틸리티 클래스
 */
public final class ChartBundleFactory {

    private ChartBundleFactory() { /* static utility */ }

    private static final int IDX_DATE = 0;

    /**
     * 차트 묶음 객체를 생성한다.
     * 차트 메타데이터, 메인 차트, 지표, 비교 차트를 묶어서 하나의 차트 묶음 응답 객체를 생성한다.
     *
     * @param metadata         차트 메타데이터
     * @param mainChart        메인 차트 데이터
     * @param indicators       지표 데이터 목록
     * @param comparisonCharts 비교 차트 데이터 목록
     * @return 생성된 차트 묶음 응답 객체
     */
    public static ChartBundleRes create(ChartMetadata metadata, Chart mainChart, List<Indicator> indicators, List<Chart> comparisonCharts) {
        return ChartBundleRes.of(metadata, mainChart, indicators, comparisonCharts);
    }

    /**
     * 원시 데이터를 차트 객체로 변환한다.
     * 캔들 차트 여부에 따라 차트 타입을 결정하고, 원시 데이터에서 시간 데이터와 시리즈 데이터를 추출하여
     * 차트 응답 객체를 생성한다.
     *
     * @param gds           데이터소스상세 번호
     * @param isCandle      캔들 차트 여부
     * @param convertedData 원시 데이터 행 목록
     * @param targetColumns 대상 컬럼 목록
     * @param labels        컬럼명과 표시 레이블을 매핑한 맵
     * @param isVisible     차트 표시 여부
     * @return 생성된 차트 응답 객체
     */
    public static Chart combineCharts(GoodsDataSourceDetail gds, boolean isCandle, List<Map<String, Object>> convertedData, List<String> targetColumns, Map<String, String> labels, boolean isVisible) {
        ChartType chartType = isCandle ? ChartType.CANDLE : ChartType.LINE;

        String xAxis = targetColumns.get(IDX_DATE);
        List<String> yAxisList = targetColumns.subList(1, targetColumns.size());

        List<String> timestamps = buildTimestamps(xAxis, convertedData);
        List<Series> series = buildSeries(yAxisList, chartType, convertedData, labels, gds.getTradeVolumeColumnNm(), isVisible);

        return Chart.of(gds.getGoodsDataSourceNo(), ChartUnitConversionService.generateDefaultUnitCd(gds), chartType, timestamps, series);
    }

    private static List<String> buildTimestamps(String xAxis, List<Map<String, Object>> rawRows) {
        return rawRows.stream()
            .map(row -> DateUtil.parseDate(row.get(xAxis)))
            .map(DateUtil::toYmd)
            .toList();
    }

    private static List<Series> buildSeries(List<String> yAxisList, ChartType chartType, List<Map<String, Object>> rawRows, Map<String, String> labels, String volumeColumn, boolean isVisible) {

        List<Series> series = IntStream.range(0, yAxisList.size())
            .mapToObj(index -> {
                String columnName = yAxisList.get(index);
                RenderAs renderAs = chartType.resolveRenderAs(index);
                String label = labels.getOrDefault(columnName, columnName);
                List<BigDecimal> values = rawRows.stream().map(row -> MathUtil.toBigDecimal(row.get(columnName))).toList();
                return Series.of(renderAs.getRenderingId(String.valueOf(index + 1)), label, renderAs, values, isVisible);
            })
            .collect(Collectors.toList());

        if (StringUtils.isNotEmpty(volumeColumn)) {
            List<BigDecimal> values = rawRows.stream().map(row -> MathUtil.toBigDecimal(row.get(volumeColumn))).toList();
            series.add(Series.of(RenderAs.VOLUME.getId(), RenderAs.VOLUME.getDisplayDescription(), RenderAs.VOLUME, values, isVisible));
        }

        return series;
    }

    /**
     * 이동평균선 지표 시리즈 목록을 생성한다.
     * 5일, 20일, 60일, 120일 이동평균선 데이터를 받아 시리즈 객체로 변환한다.
     * 개인화 설정에 따라 각 이동평균선의 표시 여부를 결정한다.
     *
     * @param ma5             5일 이동평균선 데이터
     * @param ma20            20일 이동평균선 데이터
     * @param ma60            60일 이동평균선 데이터
     * @param ma120           120일 이동평균선 데이터
     * @param indicatorValues 개인화된 지표 값 목록
     * @return 이동평균선 시리즈 목록
     */
    public static List<Series> createMovingAverageIndicator(List<BigDecimal> ma5, List<BigDecimal> ma20, List<BigDecimal> ma60, List<BigDecimal> ma120, List<String> indicatorValues) {

        Map<String, List<BigDecimal>> series = Map.of(
            "5", ma5,
            "20", ma20,
            "60", ma60,
            "120", ma120
        );

        RenderAs renderAs = RenderAs.MA;

        List<Series> seriesList = new ArrayList<>();

        series.forEach((key, value) -> {
            String renderingId = renderAs.getRenderingId(key);
            String label = String.format("%s(%s)", renderAs.getDisplayDescription(), key);
            seriesList.add(Series.of(renderingId, label, renderAs, value, indicatorValues.contains(renderingId)));
        });

        return seriesList;
    }

    /**
     * 볼린저 밴드 지표 시리즈 목록을 생성한다.
     * 볼린저 밴드 결과(상단, 중간, 하단) 데이터를 받아 시리즈 객체로 변환한다.
     * 개인화 설정에 따라 볼린저 밴드의 표시 여부를 결정한다.
     *
     * @param results         볼린저 밴드 결과 목록
     * @param indicatorValues 개인화된 지표 값 목록
     * @return 볼린저 밴드 시리즈 목록
     */
    public static List<Series> createBollingerBandsIndicator(List<BollingerBandResult> results, List<String> indicatorValues) {

        Map<String, List<BigDecimal>> series = Map.of(
            "upper", results.stream().map(BollingerBandResult::upper).toList(),
            "middle", results.stream().map(BollingerBandResult::middle).toList(),
            "lower", results.stream().map(BollingerBandResult::lower).toList()
        );

        RenderAs renderAs = RenderAs.BB;

        List<Series> seriesList = new ArrayList<>();

        series.forEach((key, value) -> {
            String renderingId = renderAs.getRenderingId(key);
            String label = String.format("%s(%s)", renderAs.getDisplayDescription(), key);
            seriesList.add(Series.of(renderingId, label, renderAs, value, indicatorValues.contains("bb")));
        });

        return seriesList;
    }

}
