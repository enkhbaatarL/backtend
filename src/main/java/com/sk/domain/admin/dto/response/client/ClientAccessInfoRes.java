package com.sk.domain.admin.dto.response.client;

import com.sk.domain.shared.entity.master.tif.ClientAccessInfo;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

public record ClientAccessInfoRes(

    @Schema(description = "클라이언트 접속 등록번호", example = "")
    Integer clientAccessRegNo,

    @Schema(description = "클라이언트 인증 ID", example = "")
    String clientCertId,

    @Schema(description = "클라이언트 시크릿키", example = "")
    String clientSecretKey,

    @Schema(description = "고객회사 번호", example = "")
    Integer customerCompanyNo,

    @Schema(description = "고객회사 코드", example = "")
    String customerCompanyCd,

    @Schema(description = "고객회사 명", example = "")
    String customerCompanyNm,

    @Schema(description = "인증유형 명", example = "")
    String certTypeNm,

    @Schema(description = "인증방법 명", example = "")
    String certMethodNm,

    @Schema(description = "허용권한범위 명", example = "")
    String permissionAuthScopeNm

) {

    @Builder
    public ClientAccessInfoRes(Integer clientAccessRegNo, String clientCertId, String clientSecretKey, Integer customerCompanyNo, String customerCompanyCd, String customerCompanyNm, String certTypeNm, String certMethodNm, String permissionAuthScopeNm) {
        this.clientAccessRegNo = clientAccessRegNo;
        this.clientCertId = clientCertId;
        this.clientSecretKey = clientSecretKey;
        this.customerCompanyNo = customerCompanyNo;
        this.customerCompanyCd = customerCompanyCd;
        this.customerCompanyNm = customerCompanyNm;
        this.certTypeNm = certTypeNm;
        this.certMethodNm = certMethodNm;
        this.permissionAuthScopeNm = permissionAuthScopeNm;
    }

    public static ClientAccessInfoRes of(ClientAccessInfo clientAccessInfo) {
        return ClientAccessInfoRes.builder()
            .clientAccessRegNo(clientAccessInfo.getClientAccessRegNo())
            .clientCertId(clientAccessInfo.getClientCertId())
            .clientSecretKey(clientAccessInfo.getClientSecretKey())
            .customerCompanyNo(clientAccessInfo.getCustomerCompany().getCustomerCompanyNo())
            .customerCompanyCd(clientAccessInfo.getCustomerCompany().getCustomerCompanyCd())
            .customerCompanyNm(clientAccessInfo.getCustomerCompany().getCustomerCompanyNm())
            .certTypeNm(clientAccessInfo.getCertTypeNm())
            .certMethodNm(clientAccessInfo.getCertMethodNm())
            .permissionAuthScopeNm(clientAccessInfo.getPermissionAuthScopeNm())
            .build();
    }

}
