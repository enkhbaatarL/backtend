package com.sk.domain.mi.dto.response.nextgen;

import io.swagger.v3.oas.annotations.media.Schema;

public record ModelParamColumn(

    @Schema(description = "Basecase Excel 컬럼 문자", example = "B")
    String column,

    @Schema(description = "컬럼 역할 (슬롯 축명 또는 value 필드 라벨)", example = "Year")
    String role
) {
}
