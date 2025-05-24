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
 * 차트 데이터와 보조지표 DTO를 생성하는 팩토리 클래스
 */
public final class ChartBundleFactory {

    private ChartBundleFactory() { /* static utility */ }

    private static final int IDX_DATE = 0;

    /**
     * 차트 묶음 객체를 생성한다.
     *
     * @param metadata         차트의 메타데이터 정보
     * @param mainChart        메인 차트 데이터
     * @param indicators       차트에 표시될 지표 목록
     * @param comparisonCharts 비교 차트 목록
     * @return 메타데이터, 메인 차트, 지표, 비교 차트가 포함된 차트 묶음 객체
     */
    public static ChartBundleRes create(ChartMetadata metadata, Chart mainChart, List<Indicator> indicators, List<Chart> comparisonCharts) {
        return ChartBundleRes.of(metadata, mainChart, indicators, comparisonCharts);
    }

    /**
     * 원시 데이터를 차트 객체로 변환한다.
     *
     * @param gds           데이터소스 상세 정보
     * @param isCandle      캔들차트 여부 (true: 캔들차트, false: 라인차트)
     * @param convertedData 변환된 원시 데이터
     * @param targetColumns 차트에 표시할 데이터 컬럼명 목록
     * @param labels        컬럼명에 대응하는 표시 레이블 맵
     * @param isVisible     차트 표시 여부
     * @return 생성된 차트 객체
     */
    public static Chart combineCharts(GoodsDataSourceDetail gds, boolean isCandle, List<Map<String, Object>> convertedData, List<String> targetColumns, Map<String, String> labels, boolean isVisible) {
        ChartType chartType = isCandle ? ChartType.CANDLE : ChartType.LINE;

        String xAxis = targetColumns.get(IDX_DATE).trim();
        List<String> yAxisList = targetColumns.subList(1, targetColumns.size()).stream().map(String::trim).collect(Collectors.toList());

        List<String> timestamps = buildTimestamps(xAxis, convertedData);
        List<Series> series = buildSeries(yAxisList, chartType, convertedData, labels, gds.getTradeVolumeColumnNm(), isVisible);

        return Chart.of(gds.getGoodsDataSourceNo(), ChartUnitConversionService.generateDefaultUnitCd(gds), chartType, timestamps, series);
    }

    /**
     * 원시 데이터에서 타임스탬프 목록을 추출한다.
     *
     * @param xAxis   X축 컬럼명
     * @param rawRows 원시 데이터 목록
     * @return 추출된 타임스탬프 문자열 목록
     */
    private static List<String> buildTimestamps(String xAxis, List<Map<String, Object>> rawRows) {
        return rawRows.stream()
            .map(row -> DateUtil.parseDate(row.get(xAxis)))
            .map(DateUtil::toYmd)
            .toList();
    }

    /**
     * 시계열 데이터로 차트 시리즈를 생성한다.
     *
     * @param yAxisList    Y축 데이터 컬럼 목록
     * @param chartType    차트 유형
     * @param rawRows      원시 데이터 목록
     * @param labels       컬럼명에 대응하는 표시 레이블 맵
     * @param volumeColumn 거래량 컬럼명
     * @param isVisible    차트 표시 여부
     * @return 생성된 차트 시리즈 목록
     */
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
     * 이동평균선 시리즈를 생성한다.
     *
     * @param ma5                 5일 이동평균 데이터
     * @param ma20                20일 이동평균 데이터
     * @param ma60                60일 이동평균 데이터
     * @param ma120               120일 이동평균 데이터
     * @param finalizedIndicators 표시할 지표 ID 목록
     * @return 이동평균선 시리즈 목록
     */
    public static List<Series> createMovingAverageIndicator(List<BigDecimal> ma5, List<BigDecimal> ma20, List<BigDecimal> ma60, List<BigDecimal> ma120, List<String> finalizedIndicators) {

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
            seriesList.add(Series.of(renderingId, label, renderAs, value, finalizedIndicators.contains(renderingId.toUpperCase())));
        });

        return seriesList;
    }

    /**
     * 볼린저 밴드 시리즈를 생성한다.
     *
     * @param results             볼린저 밴드 계산 결과 목록
     * @param finalizedIndicators 표시할 지표 ID 목록
     * @return 볼린저 밴드 시리즈 목록 (상단/중간/하단 밴드)
     */
    public static List<Series> createBollingerBandsIndicator(List<BollingerBandResult> results, List<String> finalizedIndicators) {

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
            seriesList.add(Series.of(renderingId, label, renderAs, value, finalizedIndicators.contains("BB")));
        });

        return seriesList;
    }

}
