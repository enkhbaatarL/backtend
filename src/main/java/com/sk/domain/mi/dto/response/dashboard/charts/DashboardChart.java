package com.sk.domain.mi.dto.response.dashboard.charts;

import com.sk.domain.shared.enums.chart.ChartPredictLineType;

import io.swagger.v3.oas.annotations.media.Schema;

public record DashboardChart(
        // 타입
        @Schema(description = "차트 타입") ChartPredictLineType type

        // Associate???
) {
}
