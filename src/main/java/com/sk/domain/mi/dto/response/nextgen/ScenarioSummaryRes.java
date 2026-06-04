package com.sk.domain.mi.dto.response.nextgen;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

public record ScenarioSummaryRes(

    @Schema(description = "인덱스별 요약 (JKM, TTF, HH)")
    List<ScenarioIndexOutlookRes> indexOutlooks,

    @Schema(description = "가격 예측 라인차트 데이터")
    ScenarioPriceForecastRes priceForecast

) {
}
