package com.sk.domain.mi.dto.response.marketData;

import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;

// @formatter:off
public record RecentViewedRes(
        @Schema(description = "프로바이더 번호", example = "")
        int providerNo,

        @Schema(description = "프로바이더 이름", example = "")
        String providerNm,

        @Schema(description = "상품 번호", example = "")
        int goodsNo,

        @Schema(description = "상품데이터번호", example = "")
        int goodsDataNo,

        @Schema(description = "상품데이터명", example = "")
        String goodsDataNm,

        @Schema(description = "상품데이터설명", example = "")
        String goodsDataExpn,

        @Schema(description = "최근 view 날짜", example = "")
        LocalDateTime recentViewedDt) {
  public RecentViewedRes(
      int providerNo,
      String providerNm,
      int goodsNo,
      int goodsDataNo,
      String goodsDataNm,
      String goodsDataExpn,
      LocalDateTime recentViewedDt) {
    this.providerNo = providerNo;
    this.providerNm = providerNm;
    this.goodsNo = goodsNo;
    this.goodsDataNo = goodsDataNo;
    this.goodsDataNm = goodsDataNm;
    this.goodsDataExpn = goodsDataExpn;
    this.recentViewedDt = recentViewedDt;
  }

  public static RecentViewedRes of(RecentViewedInf recentViewedInf){
      return new RecentViewedRes(recentViewedInf.getPrvdNo(), recentViewedInf.getPrvdNm(), recentViewedInf.getGdsNo(),
          recentViewedInf.getGdsDataNo(), recentViewedInf.getGdsDataNm(), recentViewedInf.getGdsDataExpn(), recentViewedInf.getMaxInqr());
  }
}
// @formatter:on
