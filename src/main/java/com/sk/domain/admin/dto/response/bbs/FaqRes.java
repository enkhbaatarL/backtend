package com.sk.domain.admin.dto.response.bbs;

import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;

// @formatter:off
public record FaqRes(
        @Schema(description = "게시물번호", example = "1") 
        Integer bulletinNo,

        @Schema(description = "카테고리", example = "CATEGORY1") 
        String bulletinCategorySeparateCode,

        @Schema(description = "제목", example = "제목") 
        String bulletinTitleName,

        @Schema(description = "등록자 회사", example = "SK주식회사 C&C") 
        String createUserCompany,

        @Schema(description = "등록자 팀", example = "ESG플랫폼사업팀") 
        String createUserTeam,

        @Schema(description = "등록자", example = "홍길동") 
        String createUserName,

        @Schema(description = "등록일", example = "2025-02-12 15:00:00") 
        LocalDateTime createDt) {
}
// @formatter:on