package com.sk.domain.admin.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import org.hibernate.validator.constraints.Length;

import io.swagger.v3.oas.annotations.media.Schema;

public record ProviderReq() {

        public record Insert(
                        @NotBlank(message = "프로바이더 이름은 빈 값일 수 없습니다.") //
                        @Length(min = 1, max = 20) //
                        @Schema(description = "프로바이더 이름", example = "") //
                        String providerNm,

                        @NotBlank(message = "프로바이더 코드는 빈 값일 수 없습니다.") //
                        @Length(min = 1, max = 6) //
                        @Schema(description = "프로바이더 코드", example = "") //
                        String providerCd,

                        @NotBlank(message = "프로바이더 설명은 빈 값일 수 없습니다.") //
                        @Length(min = 1, max = 100) //
                        @Schema(description = "프로바이더 설명", example = "") //
                        String providerExpn,

                        @NotNull(message = "프로바이더 사용유무는 빈 값일 수 없습니다.") //
                        @Schema(description = "프로바이더 사용유무", example = "") //
                        boolean isUsed) {

        }

        public record Update(
                        @Schema(description = "프로바이더 이름", example = "") //
                        String providerNm,

                        @Schema(description = "프로바이더 코드", example = "") //
                        String providerCd,

                        @Schema(description = "프로바이더 설명", example = "") //
                        String providerExpn,

                        @Schema(description = "프로바이더 사용유무", example = "") //
                        boolean isUsed) {

        }
}
