package com.sk.domain.admin.dto.response.provider;

import com.sk.domain.shared.entity.master.tmd.GoodsDataCategory;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

// @formatter:off
@Builder
public record CategoryDataRes(
        @Schema(description = "코드ID", example = "")
        String codeId) {
}
// @formatter:on