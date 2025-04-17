package com.sk.domain.mi.dto.response.chart;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * 차트 API의 최상위 응답 DTO (번들)
 *
 * @param chartMetadata 차트의 공통 메타데이터 (단위, 간격 등)
 * @param mainChart     주요 차트 데이터 (캔들 차트 등) - ChartDataDto 사용
 * @param indicators    보조 지표 데이터 목록 (이동평균선, 볼린저밴드 등) - IndicatorDto 사용
 * @param compareCharts 비교 차트 데이터 목록 (다른 상품 가격, 지수 등) - ChartDataDto 사용
 */
public record ChartBundleRes(

    @Schema(description = "차트 메타데이터")
    ChartMetadata chartMetadata,

    @Schema(description = "메인 차트")
    ChartData mainChart,

    @Schema(description = "메인 차트 보조지표")
    List<IndicatorData> indicators,

    @Schema(description = "비교군 차트")
    List<ChartData> compareCharts

) {

}
