package com.sk.domain.mi.dto.response.dashboard.charts;

import com.sk.domain.shared.entity.master.tcm.ChartPredictSettingDetail;
import com.sk.domain.shared.entity.master.tmd.GoodsDataSourceDetail;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

public record DashboardDataSource(
        @Schema(description = "데이터 소스 번호") Integer goodsDataSourceNo,

        @Schema(description = "대시보드 차트 라인 명") String dashboardLineName) {
    @Builder
    public DashboardDataSource(Integer goodsDataSourceNo, String dashboardLineName) {
        this.goodsDataSourceNo = goodsDataSourceNo;
        this.dashboardLineName = dashboardLineName;
    }

    public static DashboardDataSource of(GoodsDataSourceDetail goodsDataSourceDetail, ChartPredictSettingDetail chartPredictSettingDetail) {
        return DashboardDataSource.builder()
                .goodsDataSourceNo(goodsDataSourceDetail.getGoodsDataSourceNo())
                .dashboardLineName(chartPredictSettingDetail.getPredictChartDetailExpn())
                .build();
    }
}