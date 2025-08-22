package com.sk.domain.mi.dto.response.signalMap;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.math.BigDecimal;
import io.swagger.v3.oas.annotations.media.Schema;

// @formatter:off
@JsonInclude(JsonInclude.Include.NON_NULL)
public record SignalMapDataDto(
        @Schema(description = "Instrument명", example = "")
        String instrumentNm,

        @Schema(description = "상관관계계산값", example = "")
        BigDecimal correlationCalculationVal,

        @Schema(description = "Group, Category 와 동일", example = "")
        String categoryName
) {
    public SignalMapDataDto(String instrumentNm, BigDecimal correlationCalculationVal, String categoryName) {
        this.instrumentNm = instrumentNm;
        this.correlationCalculationVal = correlationCalculationVal;
        this.categoryName = categoryName;
    }
}
// @formatter:on
