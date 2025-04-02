package com.sk.domain.admin.dto.request.provider;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import io.swagger.v3.oas.annotations.media.Schema;

// @formatter:off
public record ProviderCreateReq(
        @NotBlank
        @Size(min = 1, max = 20)
        @Schema(description = "프로바이더 이름", example = "레피니티브")
        String providerNm,

        @NotBlank
        @Size(min = 1, max = 20)
        @Pattern(regexp = "^[A-Z]+$", message = "영어 대문자만 입력 가능합니다. 소문자와 띄어쓰기는 허용되지 않습니다.")
        @Schema(description = "프로바이더 ID", example = "REF")
        String providerId,

        @NotBlank
        @Size(min = 1, max = 100)
        @Schema(description = "프로바이더 설명", example = "레피니티브 설명")
        String providerExpn,

        @NotNull
        @Schema(description = "프로바이더 사용유무", example = "true")
        Boolean isUsed) {

}
// @formatter:on