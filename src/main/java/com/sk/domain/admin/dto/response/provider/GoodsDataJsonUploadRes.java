package com.sk.domain.admin.dto.response.provider;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

// @formatter:off
public record GoodsDataJsonUploadRes(
        @Schema(description = "컴포넌트 타입 ID", example = "TYPE_ID")
        String componentTypeId
){

    @Builder
    public GoodsDataJsonUploadRes(String componentTypeId) {
        this.componentTypeId = componentTypeId;
    }

    public static GoodsDataJsonUploadRes of(String componentTypeId, Boolean isSuccess) {
        return GoodsDataJsonUploadRes.builder()
                .componentTypeId(componentTypeId)
                .build();
    }
}
// @formatter:on
