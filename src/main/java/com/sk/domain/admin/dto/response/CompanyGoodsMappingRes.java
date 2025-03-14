package com.sk.domain.admin.dto.response;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.sk.domain.shared.dto.AuditInfo;
import com.sk.domain.shared.entity.master.tam.CustomerCompanyGoodsMapping;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

public record CompanyGoodsMappingRes(

    @Schema(description = "회사번호", example = "")
    Integer companyNo,

    @Schema(description = "상품번호", example = "")
    Integer goodsNo,

    @Schema(description = "사용여부", example = "")
    boolean isUsed,

    @Schema(description = "상품권한주체구분코드", example = "")
    String goodsAuthManageCd,

    @JsonUnwrapped
    @Schema(description = "감사 정보")
    AuditInfo auditInfo

) {

    @Builder
    public CompanyGoodsMappingRes(Integer companyNo, Integer goodsNo, boolean isUsed, String goodsAuthManageCd, AuditInfo auditInfo) {
        this.companyNo = companyNo;
        this.goodsNo = goodsNo;
        this.isUsed = isUsed;
        this.goodsAuthManageCd = goodsAuthManageCd;
        this.auditInfo = auditInfo;
    }

    public static CompanyGoodsMappingRes of(CustomerCompanyGoodsMapping companyGoodsMapping) {
        return CompanyGoodsMappingRes.builder()
            .companyNo(companyGoodsMapping.getId().getCustomerCompanyNo())
            .goodsNo(companyGoodsMapping.getId().getGoodsNo())
            .isUsed(companyGoodsMapping.getIsUsed())
            .goodsAuthManageCd(companyGoodsMapping.getId().getGoodsAuthManageSupportCd().getCode())
            .auditInfo(AuditInfo.of(companyGoodsMapping.getCreateUserId(), companyGoodsMapping.getCreateDt(), companyGoodsMapping.getModifyUserId(), companyGoodsMapping.getModifyDt()))
            .build();
    }

}
