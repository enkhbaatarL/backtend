package com.sk.domain.shared.entity.compositeKey.tmd;

import org.hibernate.annotations.Comment;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EqualsAndHashCode
public class GoodsDataSourceDetailUnitConvertMappingId {

  @Column(name = "gds_data_src_no", nullable = false, columnDefinition = "int unsigned")
  @Comment("상품데이타소스번호")
  private Integer goodsDataSourceNo;

  @Column(name = "unit_conv_no", nullable = false, columnDefinition = "int unsigned")
  @Comment("단위변환번호")
  private Integer unitConvNo;

  @Builder
  public GoodsDataSourceDetailUnitConvertMappingId(Integer goodsDataSourceNo, Integer unitConvNo) {
    this.goodsDataSourceNo = goodsDataSourceNo;
    this.unitConvNo = unitConvNo;
  }

  public static GoodsDataSourceDetailUnitConvertMappingId of(Integer goodsDataSourceNo, Integer unitConvNo) {
    return GoodsDataSourceDetailUnitConvertMappingId.builder()
        .goodsDataSourceNo(goodsDataSourceNo)
        .unitConvNo(unitConvNo)
        .build();
  }

}
