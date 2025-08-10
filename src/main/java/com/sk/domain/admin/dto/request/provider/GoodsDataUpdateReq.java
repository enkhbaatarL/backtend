package com.sk.domain.admin.dto.request.provider;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

// @formatter:off
public record GoodsDataUpdateReq(
    @Schema(description = "상품데이터설명", example = "") 
    String goodsDataExpn,
    
    @Schema(description = "Summary", example = "") 
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