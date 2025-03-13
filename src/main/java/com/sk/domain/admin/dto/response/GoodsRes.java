package com.sk.domain.admin.dto.response;

import java.time.LocalDateTime;

import com.sk.domain.shared.entity.master.tmd.Goods;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

public record GoodsRes() {

  @Builder
  public record All(

      @Schema(description = "상품 번호", example = "5000") //
      Integer goodsNo,

      @Schema(description = "상품 이름", example = "제조사별 DRAM 생산량") //
      String goodsNm,

      @Schema(description = "상품 코드", example = "DRAM") //
      String goodsCd,

      @Schema(description = "상품 설명", example = "상품 설명") //
      String goodsExpn,

      @Schema(description = "상품 사용유무", example = "Y") //
      Boolean isUsed,

      @Schema(description = "등록자", example = "sk01234") //
      String createUserId,

      @Schema(description = "등록일시", example = "2025-02-12 15:00:00") //
      LocalDateTime createDt,

      @Schema(description = "수정자", example = "sk01234") //
      String modifyUserId,

      @Schema(description = "수정일시", example = "2025-02-12 15:00:00") //
      LocalDateTime modifyDt) {

    public static All of(Goods goods) {
      return All.builder() //
          .goodsNo(goods.getGoodsNo()) //
          .goodsNm(goods.getGoodsNm()) //
          .goodsCd(goods.getGoodsCd()) //
          .goodsExpn(goods.getGoodsExpn()) //
          .isUsed(goods.getIsUsed()) //
          .createUserId(goods.getCreateUserId()) //
          .createDt(goods.getCreateDt()) //
          .modifyUserId(goods.getModifyUserId()) //
          .modifyDt(goods.getModifyDt()) //
          .build();
    }

  }

  public record One(
      @Schema(description = "상품 번호", example = "5000") //
      Integer goodsNo,

      @Schema(description = "상품 이름", example = "제조사별 DRAM 생산량") //
      String goodsNm,

      @Schema(description = "상품 코드", example = "DRAM") //
      String goodsCd,

      @Schema(description = "상품 설명", example = "상품 설명") //
      String goodsExpn,

      @Schema(description = "상품 사용유무", example = "Y") //
      Boolean isUsed,

      @Schema(description = "등록자", example = "sk01234") //
      String createUserId,

      @Schema(description = "등록일시", example = "2025-02-12 15:00:00") //
      LocalDateTime createDt,

      @Schema(description = "등록자 이름", example = "홍길동") //
      String createUserName,

      @Schema(description = "수정자", example = "sk01234") //
      String modifyUserId,

      @Schema(description = "수정일시", example = "2025-02-12 15:00:00") //
      LocalDateTime modifyDt,

      @Schema(description = "수정자 이름", example = "홍길동") //
      String modifyUserName) {
  }

  @Builder
  public record Update(
      @Schema(description = "상품 번호", example = "5000") //
      Integer goodsNo,

      @Schema(description = "상품 이름", example = "제조사별 DRAM 생산량") //
      String goodsNm,

      @Schema(description = "상품 코드", example = "DRAM") //
      String goodsCd,

      @Schema(description = "상품 설명", example = "상품 설명") //
      String goodsExpn,

      @Schema(description = "상품 사용유무", example = "Y") //
      Boolean isUsed,

      @Schema(description = "등록자", example = "sk01234") //
      String createUserId,

      @Schema(description = "등록일시", example = "2025-02-12 15:00:00") //
      LocalDateTime createDt,

      @Schema(description = "수정자", example = "sk01234") //
      String modifyUserId,

      @Schema(description = "수정일시", example = "2025-02-12 15:00:00") //
      LocalDateTime modifyDt) {

    public static Update of(Goods goods) {
      return Update.builder() //
          .goodsNo(goods.getGoodsNo()) //
          .goodsNm(goods.getGoodsNm()) //
          .goodsCd(goods.getGoodsCd()) //
          .goodsExpn(goods.getGoodsExpn()) //
          .isUsed(goods.getIsUsed()) //
          .createUserId(goods.getCreateUserId()) //
          .createDt(goods.getCreateDt()) //
          .modifyUserId(goods.getModifyUserId()) //
          .modifyDt(goods.getModifyDt()) //
          .build();
    }
  }

  @Builder
  public record Delete(
      @Schema(description = "상품 번호", example = "5000") //
      Integer goodsNo,

      @Schema(description = "상품 이름", example = "제조사별 DRAM 생산량") //
      String goodsNm,

      @Schema(description = "상품 코드", example = "DRAM") //
      String goodsCd,

      @Schema(description = "상품 설명", example = "상품 설명") //
      String goodsExpn,

      @Schema(description = "상품 사용유무", example = "Y") //
      Boolean isUsed,

      @Schema(description = "등록자", example = "sk01234") //
      String createUserId,

      @Schema(description = "등록일시", example = "2025-02-12 15:00:00") //
      LocalDateTime createDt,

      @Schema(description = "수정자", example = "sk01234") //
      String modifyUserId,

      @Schema(description = "수정일시", example = "2025-02-12 15:00:00") //
      LocalDateTime modifyDt) {

    public static Delete of(Goods goods) {
      return Delete.builder() //
          .goodsNo(goods.getGoodsNo()) //
          .goodsNm(goods.getGoodsNm()) //
          .goodsCd(goods.getGoodsCd()) //
          .goodsExpn(goods.getGoodsExpn()) //
          .isUsed(goods.getIsUsed()) //
          .createUserId(goods.getCreateUserId()) //
          .createDt(goods.getCreateDt()) //
          .modifyUserId(goods.getModifyUserId()) //
          .modifyDt(goods.getModifyDt()) //
          .build();
    }
  }
}
