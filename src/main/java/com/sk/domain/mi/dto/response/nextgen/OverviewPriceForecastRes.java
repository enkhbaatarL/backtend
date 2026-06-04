package com.sk.domain.mi.dto.response.nextgen;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.List;

public record OverviewPriceForecastRes(

    @Schema(description = "인덱스명", example = "JKM")
    String indexName,

    @Schema(description = "통화", example = "USD")
    String currency,

    @Schema(description = "단위", example = "MMBtu")
    String unit,

    @ArraySchema(
        arraySchema = @Schema(description = "월별 날짜 목록 (모든 시나리오 공유)"),
        schema = @Schema(description = "년-월", example = "2023-01")
    )
    List<String> dates,

    @Schema(description = "시나리오별 가격 예측 시리즈")
    List<ScenarioForecastLine> series

) {

    public record ScenarioForecastLine(

        @Schema(description = "시나리오 ID", example = "1")
        Long scenarioId,

        @Schema(description = "시나리오명 (범례 표시용)", example = "호르무즈 봉쇄 3개월+ 유지")
        String scenarioName,

        @Schema(description = "BC(Base Case) 여부", example = "true")
        boolean baseCase,

        @Schema(description = "월별 가격 값 (dates와 동일 순서)")
        List<BigDecimal> values

    ) {}

}
