package com.sk.domain.mi.dto.response.dashboard.charts.metaData;

import com.sk.domain.shared.entity.master.tcm.ChartPredictSetting;
import com.sk.domain.shared.entity.master.tmd.GoodsDataSourceDetail;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

public record DashboardChartFactorGraphMetadata(
    @Schema(description = "화폐", example = "$") String currency,

    @Schema(description = "현재 값 단위", example = "MMBtu") String currentUnitCd,

    @Schema(description = "테이블 헤더 명", example = "Vehicles") String tableHeader
) {
    @Builder
    public DashboardChartFactorGraphMetadata(String currency, String currentUnitCd, String tableHeader) {
        this.currency = currency;
        this.currentUnitCd = currentUnitCd;
        this.tableHeader = tableHeader;
    }

    public static DashboardChartFactorGraphMetadata of(GoodsDataSourceDetail goodsDataSourceDetail, ChartPredictSetting chartPredictSetting) {
        return DashboardChartFactorGraphMetadata.builder()
                .currency(goodsDataSourceDetail.getGoodsDataSourceCurrencyUnitNm())
                .currentUnitCd(goodsDataSourceDetail.getGoodsDataSourceUnitNm())
                .tableHeader(chartPredictSetting.getPredictTargetRepresentSpecNm())
                .build();
    }

    public static DashboardChartFactorGraphMetadata of(ChartPredictSetting chartPredictSetting) {
        return DashboardChartFactorGraphMetadata.builder()
                .tableHeader(chartPredictSetting.getPredictTargetRepresentSpecNm())
                .build();
    }
}
