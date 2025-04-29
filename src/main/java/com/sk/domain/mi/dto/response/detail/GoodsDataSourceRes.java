package com.sk.domain.mi.dto.response.detail;

import com.sk.domain.shared.entity.master.tmd.GoodsDataSourceDetail;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.util.List;

@Builder
public record GoodsDataSourceRes(

    @Schema(description = "상품 데이터 소스 번호", example = "10004")
    Integer goodsDataSourceNo,

    @Schema(description = "상품 데이터 소스 명", example = "structured data 1234")
    String goodsDataSourceNm

) {

    public static GoodsDataSourceRes of(GoodsDataSourceDetail entity) {
        return GoodsDataSourceRes.builder()
            .goodsDataSourceNo(entity.getGoodsDataSourceNo())
            .goodsDataSourceNm(entity.getGoodsDataSourceNm())
            .build();
    }

    public static List<GoodsDataSourceRes> ofList(List<GoodsDataSourceDetail> entities) {
        return entities.stream()
            .map(GoodsDataSourceRes::of)
            .toList();
    }

}
