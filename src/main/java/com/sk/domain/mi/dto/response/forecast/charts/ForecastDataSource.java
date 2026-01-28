package com.sk.domain.mi.dto.response.forecast.charts;

import com.sk.domain.shared.entity.master.tcm.ChartPredictSettingDetail;
import com.sk.domain.shared.entity.master.tmd.GoodsDataSourceDetail;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

public record ForecastDataSource(
        @Schema(description = "데이터 소스 번호") Integer goodsDataSourceNo,
        @Schema(description = "차트 라인 명") String dashboardLineName) {
    @Builder
    public ForecastDataSource(Integer goodsDataSourceNo, String dashboardLineName) {
        this.goodsDataSourceNo = goodsDataSourceNo;
        this.dashboardLineName = dashboardLineName;
    }

    public static ForecastDataSource of(GoodsDataSourceDetail goodsDataSourceDetail, ChartPredictSettingDetail chartPredictSettingDetail) {
        return ForecastDataSource.builder()
                .goodsDataSourceNo(goodsDataSourceDetail.getGoodsDataSourceNo())
                .dashboardLineName(chartPredictSettingDetail.getPredictChartDetailExpn())
                .build();
    }
}
