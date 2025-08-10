package com.sk.domain.shared.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record AuditExtension(

    @Schema(description = "등록자명", example = "아이유")
    String createUserNm,

    @Schema(description = "부서명", example = "ESG플랫폼사업팀")
    String createUserDeptNm,

    @Schema(description = "회사명", example = "SK이노베이션")
    String createUserCpnyNm,

    @Schema(description = "수정자명", example = "아이유")
    String modifyUserNm,

    @Schema(description = "부서명", example = "ESG플랫폼사업팀")
    String modifyUserDeptNm,

    @Schema(description = "회사명", example = "SK이노베이션")
    String modifyUserCpnyNm

) {

    public AuditExtension(String createUserNm, String createUserDeptNm, String createUserCpnyNm, String modifyUserNm, String modifyUserDeptNm, String modifyUserCpnyNm) {
        this.createUserNm = createUserNm;
        this.createUserDeptNm = createUserDeptNm;
        this.createUserCpnyNm = createUserCpnyNm;
        this.modifyUserNm = modifyUserNm;
        this.modifyUserDeptNm = modifyUserDeptNm;
        this.modifyUserCpnyNm = modifyUserCpnyNm;
    }

    public static AuditExtension of(String createUserNm, String createUserDeptNm, String createUserCpnyNm, String modifyUserNm, String modifyUserDeptNm, String modifyUserCpnyNm) {
        return new AuditExtension(createUserNm, createUserDeptNm, createUserCpnyNm, modifyUserNm, modifyUserDeptNm, modifyUserCpnyNm);
    }

}
