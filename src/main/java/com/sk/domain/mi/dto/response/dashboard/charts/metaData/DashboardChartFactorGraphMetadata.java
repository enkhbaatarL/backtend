package com.sk.domain.mi.dto.response.dashboard.charts.metaData;

import com.sk.domain.shared.entity.master.tmd.GoodsDataSourceDetail;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

public record DashboardChartFactorGraphMetadata(
    @Schema(description = "화폐", example = "$") String currency,

    @Schema(description = "현재 값 단위", example = "MMBtu") String currentUnitCd
) {
    @Builder
    public DashboardChartFactorGraphMetadata(String currency, String currentUnitCd) {
        this.currency = currency;
        this.currentUnitCd = currentUnitCd;
    }

    public static DashboardChartFactorGraphMetadata of(GoodsDataSourceDetail goodsDataSourceDetail) {
        return DashboardChartFactorGraphMetadata.builder()
                .currency(goodsDataSourceDetail.getGoodsDataSourceCurrencyUnitNm())
                .currentUnitCd(goodsDataSourceDetail.getGoodsDataSourceUnitNm())
                .build();
    }
}
