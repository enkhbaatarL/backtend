package com.sk.domain.mi.dto.response.bbs;

import io.swagger.v3.oas.annotations.media.Schema;

// @formatter:off
public record MiFaqRes(
        @Schema(description = "게시물번호", example = "1") 
        Integer bulletinNo,

        @Schema(description = "제목", example = "관리자에서 등록한 FAQ 제목") 
        String bulletinTitleNm,

        @Schema(description = "내용", example = "관리자에서 등록한 FAQ 내용") 
        String bulletinCntt) {
}
// @formatter:on