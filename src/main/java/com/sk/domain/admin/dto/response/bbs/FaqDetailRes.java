package com.sk.domain.admin.dto.response.bbs;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

// @formatter:off
public record FaqDetailRes(
        @Schema(description = "카테고리", example = "CAT1") 
        String bulletinCategorySeparateCd,

        @Schema(description = "제목", example = "게시물 제목") 
        String bulletinTitleNm,

        @Schema(description = "내용", example = "게시물 내용") 
        String bulletinCntt) {

    @Builder
    public FaqDetailRes(String bulletinCategorySeparateCd, String bulletinTitleNm, String bulletinCntt) {
        this.bulletinCategorySeparateCd = bulletinCategorySeparateCd;
        this.bulletinTitleNm = bulletinTitleNm;
        this.bulletinCntt = bulletinCntt;
    }

    public static FaqDetailRes of(String bulletinCategorySeparateCd, String bulletinTitleNm, String bulletinCntt) {
        return new FaqDetailRes(bulletinCategorySeparateCd, bulletinTitleNm, bulletinCntt);
    }
}
// @formatter:on