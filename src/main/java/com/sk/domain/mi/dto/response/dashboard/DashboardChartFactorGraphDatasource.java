package com.sk.domain.mi.dto.response.dashboard;

import com.sk.domain.shared.entity.master.tmd.GoodsDataSourceDetail;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

public record DashboardChartFactorGraphDatasource(
        @Schema(description = "차트 데이터 소스 번호") Integer goodsDataSourceNo,

        @Schema(description = "차트 데이터 소스 명") String goodsDataSourceNm,

        @Schema(description = "상품 번호") Integer goodsNo,

        @Schema(description = "상품 데이터 번호") Integer goodsDataNo,

        @Schema(description = "권한 여부", example = "true") Boolean isPermitted
) {
    @Builder
    public DashboardChartFactorGraphDatasource(Integer goodsDataSourceNo, String goodsDataSourceNm, Integer goodsNo, Integer goodsDataNo, Boolean isPermitted) {
        this.goodsDataSourceNo = goodsDataSourceNo;
        this.goodsDataSourceNm = goodsDataSourceNm;
        this.goodsNo = goodsNo;
        this.goodsDataNo = goodsDataNo;
        this.isPermitted = isPermitted;
    }

    public static DashboardChartFactorGraphDatasource of(GoodsDataSourceDetail goodsDataSourceDetail, Boolean isPermitted) {
        return DashboardChartFactorGraphDatasource.builder()
                .goodsDataSourceNo(goodsDataSourceDetail.getGoodsDataSourceNo())
                .goodsDataSourceNm(goodsDataSourceDetail.getGoodsDataSourceNm())
                .goodsDataNo(goodsDataSourceDetail.getGoodsData().getGoodsDataNo())
                .goodsNo(goodsDataSourceDetail.getGoodsData().getGoods().getGoodsNo())
                .isPermitted(isPermitted)
                .build();
    }
}
