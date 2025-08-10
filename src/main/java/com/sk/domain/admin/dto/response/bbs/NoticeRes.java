package com.sk.domain.admin.dto.response.bbs;

import com.sk.domain.shared.dto.AuditInfo;

import io.swagger.v3.oas.annotations.media.Schema;

// @formatter:off
public record NoticeRes(
        @Schema(description = "게시물번호", example = "1") 
        Integer bulletinNo,

        @Schema(description = "고정", example = "true") 
        Boolean isTopFixed,

        @Schema(description = "팝업사용", example = "true") 
        Boolean isPopupUse,

        @Schema(description = "카테고리", example = "CATEGORY1") 
        String bulletinCategorySeparateCode,

        @Schema(description = "제목", example = "제목") 
        String bulletinTitleName,

        @Schema(description = "게시여부", example = "ACTIVE",  allowableValues = {"ACTIVE", "INAVTIVE" }) 
        String status,

        @Schema(description = "게시대상", example = "SK 실트론, SK 하이닉스") 
        String target,
        
        @Schema(description = "감사 정보")
        AuditInfo auditInfo) {
}
// @formatter:on