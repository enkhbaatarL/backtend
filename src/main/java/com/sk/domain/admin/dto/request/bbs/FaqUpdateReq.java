package com.sk.domain.admin.dto.request.bbs;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

// @formatter:off
public record FaqUpdateReq(

        @Schema(description = "카테고리", example = "CAT1")
        String bulletinCategorySeparateCode,

        @Size(min = 1, max = 200)
        @Schema(description = "제목", example = "게시물 제목")
        String bulletinTitleName,

        @Schema(description = "내용", example = "게시물 내용")
        String bulletinContent) {

}
// @formatter:on