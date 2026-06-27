package com.sk.domain.admin.dto.response.provider;

import com.sk.common.utils.DateUtil;
import com.sk.domain.admin.dto.request.provider.Recommendation;
import com.sk.domain.admin.dto.request.provider.UnstructuredDataLinkReq;
import com.sk.domain.shared.entity.master.tmd.GoodsDataExtension;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import com.sk.domain.shared.entity.master.tmd.GoodsData;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

// @formatter:off
public record GoodsDataDetailRes(
    @Schema(description = "상품데이터번호", example = "") Integer goodsDataNo,
    @Schema(description = "상품데이터명", example = "") String goodsDataNm,
    @Schema(description = "상품데이터설명", example = "") String goodsDataExpn,
    @Schema(description = "상품데이터요약내용", example = "") String goodsDataSummaryCntt,
    @Schema(description = "데이터기고자명 Summary", example = "") String dataContributorNm,
    @Schema(description = "문서 타입 카테고리 리스트", example = "") List<CategoryDataRes> docCategoryList,
    @Schema(description = "언어 타입 카테고리 리스트", example = "") List<CategoryDataRes> langCategoryList,
    @Schema(description = "정형 데이타 리스트", example = "") List<StructuredDataRes> structuredDataList,
    @Schema(description = "비정형 데이타 리스트", example = "")
        List<UnstructuredDataRes> unstructuredDataList,

    // OMDIA 추가
    @Schema(description = "발행일", example = "2025-12-31") String publishedDate, // DB 컬럼추가
    @Schema(description = "발행일시", example = "2025-12-31 15:30:45") String publishedDateTime,
    @Schema(description = "저자 리스트", example = "Jane, John Doe") String authors, // DB 컬럼 추가

    // OMDIA 부가 정보로 저장
    @Schema(description = "추천 자료", example = "") List<Recommendation> recommendations,
    @Schema(description = "본래 URL", example = "") String url,
    @Schema(description = "프로바이더 데이터 ID, 프로바이더가 관리하는 데이터 ID", example = "")
        String providerContentId,
    @Schema(description = "비정형 데이타 링크 리스트", example = "")
        List<UnstructuredDataLinkReq> unstructuredDataLinkList) {

  @Builder
  public GoodsDataDetailRes(
      Integer goodsDataNo,
      String goodsDataNm,
      String goodsDataExpn,
      String goodsDataSummaryCntt,
      String dataContributorNm,
      List<CategoryDataRes> docCategoryList,
      List<CategoryDataRes> langCategoryList,
      List<StructuredDataRes> structuredDataList,
      List<UnstructuredDataRes> unstructuredDataList,
      String publishedDate,
      String publishedDateTime,
      String authors,
      List<Recommendation> recommendations,
      String url,
      String providerContentId,
      List<UnstructuredDataLinkReq> unstructuredDataLinkList) {
    this.goodsDataNo = goodsDataNo;
    this.goodsDataNm = goodsDataNm;
    this.goodsDataExpn = goodsDataExpn;
    this.goodsDataSummaryCntt = goodsDataSummaryCntt;
    this.dataContributorNm = dataContributorNm;
    this.docCategoryList = docCategoryList;
    this.langCategoryList = langCategoryList;
    this.structuredDataList = structuredDataList;
    this.unstructuredDataList = unstructuredDataList;
    this.publishedDate = publishedDate;
    this.publishedDateTime = publishedDateTime;
    this.authors = authors;
    this.recommendations = recommendations;
    this.url = url;
    this.providerContentId = providerContentId;
    this.unstructuredDataLinkList = unstructuredDataLinkList;
  }

  public static GoodsDataDetailRes of(
      GoodsData goodsData,
      List<CategoryDataRes> docCategoryList,
      List<CategoryDataRes> langCategoryList,
      List<StructuredDataRes> structuredDataList,
      List<UnstructuredDataRes> unstructuredDataList,
      LocalDate publishedDate,
      LocalDateTime publishedDateTmp,
      GoodsDataExtension goodsDataExtension) {

      List<Recommendation> recommendations = null;
      String url = null;
      String providerContentId = null;
      List<UnstructuredDataLinkReq> unstructuredDataLinkList = null;
      String authors = null;
      if(goodsDataExtension != null){
          GoodsDataExtensionPayloadRes goodsDataExtensionPayloadRes = goodsDataExtension.generatePayloadRes();
          recommendations = goodsDataExtensionPayloadRes.recommendations();
          url = goodsDataExtensionPayloadRes.url();
          providerContentId = goodsDataExtensionPayloadRes.providerContentId();
          unstructuredDataLinkList = goodsDataExtensionPayloadRes.unstructuredDataLinkList();
          authors = goodsDataExtensionPayloadRes.authors();
      }

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
        .publishedDate(publishedDate == null ? null : DateUtil.toYmdHyphen(publishedDate))
        .publishedDateTime(publishedDateTmp != null ? DateUtil.toYyyyMmDdHms(publishedDateTmp) : null)
        .authors(authors)
        .recommendations(recommendations)
        .url(url)
        .providerContentId(providerContentId)
        .unstructuredDataLinkList(unstructuredDataLinkList)
        .build();
  }
}
// @formatter:on
