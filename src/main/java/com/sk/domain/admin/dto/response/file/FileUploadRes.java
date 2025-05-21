package com.sk.domain.admin.dto.response.file;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

// @formatter:off
public record FileUploadRes(
        @Schema(description = "파일번호", example = "1")
        Long fileNo,

        @Schema(description = "파일명", example = "sample.jpg")
        String fileNm,

        @Schema(description = "파일크기(Byte)", example = "1024")
        Long fileSize,

        @Schema(description = "파일URL", example = "")
        String fileUrl) {

    @Builder
    public FileUploadRes(Long fileNo, String fileNm, Long fileSize, String fileUrl) {
        this.fileNo = fileNo;
        this.fileNm = fileNm;
        this.fileSize = fileSize;
        this.fileUrl = fileUrl;
    }

    public static FileUploadRes of(Long fileNo, String fileNm, Long fileSize, String fileUrl) {
        return FileUploadRes.builder()
                .fileNo(fileNo)
                .fileNm(fileNm)
                .fileSize(fileSize)
                .fileUrl(fileUrl)
                .build();
    }
}
// @formatter:on