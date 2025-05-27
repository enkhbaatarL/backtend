package com.sk.domain.mi.dto.response.dashboard.charts;

import io.swagger.v3.oas.annotations.media.Schema;

public class DashboardChartRes {
    // 기준 날짜
    @Schema(description = "메타 데이터")
    DashboardMetadata dashboardMetadata;
   
    // 메인인지 아닌지???

    @Schema(description = "그래프")
    DashboardChart dashboardChart;

    
}
