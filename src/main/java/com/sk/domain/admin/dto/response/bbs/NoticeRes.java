package com.sk.domain.admin.dto.response.bbs;

import io.swagger.v3.oas.annotations.media.Schema;

// @formatter:off
public record NoticeRes(
        @Schema(description = "게시물번호", example = "1") 
        Integer bulletinNo,

        @Schema(description = "고정", example = "true") 
        Boolean isTopFixed,

        @Schema(description = "팝업사용", example = "true") 
        Boolean isPopupUse,

        @Schema(description = "카테고리", example = "카테고리") 
        String bulletinCategorySeparateCode,

        @Schema(description = "제목", example = "제목") 
        String bulletinTitleName,

        @Schema(description = "등록자 회사", example = "SK주식회사 C&C") 
        String createUserCompany,

        @Schema(description = "등록자 팀", example = "ESG플랫폼사업팀") 
        String createUserTeam,

        @Schema(description = "등록자", example = "홍길동") 
        String createUserName,

        @Schema(description = "등록일", example = "20250212") 
        String createDt,

        @Schema(description = "게시여부", example = "게시중") 
        String status,

        @Schema(description = "게시대상", example = "SK 실트론, SK 하이닉스") 
        String target) {
}
// @formatter:on