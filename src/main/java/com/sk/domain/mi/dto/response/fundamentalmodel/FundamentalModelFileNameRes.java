package com.sk.domain.mi.dto.response.fundamentalmodel;

import io.swagger.v3.oas.annotations.media.Schema;

public record FundamentalModelFileNameRes(

    @Schema(description = "화면에 표시할 input 파일명", example = "input_1.xlsx")
    String fileNm

) {
}
