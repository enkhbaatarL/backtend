package com.sk.domain.admin.dto.request.provider;

import io.swagger.v3.oas.annotations.media.Schema;

public record GoodsReq() {

  public record Update(
      @Schema(description = "상품 이름", example = "제조사별 DRAM 생산량") //
      String goodsNm,

      @Schema(description = "상품 코드", example = "DRAM") //
      String goodsCd,

      @Schema(description = "상품 설명", example = "상품 설명") //
      String goodsExpn,

      @Schema(description = "상품 사용유무", example = "true") //
      boolean isUsed) {

  }
}
