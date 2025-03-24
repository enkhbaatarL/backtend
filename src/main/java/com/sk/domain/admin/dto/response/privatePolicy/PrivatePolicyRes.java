package com.sk.domain.admin.dto.response.privatePolicy;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

// @formatter:off
@Builder
public record PrivatePolicyRes(
        @Schema(description = "정책약관번호", example = "1") 
        Long poltacNo,

        @Schema(description = "정책약관버전명", example = "sample.jpg") 
        String poltacVerNm) {

}
// @formatter:on