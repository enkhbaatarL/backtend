package com.sk.domain.admin.dto.response;

import java.time.LocalDateTime;

import com.sk.domain.shared.entity.master.tmd.Provider;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

public record ProviderRes(
    @Schema(description = "프로바이더번호", example = "5000") //
    Integer providerNo,

    @Schema(description = "프로바이더명", example = "레피니티브") //
    String providerNm,

    @Schema(description = "프로바이더코드", example = "REF") //
    String providerCd,

    @Schema(description = "사용여부", example = "true") //
    boolean isUsed,

    @Schema(description = "상품개수", example = "10") //
    Long goodsCount,

    @Schema(description = "생성일시", example = "2025-02-12 15:00:00") //
    LocalDateTime createDt) {

  public record Name(
      @Schema(description = "프로바이더번호", example = "5000") //
      Integer providerNo,

      @Schema(description = "프로바이더명", example = "레피니티브") //
      String providerNm) {
  }

  @Builder
  public record Create(
      @Schema(description = "프로바이더번호", example = "5000") //
      Integer providerNo,

      @Schema(description = "프로바이더명", example = "레피니티브") //
      String providerNm,

      @Schema(description = "프로바이더코드", example = "REF") //
      String providerCd,

      @Schema(description = "프로바이더설명", example = "레피니티브 설명") //
      String providerExpn,

      @Schema(description = "사용여부", example = "true") //
      boolean isUsed,

      @Schema(description = "생성사용자로그인ID", example = "sk01234") //
      String createUserId,

      @Schema(description = "생성일시", example = "2025-02-12 15:00:00") //
      LocalDateTime createDt,

      @Schema(description = "수정사용자로그인ID", example = "sk01234") //
      String modifyUserId,

      @Schema(description = "수정일시", example = "2025-02-12 15:00:00") //
      LocalDateTime modifyDt) {

    public static Create of(Provider provider) {
      return Create.builder() //
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

  public record Get(
      @Schema(description = "프로바이더번호", example = "5000") //
      Integer providerNo,

      @Schema(description = "프로바이더명", example = "레피니티브") //
      String providerNm,

      @Schema(description = "프로바이더코드", example = "REF") //
      String providerCd,

      @Schema(description = "프로바이더설명", example = "레피니티브 설명") //
      String providerExpn,

      @Schema(description = "사용여부", example = "true") //
      boolean isUsed,

      @Schema(description = "생성사용자로그인ID", example = "sk01234") //
      String createUserId,

      @Schema(description = "생성일시", example = "2025-02-12 15:00:00") //
      LocalDateTime createDt,

      @Schema(description = "생성사이름", example = "홍길동") //
      String createUserName) {

  }

  @Builder
  public record Update(
      @Schema(description = "프로바이더번호", example = "5000") //
      Integer providerNo,

      @Schema(description = "프로바이더명", example = "레피니티브") //
      String providerNm,

      @Schema(description = "프로바이더코드", example = "REF") //
      String providerCd,

      @Schema(description = "프로바이더설명", example = "레피니티브 설명") //
      String providerExpn,

      @Schema(description = "사용여부", example = "true") //
      boolean isUsed,

      @Schema(description = "생성사용자로그인ID", example = "sk01234") //
      String createUserId,

      @Schema(description = "생성일시", example = "2025-02-12 15:00:00") //
      LocalDateTime createDt,

      @Schema(description = "수정사용자로그인ID", example = "sk01234") //
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
      @Schema(description = "프로바이더번호", example = "5000") //
      Integer providerNo,

      @Schema(description = "프로바이더명", example = "레피니티브") //
      String providerNm,

      @Schema(description = "프로바이더코드", example = "REF") //
      String providerCd,

      @Schema(description = "프로바이더설명", example = "레피니티브 설명") //
      String providerExpn,

      @Schema(description = "사용여부", example = "true") //
      boolean isUsed,

      @Schema(description = "생성사용자로그인ID", example = "sk01234") //
      String createUserId,

      @Schema(description = "생성일시", example = "2025-02-12 15:00:00") //
      LocalDateTime createDt,

      @Schema(description = "수정사용자로그인ID", example = "sk01234") //
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

  public record LatestUpdated(
      @Schema(description = "수정일시", example = "2025-02-12 15:00:00") //
      LocalDateTime modifyDt,

      @Schema(description = "수정자이름", example = "홍길동") //
      String modifyUserName) {

  }
}