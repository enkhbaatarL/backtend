package com.sk.domain.admin.dto.response;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.querydsl.core.annotations.QueryProjection;
import com.sk.domain.shared.dto.AuditInfo;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.LocalDateTime;

public record ProviderMappingGoodsRes(

    @Schema(description = "프로바이더번호", example = "")
    Integer providerNo,

    @Schema(description = "프로바이더명", example = "")
    String providerNm,

    @Schema(description = "회사에 등록된 상품 개수", example = "")
    int numberOfCompanyGoods,

    @JsonUnwrapped
    @Schema(description = "감사 정보")
    AuditInfo auditInfo

) {

    @QueryProjection
    public ProviderMappingGoodsRes(Integer providerNo, String providerNm, int numberOfCompanyGoods, String createUserId, LocalDateTime createDt, String modifyUserId, LocalDateTime modifyDt) {
        this(providerNo, providerNm, numberOfCompanyGoods,
            AuditInfo.of(createUserId, createDt, modifyUserId, modifyDt));
    }

    @Builder
    public ProviderMappingGoodsRes(Integer providerNo, String providerNm, int numberOfCompanyGoods, AuditInfo auditInfo) {
        this.providerNo = providerNo;
        this.providerNm = providerNm;
        this.numberOfCompanyGoods = numberOfCompanyGoods;
        this.auditInfo = auditInfo;
    }

}
