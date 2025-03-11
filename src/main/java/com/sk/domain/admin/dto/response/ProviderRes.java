package com.sk.domain.admin.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

public record ProviderRes(
    @Schema(description = "프로바이더명", example = "레피니티브") //
    String providerName,

    @Schema(description = "프로바이더 설명", example = "레피니티브 설명") //
    String providerDesc,

    @Schema(description = "프로바이더 코드", example = "REF") //
    String providerCode,

    @Schema(description = "등록자", example = "sk01234") // 
    String createUserId,
    
    @Schema(description = "등록일시", example = "2025-02-12 15:00:00") //
    LocalDateTime createDt,

    @Schema(description = "수정자", example = "sk01234") // 
    String modifyUserId,

    @Schema(description = "수정일시", example = "2025-02-12 15:00:00") //
    LocalDateTime modifyDt) {

  @Builder
  public ProviderRes(String providerName, String providerDesc, String providerCode, String createUserId, LocalDateTime createDt, String modifyUserId, LocalDateTime modifyDt) {
    this.providerName = providerName;
    this.providerDesc = providerDesc;
    this.providerCode = providerCode;
    this.createUserId = createUserId;
        this.createDt = createDt;
        this.modifyUserId = modifyUserId;
        this.modifyDt = modifyDt;
  }

  // public static ProviderRes of(CompanyInfo savedEntity) {
  // return ProviderRes.builder()
  // .companyNm(savedEntity.getCompanyNm())
  // .companyEngNm(savedEntity.getCompanyEngNm())
  // .createUserId(savedEntity.getCreateUserId())
  // .createDt(savedEntity.getCreateDt())
  // .modifyUserId(savedEntity.getModifyUserId())
  // .modifyDt(savedEntity.getModifyDt())
  // .build();
  // }
}
