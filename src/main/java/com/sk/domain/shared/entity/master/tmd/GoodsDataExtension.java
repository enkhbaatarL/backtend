package com.sk.domain.shared.entity.master.tmd;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sk.domain.admin.dto.request.provider.CategoryDataReq;
import com.sk.domain.admin.dto.request.provider.Recommendation;
import com.sk.domain.admin.dto.request.provider.UnstructuredDataLinkReq;
import com.sk.domain.admin.dto.response.provider.GoodsDataExtensionPayloadRes;
import com.sk.domain.shared.entity.base.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Getter
@Entity
@Comment("TMD_상품데이터기본_추가")
@Table(name = "tmd_gdsdatam_ext")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GoodsDataExtension extends BaseEntity {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  @Id
  @Column(name = "gds_data_no", nullable = false, columnDefinition = "int unsigned")
  @Comment("상품데이터번호")
  private Integer goodsDataNo;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "payload", columnDefinition = "json", nullable = false)
  private JsonNode payload;

  @Builder
  public GoodsDataExtension(
      LocalDateTime createDt,
      LocalDateTime modifyDt,
      String createUserId,
      String modifyUserId,
      Boolean isDeleted,
      Integer goodsDataNo,
      JsonNode payload) {
    this.goodsDataNo = goodsDataNo;
    this.payload = payload;
  }

  public static GoodsDataExtension of(
      Integer goodsDataNo,
      String authors,
      List<Recommendation> recommendations,
      String url,
      String providerContentId,
      List<UnstructuredDataLinkReq> unstructuredDataLinkList) {

    ObjectNode objectNode = OBJECT_MAPPER.createObjectNode();
    if (providerContentId != null) objectNode.put("providerContentId", providerContentId);
    if (authors != null) objectNode.put("authors", authors);
    if (url != null) objectNode.put("url", url);
    ArrayNode recommendationsNode = objectNode.putArray("recommendations");
    if (recommendations != null && !recommendations.isEmpty()) {
      for (Recommendation recommendation : recommendations) {
        ObjectNode recommendationNode = OBJECT_MAPPER.createObjectNode();
        recommendationNode.put("goodsDataNm", recommendation.goodsDataNm());
        recommendationNode.put("authors", recommendation.authors());
        recommendationNode.put("goodsDataSummaryCntt", recommendation.goodsDataSummaryCntt());
        recommendationNode.put("publishedDate", recommendation.publishedDate());
        ArrayNode docCategoryArray = recommendationNode.putArray("docCategoryList");
        if (recommendation.docCategoryList() != null
            && !recommendation.docCategoryList().isEmpty()) {
          for (CategoryDataReq categoryDataReq : recommendation.docCategoryList()) {
            docCategoryArray.add(categoryDataReq.codeId());
          }
        }
        recommendationsNode.add(recommendationNode);
      }
    }
    ArrayNode unstructuredDataLinkNodes = objectNode.putArray("unstructuredDataLinkList");
    if (unstructuredDataLinkList != null && !unstructuredDataLinkList.isEmpty()) {
      for (UnstructuredDataLinkReq unstructuredDataLinkReq : unstructuredDataLinkList) {
        ObjectNode unstructuredDataLinkNode = OBJECT_MAPPER.createObjectNode();
        if (unstructuredDataLinkReq.name() != null)
          unstructuredDataLinkNode.put("name", unstructuredDataLinkReq.name());
        unstructuredDataLinkNode.put("url", unstructuredDataLinkReq.url());
        unstructuredDataLinkNodes.add(unstructuredDataLinkNode);
      }
    }
    return GoodsDataExtension.builder().goodsDataNo(goodsDataNo).payload(objectNode).build();
  }

  public void updatePayload(
      String authors,
      List<Recommendation> recommendations,
      String url,
      String providerContentId,
      List<UnstructuredDataLinkReq> unstructuredDataLinkList) {

    ObjectNode objectNode;
    if (this.payload instanceof ObjectNode) {
      objectNode = (ObjectNode) this.payload;
    } else {
      objectNode = OBJECT_MAPPER.createObjectNode();
      this.payload = objectNode;
    }

    if (providerContentId != null) objectNode.put("providerContentId", providerContentId);
    if (authors != null) objectNode.put("authors", authors);
    if (url != null) objectNode.put("url", url);

    if (recommendations != null) {
      ArrayNode recommendationsNode = objectNode.putArray("recommendations");
      for (Recommendation recommendation : recommendations) {
        ObjectNode recommendationNode = OBJECT_MAPPER.createObjectNode();
        recommendationNode.put("goodsDataNm", recommendation.goodsDataNm());
        recommendationNode.put("authors", recommendation.authors());
        recommendationNode.put("goodsDataSummaryCntt", recommendation.goodsDataSummaryCntt());
        recommendationNode.put("publishedDate", recommendation.publishedDate());
        ArrayNode docCategoryArray = recommendationNode.putArray("docCategoryList");
        if (recommendation.docCategoryList() != null
            && !recommendation.docCategoryList().isEmpty()) {
          for (CategoryDataReq categoryDataReq : recommendation.docCategoryList()) {
            docCategoryArray.add(categoryDataReq.codeId());
          }
        }
        recommendationsNode.add(recommendationNode);
      }
    }

    if (unstructuredDataLinkList != null) {
      ArrayNode unstructuredDataLinkNodes = objectNode.putArray("unstructuredDataLinkList");
      for (UnstructuredDataLinkReq unstructuredDataLinkReq : unstructuredDataLinkList) {
        ObjectNode unstructuredDataLinkNode = OBJECT_MAPPER.createObjectNode();
        if (unstructuredDataLinkReq.name() != null)
          unstructuredDataLinkNode.put("name", unstructuredDataLinkReq.name());
        unstructuredDataLinkNode.put("url", unstructuredDataLinkReq.url());
        unstructuredDataLinkNodes.add(unstructuredDataLinkNode);
      }
    }
  }

  public GoodsDataExtensionPayloadRes generatePayloadRes() {
    JsonNode payload = this.payload;
    String providerContentId =
        payload.has("providerContentId") ? payload.get("providerContentId").asText() : null;
    String url = payload.has("url") ? payload.get("url").asText() : null;
    String authors = payload.has("authors") ? payload.get("authors").asText() : null;

    List<Recommendation> recommendations = new java.util.ArrayList<>();
    if (payload.has("recommendations") && payload.get("recommendations").isArray()) {
      for (JsonNode recNode : payload.get("recommendations")) {
        List<CategoryDataReq> docCategoryList = new java.util.ArrayList<>();
        if (recNode.has("docCategoryList") && recNode.get("docCategoryList").isArray()) {
          for (JsonNode catNode : recNode.get("docCategoryList")) {
            docCategoryList.add(new CategoryDataReq(catNode.asText()));
          }
        }
        recommendations.add(
            new Recommendation(
                recNode.has("goodsDataNm") ? recNode.get("goodsDataNm").asText() : null,
                recNode.has("goodsDataSummaryCntt")
                    ? recNode.get("goodsDataSummaryCntt").asText()
                    : null,
                recNode.has("publishedDate") ? recNode.get("publishedDate").asText() : null,
                docCategoryList,
                recNode.has("authors") ? recNode.get("authors").asText() : null));
      }
    }

    List<UnstructuredDataLinkReq> unstructuredDataLinkList = new java.util.ArrayList<>();
    if (payload.has("unstructuredDataLinkList")
        && payload.get("unstructuredDataLinkList").isArray()) {
      for (JsonNode linkNode : payload.get("unstructuredDataLinkList")) {
        unstructuredDataLinkList.add(
            new UnstructuredDataLinkReq(
                linkNode.has("name") ? linkNode.get("name").asText() : null,
                linkNode.has("url") ? linkNode.get("url").asText() : null));
      }
    }

    return GoodsDataExtensionPayloadRes.builder()
        .goodsDataNo(this.goodsDataNo)
        .authors(authors)
        .providerContentId(providerContentId)
        .url(url)
        .recommendations(recommendations)
        .unstructuredDataLinkList(unstructuredDataLinkList)
        .build();
  }

  public List<String> getAuthors() {
    String authors = this.getStringOfAuthors();
    if (authors == null || authors.trim().isEmpty()) {
      return List.of("");
    }
    return Arrays.stream(authors.trim().split(",")).map(String::trim).toList();
  }

  public String getStringOfAuthors() {
    JsonNode payload = this.payload;
    return payload.has("authors") ? payload.get("authors").asText() : null;
  }
}
