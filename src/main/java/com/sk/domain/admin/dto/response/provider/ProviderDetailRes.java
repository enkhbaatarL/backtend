package com.sk.domain.admin.dto.response.provider;

import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;

public record ProviderDetailRes(

        @Schema(description = "프로바이더번호", example = "5000") //
        Integer providerNo,

        @Schema(description = "프로바이더명", example = "레피니티브") //
        String providerNm,

        @Schema(description = "프로바이더코드", example = "REF") //
        String providerCd,

        @Schema(description = "프로바이더설명", example = "레피니티브 설명") //
        String providerExpn,

        @Schema(description = "사용여부", example = "true") //
        Boolean isUsed,

        @Schema(description = "생성사용자로그인ID", example = "sk01234") //
        String createUserId,

        @Schema(description = "생성일시", example = "2025-02-12 15:00:00") //
        LocalDateTime createDt,

        @Schema(description = "생성사이름", example = "홍길동") //
        String createUserName) {
}
