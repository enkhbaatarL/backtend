package com.sk.domain.shared.entity.master.tmd;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sk.common.converter.BooleanToYNAttributeConverter;
import com.sk.domain.admin.dto.request.provider.CategoryDataReq;
import com.sk.domain.admin.dto.request.provider.Recommendation;
import com.sk.domain.shared.entity.base.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.annotations.ColumnDefault;
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

    public static GoodsDataExtension of(Integer goodsDataNo, List<Recommendation> recommendations, String url) {

      ObjectNode objectNode = OBJECT_MAPPER.createObjectNode();
      objectNode.put("url", url);
      ArrayNode recommendationsNode = objectNode.putArray("recommendations");
      if(recommendations != null && !recommendations.isEmpty()){
          for(Recommendation recommendation : recommendations){
              ObjectNode recommendationNode = OBJECT_MAPPER.createObjectNode();
              recommendationNode.put("goodsDataNm", recommendation.goodsDataNm());
              recommendationNode.put("authors", recommendation.authors());
              recommendationNode.put("goodsDataSummaryCntt", recommendation.goodsDataSummaryCntt());
              recommendationNode.put("publishedDate", recommendation.publishedDate());
              ArrayNode docCategoryArray = recommendationNode.putArray("docCategoryList");
              if(recommendation.docCategoryList() != null && !recommendation.docCategoryList().isEmpty()){
                  for(CategoryDataReq categoryDataReq: recommendation.docCategoryList()){
                      docCategoryArray.add(categoryDataReq.codeId());
                  }
              }
              recommendationsNode.add(recommendationNode);
          }
      }
      return GoodsDataExtension.builder().goodsDataNo(goodsDataNo).payload(objectNode).build();
    }
}
