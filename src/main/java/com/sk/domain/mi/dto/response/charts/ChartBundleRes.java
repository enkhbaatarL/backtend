package com.sk.domain.mi.dto.response.charts;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.util.List;

/**
 * 차트 API의 최상위 응답 DTO (번들)
 *
 * @param metadata 차트의 공통 메타데이터 (단위, 간격 등)
 * @param mainChart     주요 차트 데이터 (캔들 차트 등) - ChartDataDto 사용
 * @param indicators    보조 지표 데이터 목록 (이동평균선, 볼린저밴드 등) - IndicatorDto 사용
 * @param comparisonCharts 비교 차트 데이터 목록 (다른 상품 가격, 지수 등) - ChartDataDto 사용
 */
@Builder
public record ChartBundleRes(

    @Schema(description = "차트 메타데이터")
    ChartMetadata metadata,

    @Schema(description = "메인 차트")
    Chart mainChart,

    @Schema(description = "메인 차트 보조지표")
    List<Indicator> indicators,

    @Schema(description = "비교군 차트")
    List<Chart> comparisonCharts

) {

    public static ChartBundleRes of(ChartMetadata metadata, Chart mainChart, List<Indicator> indicators, List<Chart> comparisonCharts) {
        return ChartBundleRes.builder()
            .metadata(metadata)
            .mainChart(mainChart)
            .indicators(indicators)
            .comparisonCharts(comparisonCharts)
            .build();
    }

}
