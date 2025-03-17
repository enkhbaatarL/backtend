package com.sk.domain.admin.dto.response.provider;

import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;

public record GoodsDataSourceRes(
    @Schema(description = "상품데이터소스번호", example = "") //
    Integer goodsDataSourceNo,

    @Schema(description = "상품데이터소스명", example = "") //
    String goodsDataSourceNm,

    @Schema(description = "대상데이터베이스명", example = "") //
    String targetDbNm,

    @Schema(description = "대상데이터베이스명", example = "") //
    String targetTableNm,

    @Schema(description = "쿼리조건내용", example = "") //
    String queryConditionContent,

    @Schema(description = "쿼리조건내용", example = "") //
    String goodsDataSourceExpn,

    @Schema(description = "생성사용자로그인ID", example = "sk01234") //
    String createUserId,

    @Schema(description = "생성일시", example = "2025-02-12 15:00:00") //
    LocalDateTime createDt,

    @Schema(description = "수정사용자로그인ID", example = "sk01234") //
    String modifyUserId,

    @Schema(description = "수정일시", example = "2025-02-12 15:00:00") //
    LocalDateTime modifyDt) {

}
