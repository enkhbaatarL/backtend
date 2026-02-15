package com.sk.domain.mi.dto.response;

import com.sk.domain.shared.entity.master.tmd.CustomerCompany;
import com.sk.domain.shared.enums.news.MarketCategoryCode;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder(toBuilder = true)
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
    Boolean isFuture,

    @Schema(description = "뉴스시장카테고리코드", example = "CROIL")
    MarketCategoryCode newsMarketCategoryCode,

    @Schema(description = "뉴스레터 화면 표출 여부")
    Boolean isNewsletter

) {

    @Builder
    public MiCompanyDetailRes(Integer customerCompanyNo, String customerCompanyNm, String customerCompanyCd, String customerCompanyMipUrl,
        Boolean isQualitative, Boolean isMari, Boolean isFuture, Boolean isNewsletter) {
        this(
            customerCompanyNo,
            customerCompanyNm,
            customerCompanyCd,
            customerCompanyMipUrl,
            isQualitative,
            isMari,
            isFuture,
            null,
            isNewsletter
        );
    }

    public static MiCompanyDetailRes of(CustomerCompany customerCompany, String miDefaultDomain, String requestHost) {
        return MiCompanyDetailRes.builder()
            .customerCompanyNo(customerCompany.getCustomerCompanyNo())
            .customerCompanyNm(customerCompany.getCustomerCompanyNm())
            .customerCompanyCd(customerCompany.getCustomerCompanyCd())
            .customerCompanyMipUrl(customerCompany.getDynamicMipUrl(requestHost, miDefaultDomain))
            .isQualitative(customerCompany.getIsQualitative())
            .isMari(customerCompany.getIsMari())
            .isFuture(customerCompany.getIsFuture())
            .isNewsletter(customerCompany.getIsNewsLetter())
            .build();
    }
}
