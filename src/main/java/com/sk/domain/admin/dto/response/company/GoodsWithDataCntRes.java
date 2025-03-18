package com.sk.domain.admin.dto.response.company;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.querydsl.core.annotations.QueryProjection;
import com.sk.domain.shared.dto.AuditInfo;
import com.sk.domain.shared.enums.GoodsAuthManageSupportCd;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.LocalDateTime;

public record GoodsWithDataCntRes(

    @Schema(description = "상품번호", example = "159")
    Integer goodsNo,

    @Schema(description = "상품명", example = "제조사별 DRAM 생산량")
    String goodsNm,

    @Schema(description = "상품설명", example = "제품상세설명")
    String goodsExpn,

    @Schema(description = "상품에 포함된 데이터 개수", example = "1")
    Integer numberOfData,

    @Schema(description = "사용여부", example = "true")
    boolean isUsed,

    @Schema(description = "상품권한", example = "INDIVIDUAL", allowableValues = {"COMPANY", "INDIVIDUAL"})
    String goodsAuthManageCd,

    @JsonUnwrapped
    AuditInfo auditInfo

) {

    @QueryProjection
    public GoodsWithDataCntRes(Integer goodsNo, String goodsNm, String goodsExpn, Integer numberOfData, boolean isUsed, GoodsAuthManageSupportCd goodsAuthManageSupportCd, String createUserId, LocalDateTime createDt, String modifyUserId, LocalDateTime modifyDt) {
        this(goodsNo, goodsNm, goodsExpn, numberOfData, isUsed, goodsAuthManageSupportCd.getCode(),
            new AuditInfo(createUserId, createDt, modifyUserId, modifyDt));
    }

    @Builder
    public GoodsWithDataCntRes(Integer goodsNo, String goodsNm, String goodsExpn, Integer numberOfData, boolean isUsed, String goodsAuthManageCd, AuditInfo auditInfo) {
        this.goodsNo = goodsNo;
        this.goodsNm = goodsNm;
        this.goodsExpn = goodsExpn;
        this.numberOfData = numberOfData;
        this.isUsed = isUsed;
        this.goodsAuthManageCd = goodsAuthManageCd;
        this.auditInfo = auditInfo;
    }

}
