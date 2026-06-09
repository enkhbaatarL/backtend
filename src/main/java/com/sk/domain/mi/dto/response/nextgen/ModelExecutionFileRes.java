package com.sk.domain.mi.dto.response.nextgen;

import io.swagger.v3.oas.annotations.media.Schema;

public record ModelExecutionFileRes(

    @Schema(description = "파일번호. DB-backed 구현에서는 `tcm_filel.file_no`와 매핑된다.", example = "1001")
    Long fileNo,

    @Schema(description = "1부터 시작하는 업로드 순서", example = "1")
    Integer fileIndex,

    @Schema(description = "원본 파일명", example = "input_1.xlsx")
    String originalFileName,

    @Schema(description = "multipart content type", example = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    String contentType,

    @Schema(description = "파일 크기 byte", example = "12345")
    Long size

) {
}
