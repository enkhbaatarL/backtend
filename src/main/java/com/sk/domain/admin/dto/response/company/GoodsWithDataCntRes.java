package com.sk.domain.admin.dto.response.company;

import com.querydsl.core.annotations.QueryProjection;
import com.sk.domain.shared.dto.AuditInfo;
import com.sk.domain.shared.enums.GoodsAuthManagementSeparateCd;
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

        @Schema(description = "상품권한 주체구분 코드", example = "INDIVIDUAL", allowableValues = {"COMPANY", "INDIVIDUAL"})
        String goodsAuthManagementSeparateCd,

        @Schema(description = "상품권한 주체구분 명", example = "개인", allowableValues = {"회사", "개인"})
        String goodsAuthManagementSeparateNm,

        AuditInfo auditInfo

) {

    @QueryProjection
    public GoodsWithDataCntRes(Integer goodsNo, String goodsNm, String goodsExpn, Integer numberOfData, boolean isUsed,
                               GoodsAuthManagementSeparateCd goodsAuthManagementSeparateCd, String createUserId, LocalDateTime createDt, String modifyUserId, LocalDateTime modifyDt,
                               String createUserNm, String createUserDeptNm, String createUserCpnyNm, String modifyUserNm, String modifyUserDeptNm, String modifyUserCpnyNm) {
        this(goodsNo, goodsNm, goodsExpn, numberOfData, isUsed, goodsAuthManagementSeparateCd.getCode(), goodsAuthManagementSeparateCd.getDescription(),
                AuditInfo.ofDetail(createUserId, createDt, modifyUserId, modifyDt, createUserNm, createUserDeptNm, createUserCpnyNm, modifyUserNm, modifyUserDeptNm, modifyUserCpnyNm));
    }

    @Builder
    public GoodsWithDataCntRes(Integer goodsNo, String goodsNm, String goodsExpn, Integer numberOfData, boolean isUsed, String goodsAuthManagementSeparateCd, String goodsAuthManagementSeparateNm, AuditInfo auditInfo) {
        this.goodsNo = goodsNo;
        this.goodsNm = goodsNm;
        this.goodsExpn = goodsExpn;
        this.numberOfData = numberOfData;
        this.isUsed = isUsed;
        this.goodsAuthManagementSeparateCd = goodsAuthManagementSeparateCd;
        this.goodsAuthManagementSeparateNm = goodsAuthManagementSeparateNm;
        this.auditInfo = auditInfo;
    }

}
