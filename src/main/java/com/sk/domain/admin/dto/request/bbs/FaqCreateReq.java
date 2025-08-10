package com.sk.domain.admin.dto.request.bbs;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// @formatter:off
public record FaqCreateReq(

        @NotBlank
        @Schema(description = "카테고리", example = "CATEGORY1")
        String bulletinCategorySeparateCd,

        @NotBlank
        @Size(min = 1, max = 200)
        @Schema(description = "제목", example = "게시물 제목")
        String bulletinTitleNm,

        @NotBlank
        @Schema(description = "내용", example = "게시물 내용")
        String bulletinCntt) {

}
// @formatter:on