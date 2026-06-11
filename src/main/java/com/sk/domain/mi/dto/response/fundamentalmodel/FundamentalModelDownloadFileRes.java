package com.sk.domain.mi.dto.response.fundamentalmodel;

import io.swagger.v3.oas.annotations.media.Schema;

public record FundamentalModelDownloadFileRes(

    @Schema(description = "파일번호", example = "1001")
    Long fileNo,

    @Schema(description = "파일명. /api/v1/file-management/download/file 요청의 fileNm으로 전달합니다.", example = "input_1.xlsx")
    String fileNm,

    @Schema(description = "파일 크기 byte. /api/v1/file-management/download/file 요청의 fileSize로 전달합니다.", example = "12345")
    Long fileSize

) {
}
