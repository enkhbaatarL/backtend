package com.sk.domain.admin.dto.request.provider;

import jakarta.validation.constraints.NotBlank;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

// @formatter:off
public record Recommendation(
    @NotBlank
    @Schema(description = "상품데이터명", example = "")
    String goodsDataNm,

    @Schema(description = "상품데이터 Summary", example = "")
    String goodsDataSummaryCntt,

    @Schema(description = "발행일", example = "2025-12-31")
    String publishedDate,

    @Schema(description = "문서 타입 카테고리 리스트", example = "")
    List<CategoryDataReq> docCategoryList,

    // OMDIA 부가 정보로 저장
    @Schema(description = "추천 자료", example = "")
    String authors
){

}
// @formatter:on
