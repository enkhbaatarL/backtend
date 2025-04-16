package com.sk.domain.admin.dto.response.file;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

// @formatter:off
@Builder
public record GoodsDataFileUploadRes(
        @Schema(description = "파일번호", example = "1")
        Long fileNo,

        @Schema(description = "컴포넌트 타입 ID", example = "TYPE_ID")
        String componentTypeId,

        @Schema(description = "파일명", example = "sample.jpg")
        String fileNm,

        @Schema(description = "파일크기(Byte)", example = "1024")
        Long fileSize,

        @Schema(description = "파일URL", example = "")
        String fileUrl) {

    public static GoodsDataFileUploadRes of(Long fileNo, String componentTypeId, String fileNm, Long fileSize, String fileUrl) {
        return GoodsDataFileUploadRes.builder()
                .fileNo(fileNo)
                .componentTypeId(componentTypeId)
                .fileNm(fileNm)
                .fileSize(fileSize)
                .fileUrl(fileUrl)
                .build();
    }
}
// @formatter:on