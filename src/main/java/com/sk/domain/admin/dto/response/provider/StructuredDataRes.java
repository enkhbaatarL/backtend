package com.sk.domain.admin.dto.response.provider;

import java.time.LocalDateTime;

import com.querydsl.core.annotations.QueryProjection;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

// @formatter:off
public record StructuredDataRes(
        
        @Schema(description = "컴포넌트유형명", example = "") 
        String componentTypeNm,

        @Schema(description = "등록일시", example = "2025-02-12 15:00:00")
        LocalDateTime createDt) 
{
        @Builder
        @QueryProjection
        public StructuredDataRes(String componentTypeNm, LocalDateTime createDt) {
                this.componentTypeNm = componentTypeNm;
                this.createDt = createDt;
        }
}
// @formatter:on