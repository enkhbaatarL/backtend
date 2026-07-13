package com.sk.domain.mi.dto.response.nextgen;

import io.swagger.v3.oas.annotations.media.Schema;

public record ModelExecutionInputFileRes(

    @Schema(description = "입력 파일 role", allowableValues = {"DEMAND", "SUPPLY", "TRADE"}, example = "DEMAND")
    String fileRole,

    @Schema(description = "파일 번호", example = "991001")
    Long fileNo,

    @Schema(description = "원본 파일명", example = "DEMAND_88001.xlsx")
    String fileNm,

    @Schema(description = "파일 크기(byte)", example = "1048576")
    Long fileSize

) {
}
