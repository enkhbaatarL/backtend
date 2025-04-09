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
    @Schema(description = "데이터명", example = "")
    String goodsDataNm,

    @Schema(description = "상품데이터설명", example = "") 
    String goodsExpn,

    @Schema(description = "Summary", example = "") 
    String goodsDataSummaryCntt,

    @NotNull
    @Schema(description = "카테고리 데이타 리스트", example = "")
    List<CategoryDataReq> categoryDataList,

    @Schema(description = "정형 데이타 리스트", example = "")
    List<StructuredDataReq> structuredDataList,

    @Schema(description = "비정형 데이타 리스트", example = "")
    List<UnstructuredDataReq> unstructuredDataList) {

}
// @formatter:on