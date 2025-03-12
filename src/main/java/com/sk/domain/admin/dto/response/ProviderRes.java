package com.sk.domain.admin.dto.response;

import java.time.LocalDateTime;

import com.sk.domain.shared.entity.master.tmd.Provider;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;


public record ProviderRes(
    @Schema(description = "프로바이더명", example = "레피니티브") //
    String providerNm,

    @Schema(description = "프로바이더 코드", example = "REF") //
    String providerCd,

    @Schema(description = "프로바이더 설명", example = "레피니티브 설명") //
    String providerExpn,

    @Schema(description = "프로바이더 사용유무", example = "true") //
    boolean isUsed,

    @Schema(description = "등록자", example = "sk01234") //
    String createUserId,

    @Schema(description = "등록일시", example = "2025-02-12 15:00:00") //
    LocalDateTime createDt,

    @Schema(description = "수정자", example = "sk01234") //
    String modifyUserId,

    @Schema(description = "수정일시", example = "2025-02-12 15:00:00") //
    LocalDateTime modifyDt) {

      @Builder
        public ProviderRes(String providerNm, String providerCd, String providerExpn, boolean isUsed, String createUserId, LocalDateTime createDt, String modifyUserId, LocalDateTime modifyDt) {
    this.providerNm = providerNm;
    this.providerCd = providerCd;
    this.providerExpn = providerExpn;
    this.isUsed = isUsed;
    this.createUserId = createUserId;
    this.createDt = createDt;
    this.modifyUserId = modifyUserId;
    this.modifyDt = modifyDt;
  }

  public static ProviderRes of(Provider savedEntity) {
    return ProviderRes.builder()
        .createUserId(savedEntity.getCreateUserId())
        .createDt(savedEntity.getCreateDt())
        .modifyUserId(savedEntity.getModifyUserId())
        .modifyDt(savedEntity.getModifyDt())
        .providerNm(savedEntity.getProviderNm())
        .providerCd(savedEntity.getProviderCd())
        .providerExpn(savedEntity.getProviderExpn())
        .isUsed(savedEntity.getIsUsed())
        .build();
  }
}
