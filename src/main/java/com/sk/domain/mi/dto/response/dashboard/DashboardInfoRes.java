package com.sk.domain.mi.dto.response.dashboard;

import java.time.LocalDate;

import com.sk.domain.shared.entity.master.tcm.ChartPredictSetting;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
public record DashboardInfoRes(
    @Schema(description = "차트 설정 명", example = "Henry Hub Price Forecast")
    String chartPredictSettingNm,
   
    @Schema(description = "최근 업데이트 일시", example = "2025-05-11")
    LocalDate lastUpdateDt
) {
    @Builder
    public DashboardInfoRes(String chartPredictSettingNm, LocalDate lastUpdateDt) {
        this.chartPredictSettingNm = chartPredictSettingNm;
        this.lastUpdateDt = lastUpdateDt;
    }
    
    public static DashboardInfoRes of(ChartPredictSetting chartPredictSetting, LocalDate lastUpdateDt) {
        return DashboardInfoRes.builder()
            .chartPredictSettingNm(chartPredictSetting.getPredictTargetSettingNm())
            .lastUpdateDt(lastUpdateDt)
            .build();
    }
}
