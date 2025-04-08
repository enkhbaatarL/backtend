package com.sk.domain.admin.dto.response.client;

import com.querydsl.core.annotations.QueryProjection;
import com.sk.common.utils.AESUtil;
import com.sk.common.utils.StringUtil;
import com.sk.domain.shared.dto.AuditInfo;
import com.sk.domain.shared.entity.master.tif.ClientAccessInfo;
import com.sk.domain.shared.entity.master.tmd.CustomerCompany;
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

    @Schema(description = "감사 정보")
    AuditInfo auditInfo

) {

    @QueryProjection
    public ClientAccessInfoRes(ClientAccessInfo clientAccessInfo,
                               String createUserNm, String createUserDeptNm, String createUserCpnyNm, String modifyUserNm, String modifyUserDeptNm, String modifyUserCpnyNm) {
        this(clientAccessInfo.getClientAccessRegNo(), clientAccessInfo.getClientCertId(), StringUtil.maskAll(clientAccessInfo.getClientSecretKey()), clientAccessInfo.getCustomerCompany().getCustomerCompanyNo(),
            clientAccessInfo.getCustomerCompany().getCustomerCompanyCd(), clientAccessInfo.getCustomerCompany().getCustomerCompanyNm(), clientAccessInfo.getCertTypeNm(), clientAccessInfo.getCertMethodNm(),
            AuditInfo.ofDetail(clientAccessInfo.getCreateUserId(), clientAccessInfo.getCreateDt(), clientAccessInfo.getModifyUserId(), clientAccessInfo.getModifyDt(), createUserNm, createUserDeptNm, createUserCpnyNm, modifyUserNm, modifyUserDeptNm, modifyUserCpnyNm));
    }

    @Builder
    public ClientAccessInfoRes(Integer clientAccessRegNo, String clientCertId, String clientSecretKey, Integer customerCompanyNo, String customerCompanyCd, String customerCompanyNm, String certTypeNm, String certMethodNm, AuditInfo auditInfo) {
        this.clientAccessRegNo = clientAccessRegNo;
        this.clientCertId = clientCertId;
        this.clientSecretKey = clientSecretKey;
        this.customerCompanyNo = customerCompanyNo;
        this.customerCompanyCd = customerCompanyCd;
        this.customerCompanyNm = customerCompanyNm;
        this.certTypeNm = certTypeNm;
        this.certMethodNm = certMethodNm;
        this.auditInfo = auditInfo;
    }

    public static ClientAccessInfoRes of(ClientAccessInfo clientAccessInfo, CustomerCompany customerCompany, String secretKey) {
        return ClientAccessInfoRes.builder()
            .clientAccessRegNo(clientAccessInfo.getClientAccessRegNo())
            .clientCertId(clientAccessInfo.getClientCertId())
            .clientSecretKey(AESUtil.decrypt(clientAccessInfo.getClientSecretKey(), secretKey)) // 복호화
            .customerCompanyNo(customerCompany.getCustomerCompanyNo())
            .customerCompanyCd(customerCompany.getCustomerCompanyCd())
            .customerCompanyNm(customerCompany.getCustomerCompanyNm())
            .certTypeNm(clientAccessInfo.getCertTypeNm())
            .certMethodNm(clientAccessInfo.getCertMethodNm())
            .auditInfo(AuditInfo.ofSimple(
                clientAccessInfo.getCreateUserId(),
                clientAccessInfo.getCreateDt(),
                clientAccessInfo.getModifyUserId(),
                clientAccessInfo.getModifyDt()
            ))
            .build();
    }

}
