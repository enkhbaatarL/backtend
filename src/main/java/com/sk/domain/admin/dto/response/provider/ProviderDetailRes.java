package com.sk.domain.admin.dto.response.provider;

import java.time.LocalDateTime;

import com.sk.domain.shared.dto.AuditInfo;

import io.swagger.v3.oas.annotations.media.Schema;

// @formatter:off
public record ProviderDetailRes(

        @Schema(description = "프로바이더번호", example = "5000")
        Integer providerNo,

        @Schema(description = "프로바이더명", example = "레피니티브")
        String providerNm,

        @Schema(description = "프로바이더코드", example = "REF")
        String providerCd,

        @Schema(description = "프로바이더설명", example = "레피니티브 설명")
        String providerExpn,

        @Schema(description = "사용여부", example = "true")
        Boolean isUsed,
        
        @Schema(description = "감사 정보")
        AuditInfo auditInfo) {
}
// @formatter:on