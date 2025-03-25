package com.sk.domain.admin.dto.response.company;

import com.querydsl.core.annotations.QueryProjection;
import com.sk.domain.shared.dto.AuditInfo;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.LocalDateTime;

public record ProviderWithGoodsCntRes(

    @Schema(description = "프로바이더 번호", example = "159")
    Integer providerNo,

    @Schema(description = "프로바이더 명", example = "LSEG")
    String providerNm,

    @Schema(description = "회사가 구독중인 상품 개수", example = "12")
    int numberOfGoods,

    @Schema(description = "감사 정보")
    AuditInfo auditInfo

) {

    @QueryProjection
    public ProviderWithGoodsCntRes(Integer providerNo, String providerNm, int numberOfGoods, String createUserId, LocalDateTime createDt, String modifyUserId, LocalDateTime modifyDt,
                                   String createUserNm, String createUserDeptNm, String createUserCpnyNm, String modifyUserNm, String modifyUserDeptNm, String modifyUserCpnyNm) {
        this(providerNo, providerNm, numberOfGoods,
            AuditInfo.ofDetail(createUserId, createDt, modifyUserId, modifyDt, createUserNm, createUserDeptNm, createUserCpnyNm, modifyUserNm, modifyUserDeptNm, modifyUserCpnyNm));
    }

    @Builder
    public ProviderWithGoodsCntRes(Integer providerNo, String providerNm, int numberOfGoods, AuditInfo auditInfo) {
        this.providerNo = providerNo;
        this.providerNm = providerNm;
        this.numberOfGoods = numberOfGoods;
        this.auditInfo = auditInfo;
    }

}
