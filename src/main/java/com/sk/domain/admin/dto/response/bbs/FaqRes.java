package com.sk.domain.admin.dto.response.bbs;

import com.sk.domain.shared.dto.AuditInfo;
import io.swagger.v3.oas.annotations.media.Schema;

// @formatter:off
public record FaqRes(
        @Schema(description = "게시물번호", example = "1") 
        Integer bulletinNo,

        @Schema(description = "카테고리", example = "CATEGORY1") 
        String bulletinCategorySeparateCd,

        @Schema(description = "제목", example = "제목") 
        String bulletinTitleNm,

        @Schema(description = "감사 정보")
        AuditInfo auditInfo) {
}
// @formatter:on