package com.sk.domain.admin.dto.request.provider;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

// @formatter:off
public record UnstructuredDataLinkReq(

        @Schema(description = "링크 이름", example = "")
        String name,

        @NotBlank
        @Schema(description = "링크 주소", example = "")
        String url) {

}
// @formatter:om
