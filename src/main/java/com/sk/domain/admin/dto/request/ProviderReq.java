package com.sk.domain.admin.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;

import io.swagger.v3.oas.annotations.media.Schema;

public record ProviderReq() {

  public record Create(
      @NotBlank //
      @Length(min = 1, max = 100) //
      @Schema(description = "프로바이더 이름", example = "레피니티브") //
      String providerNm,

      @NotBlank //
      @Schema(description = "프로바이더 코드", example = "REF") //
      String providerCd,

      @NotBlank //
      @Schema(description = "프로바이더 설명", example = "레피니티브 설명") //
      String providerExpn,

      @NotNull //
      @Schema(description = "프로바이더 사용유무", example = "true") //
      boolean isUsed) {

  }

  public record Update(
      @Schema(description = "프로바이더 이름", example = "레피니티브") //
      String providerNm,

      @Schema(description = "프로바이더 코드", example = "REF") //
      String providerCd,

      @Schema(description = "프로바이더 설명", example = "레피니티브 설명") //
      String providerExpn,

      @Schema(description = "프로바이더 사용유무", example = "true") //
      boolean isUsed) {

  }
}
