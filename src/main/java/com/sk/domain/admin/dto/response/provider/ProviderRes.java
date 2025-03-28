package com.sk.domain.admin.dto.response.provider;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import io.swagger.v3.oas.annotations.media.Schema;

// @formatter:off
public record ProviderRes(
        @Schema(description = "데이터 프로바이더번호", example = "5000") 
        Integer providerNo,

        @Schema(description = "데이터 프로바이더명", example = "레피니티브") 
        String providerNm,

        @Schema(description = "고유 코드", example = "REF") 
        String providerCd,

        @Schema(description = "사용상태", example = "true") 
        Boolean isUsed,

        @Schema(description = "상품수", example = "10") 
        Long goodsCount,

        @Schema(description = "등록일", example = "2025-02-12") 
        String createDt) {

    public ProviderRes(Integer providerNo, String providerNm, String providerCd, Boolean isUsed, Long goodsCount, LocalDateTime createDateTime) {
        this(
                providerNo,
                providerNm,
                providerCd,
                isUsed,
                goodsCount,
                createDateTime != null
                        ? createDateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                        : null);
    }
}
// @formatter:on