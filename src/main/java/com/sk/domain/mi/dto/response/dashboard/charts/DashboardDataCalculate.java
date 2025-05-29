package com.sk.domain.mi.dto.response.dashboard.charts;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import com.sk.domain.mi.dto.response.dashboard.charts.graph.DashboardChart;
import com.sk.domain.shared.entity.master.tmd.GoodsDataSourceDetail;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

public record DashboardDataCalculate(
        @Schema(description = "예측 데이터 마지막 업데이트 중에 가장 날짜가 최근인 값을 계산하기 위해 예측 데이터별 마지막 업데이트 날짜 목록") List<LocalDate> lastUpdatedList,

        @Schema(description = "알림 세팅에서 사용할 데이터 소스 목록(예측 데이터만 해당)") List<DashboardDataSource> dashboardDataSources,

        @Schema(description = "Actual 데이터 소스 화폐 단위") GoodsDataSourceDetail actualGoodsDataSourceDetail,

        @Schema(description = "associatedGoodsDataSourceDetail 기준으로 데이터를 묶어주기 위함") List<Map<Integer, DashboardChart>> associatedCharts) {

    @Builder
    public DashboardDataCalculate(List<LocalDate> lastUpdatedList, List<DashboardDataSource> dashboardDataSources,
            GoodsDataSourceDetail actualGoodsDataSourceDetail, List<Map<Integer, DashboardChart>> associatedCharts) {
        this.lastUpdatedList = lastUpdatedList;
        this.dashboardDataSources = dashboardDataSources;
        this.actualGoodsDataSourceDetail = actualGoodsDataSourceDetail;
        this.associatedCharts = associatedCharts;
    }
}
