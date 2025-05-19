package com.sk.domain.mi.dto.response.bbs;

import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;

// @formatter:off
public record MiNoticeRes(
        @Schema(description = "게시물번호", example = "1") 
        Integer bulletinNo,

        @Schema(description = "고정", example = "true") 
        Boolean isTopFixed,

        @Schema(description = "카테고리", example = "CATEGORY1") 
        String bulletinCategorySeparateCode,

        @Schema(description = "제목", example = "제목") 
        String bulletinTitleName,

        @Schema(description = "등록일자", example = "2023-10-01 12:00:00")
        LocalDateTime createDt,
        
        @Schema(description = "읽음 처리", example = "false")
        Boolean isRead
        ) {    

}
// @formatter:on