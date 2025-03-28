package com.sk.domain.shared.dto;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
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
    LocalDateTime modifyDt,

    @JsonUnwrapped
    AuditExtension auditExtension

) {

    public static AuditInfo ofSimple(String createUserId, LocalDateTime createDt, String modifyUserId, LocalDateTime modifyDt) {
        return new AuditInfo(createUserId, createDt, modifyUserId, modifyDt, null);
    }

    public static AuditInfo ofDetail(String createUserId, LocalDateTime createDt, String modifyUserId, LocalDateTime modifyDt, String createUserNm, String createUserDeptNm, String createUserCpnyNm, String modifyUserNm, String modifyUserDeptNm, String modifyUserCpnyNm) {
        return new AuditInfo(createUserId, createDt, modifyUserId, modifyDt,
            new AuditExtension(createUserNm, createUserDeptNm, createUserCpnyNm, modifyUserNm, modifyUserDeptNm, modifyUserCpnyNm));
    }

}
