package com.sk.domain.admin.dto.response;

import java.time.LocalDateTime;

import com.sk.domain.shared.entity.master.tmd.Goods;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

public record GoodsRes(
    @Schema(description = "상품번호", example = "5000") //
    Integer goodsNo,

    @Schema(description = "상품명", example = "제조사별 DRAM 생산량") //
    String goodsNm,

    @Schema(description = "상품설명", example = "상품 설명") //
    String goodsExpn,

    @Schema(description = "데이타 개수", example = "10") //
    Long goodsDataSourceCount) {

  public record Get(
      @Schema(description = "상품번호", example = "5000") //
      Integer goodsNo,

      @Schema(description = "상품명", example = "제조사별 DRAM 생산량") //
      String goodsNm,

      @Schema(description = "상품코드", example = "DRAM") //
      String goodsCd,

      @Schema(description = "상품설명", example = "상품 설명") //
      String goodsExpn,

      @Schema(description = "사용여부", example = "Y") //
      Boolean isUsed,

      @Schema(description = "생성사용자로그인ID", example = "sk01234") //
      String createUserId,

      @Schema(description = "생성일시", example = "2025-02-12 15:00:00") //
      LocalDateTime createDt,

      @Schema(description = "생성사이름", example = "홍길동") //
      String createUserName,

      @Schema(description = "수정사용자로그인ID", example = "sk01234") //
      String modifyUserId,

      @Schema(description = "수정일시", example = "2025-02-12 15:00:00") //
      LocalDateTime modifyDt,

      @Schema(description = "수정자이름", example = "홍길동") //
      String modifyUserName) {
  }

  @Builder
  public record Update(
      @Schema(description = "상품번호", example = "5000") //
      Integer goodsNo,

      @Schema(description = "상품명", example = "제조사별 DRAM 생산량") //
      String goodsNm,

      @Schema(description = "상품코드", example = "DRAM") //
      String goodsCd,

      @Schema(description = "상품설명", example = "상품 설명") //
      String goodsExpn,

      @Schema(description = "사용여부", example = "Y") //
      Boolean isUsed,

      @Schema(description = "생성사용자로그인ID", example = "sk01234") //
      String createUserId,

      @Schema(description = "생성일시", example = "2025-02-12 15:00:00") //
      LocalDateTime createDt,

      @Schema(description = "수정사용자로그인ID", example = "sk01234") //
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
      @Schema(description = "상품번호", example = "5000") //
      Integer goodsNo,

      @Schema(description = "상품명", example = "제조사별 DRAM 생산량") //
      String goodsNm,

      @Schema(description = "상품코드", example = "DRAM") //
      String goodsCd,

      @Schema(description = "상품설명", example = "상품 설명") //
      String goodsExpn,

      @Schema(description = "사용여부", example = "Y") //
      Boolean isUsed,

      @Schema(description = "생성사용자로그인ID", example = "sk01234") //
      String createUserId,

      @Schema(description = "생성일시", example = "2025-02-12 15:00:00") //
      LocalDateTime createDt,

      @Schema(description = "수정사용자로그인ID", example = "sk01234") //
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
