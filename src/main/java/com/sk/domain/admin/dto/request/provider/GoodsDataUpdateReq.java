package com.sk.domain.admin.dto.request.provider;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

// @formatter:off
public record GoodsDataUpdateReq(
    @Schema(description = "상품데이터설명", example = "") 
    String goodsExpn,
    
    @Schema(description = "Summary", example = "") 
    String goodsDataSummaryCntt,

    @Schema(description = "카테고리 데이타 리스트", example = "")
    List<CategoryDataReq> categoryDataList,

    @Schema(description = "정형 데이타 리스트", example = "")
    List<StructuredDataReq> structuredDataList,

    @Schema(description = "비정형 데이타 리스트", example = "")
    List<UnstructuredDataReq> unstructuredDataList) {
}
// @formatter:on