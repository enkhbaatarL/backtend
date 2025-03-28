package com.sk.domain.admin.dto.request.provider;

import io.swagger.v3.oas.annotations.media.Schema;

// @formatter:off
public record CategoryDataReq(

        @Schema(description = "카테고리 중분류 ID", example = "A_WOODMAC") 
        String CodeId) {

}
// @formatter:on