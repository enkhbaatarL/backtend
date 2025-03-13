package com.sk.domain.shared.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * 공통 응답 field DTO 객체
 */
public record AuditInfo(

    @Schema(description = "등록자", example = "sk01234")
    String createUserId,

    @Schema(description = "등록일시", example = "2025-02-12 15:00:00")
    LocalDateTime createDt,

    @Schema(description = "수정자", example = "sk01234")
    String modifyUserId,

    @Schema(description = "수정일시", example = "2025-02-12 15:00:00")
    LocalDateTime modifyDt

) {

    public static AuditInfo of(String createUserId, LocalDateTime createDt, String modifyUserId, LocalDateTime modifyDt) {
        return new AuditInfo(createUserId, createDt, modifyUserId, modifyDt);
    }

}
