package com.sk.domain.admin.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import io.swagger.v3.oas.annotations.media.Schema;

public record ProviderReq() {

  public record Create(
      @NotBlank //
      @Size(min = 1, max = 20) //
      @Schema(description = "프로바이더 이름", example = "레피니티브") //
      String providerNm,

      @NotBlank //
      @Size(min = 1, max = 20) //
      @Schema(description = "프로바이더 코드", example = "REF") //
      String providerCd,

      @NotBlank //
      @Size(min = 1, max = 100) //
      @Schema(description = "프로바이더 설명", example = "레피니티브 설명") //
      String providerExpn,

      @NotNull //
      @Schema(description = "프로바이더 사용유무", example = "true") //
      boolean isUsed) {

  }

  public record Update(
      @Size(min = 1, max = 20) //
      @Schema(description = "프로바이더 이름", example = "레피니티브") //
      String providerNm,

      @Size(min = 1, max = 100) //
      @Schema(description = "프로바이더 설명", example = "레피니티브 설명") //
      String providerExpn,

      @Schema(description = "프로바이더 사용유무", example = "true") //
      boolean isUsed) {

  }
}
