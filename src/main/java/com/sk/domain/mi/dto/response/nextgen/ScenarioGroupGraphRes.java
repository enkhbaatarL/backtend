package com.sk.domain.mi.dto.response.nextgen;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.List;

/**
 * 시나리오 그룹 펼침 그래프 데이터 (단일 인덱스).
 */
public record ScenarioGroupGraphRes(

    @Schema(description = "시나리오 그룹 ID", example = "1")
    Long scenarioGroupId,

    @Schema(description = "인덱스명", example = "JKM")
    String indexName,

    @Schema(description = "통화 (USD, EUR 등)", example = "USD")
    String currency,

    @Schema(description = "단위 (MMBtu, MWh 등)", example = "MMBtu")
    String unit,

    @ArraySchema(
        arraySchema = @Schema(description = "x축 날짜 목록 (모든 시리즈 공유)"),
        schema = @Schema(description = "날짜", example = "2025-12-01")
    )
    List<String> dates,

    @Schema(description = "기준선 시리즈 (CME, MI Base)")
    List<BaseSeries> baseSeries,

    @Schema(description = "시나리오별 예측 시리즈")
    List<ScenarioSeries> scenarioSeries

) {

    public record BaseSeries(

        @Schema(description = "시리즈 식별자", example = "CME")
        String seriesId,

        @Schema(description = "시리즈명", example = "CME")
        String seriesName,

        @Schema(description = "값 목록 (dates와 동일 순서)")
        List<BigDecimal> values

    ) {}

    public record ScenarioSeries(

        @Schema(description = "시나리오 ID", example = "11")
        Long scenarioId,

        @Schema(description = "시나리오명", example = "단기 봉쇄와 제한적 공급 대응")
        String scenarioName,

        @Schema(description = "BC(Base Case) 여부", example = "false")
        boolean baseCase,

        @Schema(description = "수급모델 실행 완료 여부", example = "true")
        boolean executed,

        @Schema(description = "값 목록 (dates와 동일 순서). 실행 전이면 null", nullable = true)
        List<BigDecimal> values

    ) {}

}
