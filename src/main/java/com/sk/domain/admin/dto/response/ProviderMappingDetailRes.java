package com.sk.domain.admin.dto.response;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.sk.domain.shared.dto.AuditInfo;
import com.sk.domain.shared.entity.master.tam.CustomerCompanyProviderMapping;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

public record ProviderMappingDetailRes(

    @JsonUnwrapped
    @Schema(description = "고객사-프로바이더 매핑정보")
    ProviderMappingRes providerMappingRes,

    @Schema(description = "프로바이더 기본 설명")
    String providerExpn,

    @JsonUnwrapped
    @Schema(description = "감사 정보")
    AuditInfo auditInfo

) {

    @Builder
    public ProviderMappingDetailRes(ProviderMappingRes providerMappingRes, String providerExpn, AuditInfo auditInfo) {
        this.providerMappingRes = providerMappingRes;
        this.providerExpn = providerExpn;
        this.auditInfo = auditInfo;
    }

    public static ProviderMappingDetailRes of(CustomerCompanyProviderMapping providerMapping) {
        return ProviderMappingDetailRes.builder()
            .providerMappingRes(ProviderMappingRes.of(providerMapping))
            .providerExpn(providerMapping.getProvider().getProviderExpn())
            .auditInfo(AuditInfo.of(providerMapping.getCreateUserId(), providerMapping.getCreateDt(), providerMapping.getModifyUserId(), providerMapping.getModifyDt()))
            .build();
    }

}
