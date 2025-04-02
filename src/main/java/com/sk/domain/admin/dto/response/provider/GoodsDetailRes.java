package com.sk.domain.admin.dto.response.provider;

import java.time.LocalDateTime;

import com.sk.domain.shared.dto.AuditInfo;

import io.swagger.v3.oas.annotations.media.Schema;

// @formatter:off
public record GoodsDetailRes(

        @Schema(description = "상품번호", example = "5000")
        Integer goodsNo,

        @Schema(description = "상품명", example = "제조사별 DRAM 생산량")
        String goodsNm,

        @Schema(description = "상품ID", example = "DRAM")
        String goodsId,

        @Schema(description = "상품설명", example = "상품 설명")
        String goodsExpn,

        @Schema(description = "사용여부", example = "Y")
        Boolean isUsed,

        @Schema(description = "감사 정보")
        AuditInfo auditInfo) {
}
// @formatter:on