package com.sk.domain.admin.dto.response.provider;

import com.sk.domain.shared.entity.master.tmd.Goods;

import io.swagger.v3.oas.annotations.media.Schema;

// @formatter:off
public record GoodsCreateRes(
        @Schema(description = "상품번호", example = "5000")
        Integer goodsNo) {

            public static GoodsCreateRes of(Goods goods) {
                return new GoodsCreateRes(goods.getGoodsNo());
            }
}
// @formatter:on