package com.sk.domain.admin.dto.response.mcp;

import com.querydsl.core.annotations.QueryProjection;
import com.sk.domain.shared.dto.AuditInfo;
import com.sk.domain.shared.entity.master.tmd.CustomerCompany;
import com.sk.domain.shared.entity.master.tmd.McpAuthInfo;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

/** [MCP 인증] MCP 인증 키 목록·단건 응답. 시크릿은 앞자리만 포함한다. */
public record McpAuthInfoRes(

    @Schema(description = "MCP 인증 등록번호", example = "1")
    Integer mcpAuthRegNo,

    @Schema(description = "MCP 인증 ID", example = "skcc-mcp")
    String mcpAuthId,

    @Schema(description = "시크릿 앞자리 (식별용, 시크릿은 다시 조회할 수 없습니다)", example = "Ab3x")
    String secretPrefix,

    @Schema(description = "고객회사 번호", example = "2014")
    Integer customerCompanyNo,

    @Schema(description = "고객회사 코드", example = "SKCC")
    String customerCompanyCd,

    @Schema(description = "고객회사 명", example = "에스케이씨씨")
    String customerCompanyNm,

    @Schema(description = "관리회사 여부", example = "false")
    Boolean isOwnerCompany,

    @Schema(description = "감사 정보")
    AuditInfo auditInfo

) {

    @QueryProjection
    public McpAuthInfoRes(McpAuthInfo mcpAuthInfo,
                          String createUserNm, String createUserDeptNm, String createUserCpnyNm, String modifyUserNm, String modifyUserDeptNm, String modifyUserCpnyNm) {
        this(mcpAuthInfo.getMcpAuthRegNo(), mcpAuthInfo.getMcpAuthId(), mcpAuthInfo.getSecretPrefixValue(), mcpAuthInfo.getCustomerCompany().getCustomerCompanyNo(),
            mcpAuthInfo.getCustomerCompany().getCustomerCompanyCd(), mcpAuthInfo.getCustomerCompany().getCustomerCompanyNm(), mcpAuthInfo.getCustomerCompany().getIsMipManagementCompany(),
            AuditInfo.ofDetail(mcpAuthInfo.getCreateUserId(), mcpAuthInfo.getCreateDt(), mcpAuthInfo.getModifyUserId(), mcpAuthInfo.getModifyDt(), createUserNm, createUserDeptNm, createUserCpnyNm, modifyUserNm, modifyUserDeptNm, modifyUserCpnyNm));
    }

    @Builder
    public McpAuthInfoRes(Integer mcpAuthRegNo, String mcpAuthId, String secretPrefix, Integer customerCompanyNo, String customerCompanyCd, String customerCompanyNm, Boolean isOwnerCompany, AuditInfo auditInfo) {
        this.mcpAuthRegNo = mcpAuthRegNo;
        this.mcpAuthId = mcpAuthId;
        this.secretPrefix = secretPrefix;
        this.customerCompanyNo = customerCompanyNo;
        this.customerCompanyCd = customerCompanyCd;
        this.customerCompanyNm = customerCompanyNm;
        this.isOwnerCompany = isOwnerCompany;
        this.auditInfo = auditInfo;
    }

    public static McpAuthInfoRes of(McpAuthInfo mcpAuthInfo, CustomerCompany customerCompany) {
        return McpAuthInfoRes.builder()
            .mcpAuthRegNo(mcpAuthInfo.getMcpAuthRegNo())
            .mcpAuthId(mcpAuthInfo.getMcpAuthId())
            .secretPrefix(mcpAuthInfo.getSecretPrefixValue())
            .customerCompanyNo(customerCompany.getCustomerCompanyNo())
            .customerCompanyCd(customerCompany.getCustomerCompanyCd())
            .customerCompanyNm(customerCompany.getCustomerCompanyNm())
            .isOwnerCompany(customerCompany.getIsMipManagementCompany())
            .auditInfo(AuditInfo.ofSimple(
                mcpAuthInfo.getCreateUserId(),
                mcpAuthInfo.getCreateDt(),
                mcpAuthInfo.getModifyUserId(),
                mcpAuthInfo.getModifyDt()
            ))
            .build();
    }

}
