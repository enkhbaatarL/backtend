package com.sk.domain.admin.dto.response.company;

import com.sk.domain.shared.dto.AuditInfo;
import com.sk.domain.shared.entity.master.tam.CustomerCompanyGoodsMapping;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

public record CompanyGoodsMapRes(

    @Schema(description = "회사번호", example = "2005")
    Integer companyNo,

    @Schema(description = "상품번호", example = "159")
    Integer goodsNo,

    @Schema(description = "사용여부", example = "true")
    boolean isUsed,

    @Schema(description = "상품권한 주체구분 코드", example = "INDIVIDUAL", allowableValues = {"COMPANY", "INDIVIDUAL"})
    String goodsAuthManagementSeparateCd,

    @Schema(description = "상품권한 주체구분 명", example = "개인", allowableValues = {"회사", "개인"})
    String goodsAuthManagementSeparateNm,

    @Schema(description = "감사 정보")
    AuditInfo auditInfo

) {

    @Builder
    public CompanyGoodsMapRes(Integer companyNo, Integer goodsNo, boolean isUsed, String goodsAuthManagementSeparateCd, String goodsAuthManagementSeparateNm, AuditInfo auditInfo) {
        this.companyNo = companyNo;
        this.goodsNo = goodsNo;
        this.isUsed = isUsed;
        this.goodsAuthManagementSeparateCd = goodsAuthManagementSeparateCd;
        this.goodsAuthManagementSeparateNm = goodsAuthManagementSeparateNm;
        this.auditInfo = auditInfo;
    }

    public static CompanyGoodsMapRes of(CustomerCompanyGoodsMapping entity) {
        return CompanyGoodsMapRes.builder()
            .companyNo(entity.getId().getCustomerCompanyNo())
            .goodsNo(entity.getId().getGoodsNo())
            .isUsed(entity.getIsUsed())
            .goodsAuthManagementSeparateCd(entity.getId().getGoodsAuthManagementSeparateCd().getCode())
            .goodsAuthManagementSeparateNm(entity.getId().getGoodsAuthManagementSeparateCd().getDescription())
            .auditInfo(AuditInfo.ofDetail(
                entity.getCreateUserId(),
                entity.getCreateDt(),
                entity.getModifyUserId(),
                entity.getModifyDt(),
                entity.accessCreatorNm(),
                entity.accessCreatorDeptNm(),
                entity.accessCreatorCompanyNm(),
                entity.accessModifierNm(),
                entity.accessModifierDeptNm(),
                entity.accessModifierCompanyNm()
            ))
            .build();
    }

    public static CompanyGoodsMapRes ofDefault(CustomerCompanyGoodsMapping entity) {
        return CompanyGoodsMapRes.builder()
            .companyNo(entity.getId().getCustomerCompanyNo())
            .goodsNo(entity.getId().getGoodsNo())
            .isUsed(entity.getIsUsed())
            .goodsAuthManagementSeparateCd(entity.getId().getGoodsAuthManagementSeparateCd().getCode())
            .goodsAuthManagementSeparateNm(entity.getId().getGoodsAuthManagementSeparateCd().getDescription())
            .auditInfo(AuditInfo.ofSimple(
                entity.getCreateUserId(),
                entity.getCreateDt(),
                entity.getModifyUserId(),
                entity.getModifyDt()
            ))
            .build();
    }

}
