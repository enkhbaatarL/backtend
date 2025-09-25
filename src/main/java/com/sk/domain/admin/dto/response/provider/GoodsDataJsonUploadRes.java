package com.sk.domain.admin.dto.response.provider;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

// @formatter:off
public record GoodsDataJsonUploadRes(
        @Schema(description = "컴포넌트 타입 ID", example = "TYPE_ID")
        String componentTypeId,
        @Schema(description = "데이터 수", example = "1024")
        Long size
){

    @Builder
    public GoodsDataJsonUploadRes(String componentTypeId, Long size) {
        this.componentTypeId = componentTypeId;
        this.size = size;
    }

    public static GoodsDataJsonUploadRes of(String componentTypeId, Long size) {
        return GoodsDataJsonUploadRes.builder()
                .componentTypeId(componentTypeId)
                .size(size)
                .build();
    }
}
// @formatter:on
