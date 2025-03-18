package com.sk.domain.admin.dto.response.company;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
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

    @Schema(description = "상품권한주체구분코드", example = "INDIVIDUAL", allowableValues = {"INDIVIDUAL", "COMPANY"})
    String goodsAuthManageCd,

    @JsonUnwrapped
    AuditInfo auditInfo

) {

    @Builder
    public CompanyGoodsMapRes(Integer companyNo, Integer goodsNo, boolean isUsed, String goodsAuthManageCd, AuditInfo auditInfo) {
        this.companyNo = companyNo;
        this.goodsNo = goodsNo;
        this.isUsed = isUsed;
        this.goodsAuthManageCd = goodsAuthManageCd;
        this.auditInfo = auditInfo;
    }

    public static CompanyGoodsMapRes of(CustomerCompanyGoodsMapping companyGoodsMapping) {
        return CompanyGoodsMapRes.builder()
            .companyNo(companyGoodsMapping.getId().getCustomerCompanyNo())
            .goodsNo(companyGoodsMapping.getId().getGoodsNo())
            .isUsed(companyGoodsMapping.getIsUsed())
            .goodsAuthManageCd(companyGoodsMapping.getId().getGoodsAuthManageSupportCd().getCode())
            .auditInfo(AuditInfo.of(companyGoodsMapping.getCreateUserId(), companyGoodsMapping.getCreateDt(), companyGoodsMapping.getModifyUserId(), companyGoodsMapping.getModifyDt()))
            .build();
    }

}
