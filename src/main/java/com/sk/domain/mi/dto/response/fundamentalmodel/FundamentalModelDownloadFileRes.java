package com.sk.domain.mi.dto.response.fundamentalmodel;

import io.swagger.v3.oas.annotations.media.Schema;

public record FundamentalModelDownloadFileRes(

    @Schema(description = "공통 File API 다운로드 요청의 파일번호", example = "1001")
    Long fileNo,

    @Schema(description = "공통 File API 다운로드 요청의 파일명", example = "output.xlsx")
    String fileNm,

    @Schema(description = "공통 File API 다운로드 요청의 파일 크기(byte)", example = "12345")
    Long fileSize

) {
}
