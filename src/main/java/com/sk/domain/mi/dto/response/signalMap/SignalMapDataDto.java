package com.sk.domain.mi.dto.response.signalMap;

import java.math.BigDecimal;
import io.swagger.v3.oas.annotations.media.Schema;

// @formatter:off
public record SignalMapDataDto(
        @Schema(description = "Instrument명", example = "")
        String instrumentNm,
        
        @Schema(description = "상관관계계산값", example = "") 
        BigDecimal correlationCalculationVal
) {
    public SignalMapDataDto(String instrumentNm, BigDecimal correlationCalculationVal) {
        this.instrumentNm = instrumentNm;
        this.correlationCalculationVal = correlationCalculationVal;
    }
}
// @formatter:on
