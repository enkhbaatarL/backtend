package com.sk.domain.mi.dto.response.insights;

import java.util.List;

// @formatter:off
public record GoodsDataFilterRes(
    List<List<GoodsDataCategoryRes>> filter) {

        public GoodsDataFilterRes(List<List<GoodsDataCategoryRes>> filter) {
            this.filter = filter;
        }

        public static GoodsDataFilterRes of(List<List<GoodsDataCategoryRes>> filter) {
            return new GoodsDataFilterRes(filter);
        }
}
// @formatter:on