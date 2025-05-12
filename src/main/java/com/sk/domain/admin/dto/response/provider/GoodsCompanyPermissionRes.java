package com.sk.domain.admin.dto.response.provider;

import com.querydsl.core.annotations.QueryProjection;
import com.sk.domain.shared.dto.AuditInfo;
import com.sk.domain.shared.enums.GoodsAuthManagementSeparateCd;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.LocalDateTime;

public record GoodsCompanyPermissionRes(

    @Schema(description = "회사 번호", example = "1004")
    Integer companyNo,

    @Schema(description = "회사 명", example = "SK 실트론")
    String companyNm,

    @Schema(description = "사용여부", example = "true")
    Boolean isUsed,

    @Schema(description = "권한주체구분코드", example = "INDIVIDUAL", allowableValues = {"COMPANY", "INDIVIDUAL"})
    String goodsAuthManagementSeparateCd,

    @Schema(description = "권한주체구분코드 명", example = "개인", allowableValues = {"회사", "개인"})
    String goodsAuthManagementSeparateNm,

    @Schema(description = "조회 권한 정보")
    PermissionInfo viewPermission,

    @Schema(description = "다운로드 권한 정보")
    PermissionInfo downloadPermission,

    AuditInfo auditInfo

) {

    @QueryProjection
    public GoodsCompanyPermissionRes(Integer companyNo, String companyNm, Boolean isUsed, GoodsAuthManagementSeparateCd goodsAuthManagementSeparateCd,
                                     Boolean viewExistsForCompany, Long viewCountForIndividual, Boolean downloadExistsForCompany, Long downloadCountForIndividual,
                                     String createUserId, LocalDateTime createDt, String modifyUserId, LocalDateTime modifyDt,
                                     String createUserNm, String createUserDeptNm, String createUserCpnyNm, String modifyUserNm, String modifyUserDeptNm, String modifyUserCpnyNm) {
        this(companyNo, companyNm, isUsed, goodsAuthManagementSeparateCd.getCode(), goodsAuthManagementSeparateCd.getDescription(),
            // viewPermission 로직
            switch (goodsAuthManagementSeparateCd) {
                case COMPANY -> viewExistsForCompany != null && viewExistsForCompany ? PermissionInfo.all() : PermissionInfo.none();
                case INDIVIDUAL -> viewCountForIndividual != null && viewCountForIndividual > 0 ? PermissionInfo.some(viewCountForIndividual) : PermissionInfo.some(0);
            },
            // downloadPermission 로직
            switch (goodsAuthManagementSeparateCd) {
                case COMPANY -> downloadExistsForCompany != null && downloadExistsForCompany ? PermissionInfo.all() : PermissionInfo.none();
                case INDIVIDUAL -> downloadCountForIndividual != null && downloadCountForIndividual > 0 ? PermissionInfo.some(downloadCountForIndividual) : PermissionInfo.some(0);
            },
            AuditInfo.ofDetail(createUserId, createDt, modifyUserId, modifyDt, createUserNm, createUserDeptNm, createUserCpnyNm, modifyUserNm, modifyUserDeptNm, modifyUserCpnyNm));
    }

    @Builder
    public GoodsCompanyPermissionRes(Integer companyNo, String companyNm, Boolean isUsed, String goodsAuthManagementSeparateCd, String goodsAuthManagementSeparateNm, PermissionInfo viewPermission, PermissionInfo downloadPermission, AuditInfo auditInfo) {
        this.companyNo = companyNo;
        this.companyNm = companyNm;
        this.isUsed = isUsed;
        this.goodsAuthManagementSeparateCd = goodsAuthManagementSeparateCd;
        this.goodsAuthManagementSeparateNm = goodsAuthManagementSeparateNm;
        this.viewPermission = viewPermission;
        this.downloadPermission = downloadPermission;
        this.auditInfo = auditInfo;
    }

    public enum PermissionType {
        NONE,
        SOME,
        ALL
    }

    public record PermissionInfo(

        @Schema(description = "권한 상태", example = "ALL", allowableValues = {"ALL", "NONE", "SOME"})
        PermissionType type,

        @Schema(description = "권한을 가진 회원 수 (type = 'SOME'일 때만 존재한다.)", example = "14")
        Long count

    ) {

        public PermissionInfo(PermissionType type, Long count) {
            this.type = type;
            this.count = count;
        }

        public static PermissionInfo all() {
            return new PermissionInfo(PermissionType.ALL, null);
        }

        public static PermissionInfo none() {
            return new PermissionInfo(PermissionType.NONE, null);
        }

        public static PermissionInfo some(long cnt) {
            return new PermissionInfo(PermissionType.SOME, cnt);
        }

    }

}
