package com.sk.domain.mi.dto.response.dashboard.charts;

import java.util.List;

import com.querydsl.core.annotations.QueryProjection;
import com.sk.domain.shared.entity.master.tcm.ChartPredictSetting;
import com.sk.domain.shared.entity.master.tcm.ChartPredictSettingDetail;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

public record ChartSettingAndDetails(
        @Schema(description = "차트 설정") ChartPredictSetting chartPredictSetting,
        @Schema(description = "차트 설정 상세") List<ChartPredictSettingDetail> chartPredictSettingDetails) {
    @Builder
    @QueryProjection
    public ChartSettingAndDetails(ChartPredictSetting chartPredictSetting, List<ChartPredictSettingDetail> chartPredictSettingDetails) {
        this.chartPredictSetting = chartPredictSetting;
        this.chartPredictSettingDetails = chartPredictSettingDetails;
    }
}
