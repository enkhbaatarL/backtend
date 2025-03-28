package com.sk.domain.admin.dto.response.provider;

import java.util.List;

import com.sk.domain.admin.dto.request.provider.StructuredDataReq;
import com.sk.domain.admin.dto.request.provider.UnstructuredDataReq;
import com.sk.domain.admin.service.UnStructuredDataRes;
import com.sk.domain.shared.entity.master.tmd.GoodsData;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

// @formatter:off
@Builder
public record GoodsDataDetailRes(
        @Schema(description = "상품데이터번호", example = "") 
        Integer goodsDataNo,

        @Schema(description = "상품데이터명", example = "") 
        String goodsDataNm,

        @Schema(description = "상품데이터설명", example = "") 
        String goodsDataExpn,

        @Schema(description = "상품데이터요약내용", example = "") 
        String goodsDataSummaryCntt,

        @Schema(description = "데이타 Category Code Id", example = "") 
        List<String> categoryCodeIdList,

        @Schema(description = "정형 데이타 리스트", example = "") 
        List<StructuredDataReq> structuredDataList,

        @Schema(description = "비정형 데이타 리스트", example = "") 
        List<UnstructuredDataReq> StructuredDataList) {

            public static GoodsDataDetailRes of(GoodsData goodsData, List<CategoryDataRes> goodsDataCategoryList, List<StructuredDataRes> StructuredDataList, List<UnStructuredDataRes> unStructuredDataList){
                return GoodsDataDetailRes.builder()
                .build();
            }
}
// @formatter:on