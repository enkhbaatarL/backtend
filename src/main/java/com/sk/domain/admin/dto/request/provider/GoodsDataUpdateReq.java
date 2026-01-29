package com.sk.domain.admin.dto.request.provider;

import java.util.Arrays;
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
    List<UnstructuredDataReq> unstructuredDataList,
    // OMDIA 추가
    @Schema(description = "발행일", example = "2025-12-31")
    String publishedDate,  // DB 컬럼추가

    // OMDIA 부가 정보로 저장
    @Schema(description = "추천 자료", example = "")
    List<Recommendation> recommendations,

    @Schema(description = "본래 URL", example = "")
    String url,

    @Schema(description = "프로바이더 데이터 ID, 프로바이더가 관리하는 데이터 ID", example = "")
    String providerContentId,

    @Schema(description = "비정형 데이타 링크 리스트", example = "")
    List<UnstructuredDataLinkReq> unstructuredDataLinkList,

    @Schema(description = "저자 리스트", example = "Jane, John Doe")
    String authors
) {

}
// @formatter:on
