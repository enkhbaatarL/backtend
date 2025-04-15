package com.sk.domain.mi.dto.response;

import com.sk.domain.shared.entity.master.tmd.GoodsData;
import com.sk.domain.shared.entity.master.tmd.GoodsDataSourceDetail;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.util.List;

public record GoodsDataSourceRes(

    @Schema(description = "상품 데이터소스 번호", example = "159")
    Integer goodsDataSourceNo,

    @Schema(description = "상품 데이터소스 명", example = "structured name test - 001")
    String goodsDataSourceNm

) {

    @Builder
    public GoodsDataSourceRes(Integer goodsDataSourceNo, String goodsDataSourceNm) {
        this.goodsDataSourceNo = goodsDataSourceNo;
        this.goodsDataSourceNm = goodsDataSourceNm;
    }

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
