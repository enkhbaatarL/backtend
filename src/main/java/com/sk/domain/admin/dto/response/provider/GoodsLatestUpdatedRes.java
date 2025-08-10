package com.sk.domain.admin.dto.response.provider;

import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;

// @formatter:off
public record GoodsLatestUpdatedRes(
        @Schema(description = "수정일시", example = "2025-02-12 15:00:00")
        LocalDateTime modifyDt,

        @Schema(description = "수정자이름", example = "홍길동")
        String modifyUserName) {

}
// @formatter:on