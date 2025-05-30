package com.sk.domain.admin.dto.request.provider;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

// @formatter:off
public record GoodsDataCreateReq(
    @NotBlank
    @Size(min = 1, max = 100)
    @Schema(description = "상품데이터명", example = "")
    String goodsDataNm,

    @Schema(description = "상품데이터설명", example = "") 
    String goodsDataExpn,

    @Schema(description = "상품데이터 Summary", example = "") 
    String goodsDataSummaryCntt,

    @Schema(description = "데이터기고자명 Summary", example = "") 
    String dataContributorNm,

    @Schema(description = "문서 타입 카테고리 리스트", example = "")
    List<CategoryDataReq> docCategoryList,

    @Schema(description = "언어 타입 카테고리 리스트", example = "")
    List<CategoryDataReq> langCategoryList,

    @Schema(description = "정형 데이타 리스트", example = "")
    List<StructuredDataReq> structuredDataList,

    @Schema(description = "비정형 데이타 리스트", example = "")
    List<UnstructuredDataReq> unstructuredDataList) {

}
// @formatter:on