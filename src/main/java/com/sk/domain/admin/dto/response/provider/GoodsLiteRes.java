package com.sk.domain.admin.dto.response.provider;

import com.sk.domain.shared.entity.master.tmd.Goods;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.util.List;

public record GoodsLiteRes(

    @Schema(description = "상품번호", example = "5000")
    Integer goodsNo,

    @Schema(description = "상품ID", example = "")
    String goodsId,

    @Schema(description = "상품명", example = "제조사별 DRAM 생산량")
    String goodsNm

) {

    @Builder
    public GoodsLiteRes(Integer goodsNo, String goodsId, String goodsNm) {
        this.goodsNo = goodsNo;
        this.goodsId = goodsId;
        this.goodsNm = goodsNm;
    }

    public static GoodsLiteRes of(Goods entity) {
        return GoodsLiteRes.builder()
            .goodsNo(entity.getGoodsNo())
            .goodsId(entity.getGoodsId())
            .goodsNm(entity.getGoodsNm())
            .build();
    }

    public static List<GoodsLiteRes> ofList(List<Goods> entities) {
        return entities.stream()
            .map(GoodsLiteRes::of)
            .toList();
    }

}
