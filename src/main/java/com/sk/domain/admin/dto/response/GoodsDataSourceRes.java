package com.sk.domain.admin.dto.response;

import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;

public record GoodsDataSourceRes() {

  public record All(
      @Schema(description = "등록자", example = "sk01234") //
      Integer goodsDataSourceNo,

      @Schema(description = "등록자", example = "sk01234") //
      String goodsDataSourceNm,

      @Schema(description = "등록자", example = "sk01234") //
      String targetDbNm,

      @Schema(description = "등록자", example = "sk01234") //
      String targetTableNm,

      @Schema(description = "등록자", example = "sk01234") //
      String queryConditionContent,

      @Schema(description = "등록자", example = "sk01234") //
      String goodsDataSourceExpn,

      @Schema(description = "등록자", example = "sk01234") //
      String createUserId,

      @Schema(description = "등록일시", example = "2025-02-12 15:00:00") //
      LocalDateTime createDt,

      @Schema(description = "수정자", example = "sk01234") //
      String modifyUserId,

      @Schema(description = "수정일시", example = "2025-02-12 15:00:00") //
      LocalDateTime modifyDt) {

  }
}
