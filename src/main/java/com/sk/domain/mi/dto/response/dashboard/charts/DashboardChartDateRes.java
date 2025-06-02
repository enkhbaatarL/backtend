package com.sk.domain.mi.dto.response.dashboard.charts;

import java.time.LocalDate;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

public record DashboardChartDateRes(
        @Schema(description = "예측 데이터 조회 가능 날짜") List<LocalDate> dates) {

    @Builder
    public DashboardChartDateRes(List<LocalDate> dates) {
        this.dates = dates;
    }

    public static DashboardChartDateRes of(List<LocalDate> dates) {
        return DashboardChartDateRes.builder()
                .dates(dates)
                .build();
    }
}
