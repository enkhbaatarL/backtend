package com.sk.domain.mi.dto.response;

import com.sk.common.converter.BooleanToYNAttributeConverter;
import com.sk.domain.shared.entity.master.tmd.CustomerCompany;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import lombok.Builder;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.Comment;

public record MiCompanyDetailRes(

    @Schema(description = "고객회사번호")
    Integer customerCompanyNo,

    @Schema(description = "고객회사명")
    String customerCompanyNm,

    @Schema(description = "고객회사코드")
    String customerCompanyCd,

    @Schema(description = "고객회사MI플랫폼URL주소")
    String customerCompanyMipUrl,

    @Schema(description = "정성모델 표시여부")
    Boolean isQualitative,

    @Schema(description = "마리 표시여부")
    Boolean isMari,

    @Schema(description = "미래 기능 예시 표출 여부")
    Boolean isFuture
) {

    @Builder
    public MiCompanyDetailRes(Integer customerCompanyNo, String customerCompanyNm, String customerCompanyCd, String customerCompanyMipUrl,
        Boolean isQualitative, Boolean isMari, Boolean isFuture) {
        this.customerCompanyNo = customerCompanyNo;
        this.customerCompanyNm = customerCompanyNm;
        this.customerCompanyCd = customerCompanyCd;
        this.customerCompanyMipUrl = customerCompanyMipUrl;
        this.isQualitative = isQualitative;
        this.isMari = isMari;
        this.isFuture = isFuture;
    }

    public static MiCompanyDetailRes of(CustomerCompany customerCompany, String miDefaultDomain) {
        return MiCompanyDetailRes.builder()
            .customerCompanyNo(customerCompany.getCustomerCompanyNo())
            .customerCompanyNm(customerCompany.getCustomerCompanyNm())
            .customerCompanyCd(customerCompany.getCustomerCompanyCd())
            .customerCompanyMipUrl(customerCompany.getCustomerCompanyMipUrl() != null ? customerCompany.getCustomerCompanyMipUrl() : miDefaultDomain)
            .isQualitative(customerCompany.getIsQualitative())
            .isMari(customerCompany.getIsMari())
            .isFuture(customerCompany.getIsFuture())
            .build();
    }
}
