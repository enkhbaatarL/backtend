package com.sk.domain.mi.dto.response.scenarioworkbench;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.List;

public record ScenarioPriceForecastRes(

    @Schema(description = "통화 (USD, EUR, KRW 등)", example = "USD")
    String currency,

    @Schema(description = "단위 (MMBtu=백만 영국 열량 단위, MT=메트릭톤)", example = "MMBtu")
    String unit,

    @ArraySchema(
        arraySchema = @Schema(description = "월별 날짜 목록 (모든 시리즈 공유)"),
        schema = @Schema(description = "년-월", example = "2023-01")
    )
    List<String> dates,

    @Schema(description = "인덱스별 가격 시리즈")
    List<PriceSeries> series

) {

    public record PriceSeries(
        @Schema(description = "인덱스명", example = "JKM")
        String indexName,

        @Schema(description = "월별 가격 값 (dates와 동일 순서)")
        List<BigDecimal> values
    ) {}

}
