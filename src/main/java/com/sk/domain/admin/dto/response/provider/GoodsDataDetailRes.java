package com.sk.domain.admin.dto.response.provider;

import java.util.List;

import com.sk.domain.shared.entity.master.tmd.GoodsData;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

// @formatter:off
public record GoodsDataDetailRes(
        @Schema(description = "상품데이터번호", example = "") 
        Integer goodsDataNo,

        @Schema(description = "상품데이터명", example = "") 
        String goodsDataNm,

        @Schema(description = "상품데이터설명", example = "") 
        String goodsDataExpn,

        @Schema(description = "상품데이터요약내용", example = "") 
        String goodsDataSummaryCntt,

        @Schema(description = "데이터기고자명 Summary", example = "") 
        String dataContributorNm,

        @Schema(description = "문서 타입 카테고리 리스트", example = "") 
        List<CategoryDataRes> docCategoryList,

        @Schema(description = "언어 타입 카테고리 리스트", example = "")
        List<CategoryDataRes> langCategoryList,

        @Schema(description = "정형 데이타 리스트", example = "") 
        List<StructuredDataRes> structuredDataList,

        @Schema(description = "비정형 데이타 리스트", example = "") 
        List<UnstructuredDataRes> unstructuredDataList) {

    @Builder
    public GoodsDataDetailRes(Integer goodsDataNo, String goodsDataNm, String goodsDataExpn, String goodsDataSummaryCntt, String dataContributorNm, List<CategoryDataRes> docCategoryList, List<CategoryDataRes> langCategoryList, List<StructuredDataRes> structuredDataList, List<UnstructuredDataRes> unstructuredDataList) {
        this.goodsDataNo = goodsDataNo;
        this.goodsDataNm = goodsDataNm;
        this.goodsDataExpn = goodsDataExpn;
        this.goodsDataSummaryCntt = goodsDataSummaryCntt;
        this.dataContributorNm = dataContributorNm;
        this.docCategoryList = docCategoryList;
        this.langCategoryList = langCategoryList;
        this.structuredDataList = structuredDataList;
        this.unstructuredDataList = unstructuredDataList;
    }

        public static GoodsDataDetailRes of(GoodsData goodsData, List<CategoryDataRes> docCategoryList, List<CategoryDataRes> langCategoryList, List<StructuredDataRes> structuredDataList, List<UnstructuredDataRes> unstructuredDataList){
            return GoodsDataDetailRes.builder()
            .goodsDataNo(goodsData.getGoodsDataNo())
            .goodsDataNm(goodsData.getGoodsDataNm())
            .goodsDataExpn(goodsData.getGoodsDataExpn())
            .goodsDataSummaryCntt(goodsData.getGoodsDataSummaryCntt())
            .dataContributorNm(goodsData.getDataContributorNm())
            .docCategoryList(docCategoryList)
            .langCategoryList(langCategoryList)
            .structuredDataList(structuredDataList)
            .unstructuredDataList(unstructuredDataList)
            .build();
        }
}
// @formatter:on