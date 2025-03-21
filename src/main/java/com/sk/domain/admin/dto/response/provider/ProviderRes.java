package com.sk.domain.admin.dto.response.provider;

import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;

public record ProviderRes(
        @Schema(description = "프로바이더번호", example = "5000")
        Integer providerNo,

        @Schema(description = "프로바이더명", example = "레피니티브")
        String providerNm,

        @Schema(description = "프로바이더코드", example = "REF")
        String providerCd,

        @Schema(description = "사용여부", example = "true")
        Boolean isUsed,

        @Schema(description = "상품개수", example = "10")
        Long goodsCount,

        @Schema(description = "생성일시", example = "2025-02-12 15:00:00")
        LocalDateTime createDt) {

}