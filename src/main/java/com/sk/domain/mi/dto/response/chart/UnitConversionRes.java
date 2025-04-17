package com.sk.domain.mi.dto.response.chart;

import io.swagger.v3.oas.annotations.media.Schema;

public record UnitConversionRes(

    @Schema(description = "단위 변환 값 단위 코드", example = "KILOGRAM")
    String unitConversionUnitCd,

    @Schema(description = "단위 변환 값 단위", example = "kg")
    String unitConversionUnit

) {
}
