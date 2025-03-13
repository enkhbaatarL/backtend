package com.sk.domain.admin.dto.response;

import java.time.LocalDateTime;

import com.sk.domain.shared.entity.master.tmd.Provider;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

public record ProviderRes() {

  public record All(

      @Schema(description = "프로바이더 번호", example = "5000") //
      Integer providerNo,

      @Schema(description = "프로바이더 이름", example = "레피니티브") //
      String providerNm,

      @Schema(description = "프로바이더 코드", example = "REF") //
      String providerCd,

      @Schema(description = "프로바이더 설명", example = "레피니티브 설명") //
      String providerExpn,

      @Schema(description = "프로바이더 사용유무", example = "true") //
      boolean isUsed,

      @Schema(description = "상품 개수", example = "10") //
      Long goodsCount,

      @Schema(description = "등록자", example = "sk01234") //
      String createUserId,

      @Schema(description = "등록일시", example = "2025-02-12 15:00:00") //
      LocalDateTime createDt,

      @Schema(description = "수정자", example = "sk01234") //
      String modifyUserId,

      @Schema(description = "수정일시", example = "2025-02-12 15:00:00") //
      LocalDateTime modifyDt) {

  }

  @Builder
  public record Insert(
      @Schema(description = "프로바이더 번호", example = "5000") //
      Integer providerNo,

      @Schema(description = "프로바이더 이름", example = "레피니티브") //
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

    public static Insert of(Provider provider) {
      return Insert.builder() //
          .providerNo(provider.getProviderNo()) //
          .providerNm(provider.getProviderNm()) //
          .providerCd(provider.getProviderCd()) //
          .providerExpn(provider.getProviderExpn()) //
          .isUsed(provider.getIsUsed()) //
          .createUserId(provider.getCreateUserId()) //
          .createDt(provider.getCreateDt()) //
          .modifyUserId(provider.getModifyUserId()) //
          .modifyDt(provider.getModifyDt()) //
          .build();
    }

  }

  public record One(
      @Schema(description = "프로바이더 번호", example = "5000") //
      Integer providerNo,

      @Schema(description = "프로바이더 이름", example = "레피니티브") //
      String providerNm,

      @Schema(description = "프로바이더 코드", example = "REF") //
      String providerCd,

      @Schema(description = "프로바이더 설명", example = "레피니티브 설명") //
      String providerExpn,

      @Schema(description = "프로바이더 사용유무", example = "true") //
      boolean isUsed,

      @Schema(description = "등록자 ID", example = "sk01234") //
      String createUserId,

      @Schema(description = "등록일시", example = "2025-02-12 15:00:00") //
      LocalDateTime createDt,

      @Schema(description = "등록자 이름", example = "홍길동") //
      String createUserName,

      @Schema(description = "수정자  ID", example = "sk01234") //
      String modifyUserId,

      @Schema(description = "수정일시", example = "2025-02-12 15:00:00") //
      LocalDateTime modifyDt,

      @Schema(description = "수정자 이름", example = "홍길동") //
      String modifyUserName

  ) {

  }

  @Builder
  public record Update(
      @Schema(description = "프로바이더 번호", example = "5000") //
      Integer providerNo,

      @Schema(description = "프로바이더 이름", example = "레피니티브") //
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

    public static Update of(Provider provider) {
      return Update.builder() //
          .providerNo(provider.getProviderNo()) //
          .providerNm(provider.getProviderNm()) //
          .providerCd(provider.getProviderCd()) //
          .providerExpn(provider.getProviderExpn()) //
          .isUsed(provider.getIsUsed()) //
          .createUserId(provider.getCreateUserId()) //
          .createDt(provider.getCreateDt()) //
          .modifyUserId(provider.getModifyUserId()) //
          .modifyDt(provider.getModifyDt()) //
          .build();
    }
  }

  @Builder
  public record Delete(
      @Schema(description = "프로바이더 번호", example = "5000") //
      Integer providerNo,

      @Schema(description = "프로바이더 이름", example = "레피니티브") //
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

    public static Delete of(Provider provider) {
      return Delete.builder() //
          .providerNo(provider.getProviderNo()) //
          .providerNm(provider.getProviderNm()) //
          .providerCd(provider.getProviderCd()) //
          .providerExpn(provider.getProviderExpn()) //
          .isUsed(provider.getIsUsed()) //
          .createUserId(provider.getCreateUserId()) //
          .createDt(provider.getCreateDt()) //
          .modifyUserId(provider.getModifyUserId()) //
          .modifyDt(provider.getModifyDt()) //
          .build();
    }
  }
}