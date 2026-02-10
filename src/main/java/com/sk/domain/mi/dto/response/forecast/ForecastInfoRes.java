package com.sk.domain.mi.dto.response.forecast;

import com.sk.domain.mi.dto.response.forecast.summary.ForecastRecordHeaderRes;
import com.sk.domain.shared.entity.master.tcm.ChartPredictSetting;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.util.List;
import lombok.Builder;

public record ForecastInfoRes(
    @Schema(description = "차트 설정 명", example = "Henry Hub Price Forecast")
    String chartPredictSettingNm,

    @Schema(description = "리포트 생성 주기 단위 코드", example = "MM")
    String reportPeriodUnitCd,

    @Schema(description = "실제 데이터 라인 목록")
    List<ForecastTargetRes> targetList,

    @Schema(description = "최근 업데이트 일시", example = "2025-05-11")
    LocalDate lastUpdateDt,

    @Schema(description = "Record탭 헤더 목록")
    List<ForecastRecordHeaderRes> recordHeaders,

    @Schema(description = "모델 정확도 카드 표시 여부", example = "true")
    Boolean modelAccuracyVisible,

    @Schema(description = "타겟별 누적 적중률")
    List<ForecastTargetAccuracyRes> targetAccuracyList
) {
    @Builder
    public ForecastInfoRes(String chartPredictSettingNm, String reportPeriodUnitCd, List<ForecastTargetRes> targetList,
            LocalDate lastUpdateDt, List<ForecastRecordHeaderRes> recordHeaders,
            Boolean modelAccuracyVisible, List<ForecastTargetAccuracyRes> targetAccuracyList) {
        this.chartPredictSettingNm = chartPredictSettingNm;
        this.reportPeriodUnitCd = reportPeriodUnitCd;
        this.targetList = targetList;
        this.lastUpdateDt = lastUpdateDt;
        this.recordHeaders = recordHeaders;
        this.modelAccuracyVisible = modelAccuracyVisible;
        this.targetAccuracyList = targetAccuracyList;
    }

    public static ForecastInfoRes of(ChartPredictSetting chartPredictSetting, List<ForecastTargetRes> targetList,
            LocalDate lastUpdateDt, List<ForecastRecordHeaderRes> recordHeaders,
            Boolean modelAccuracyVisible, List<ForecastTargetAccuracyRes> targetAccuracyList) {
        return ForecastInfoRes.builder()
            .chartPredictSettingNm(chartPredictSetting.getPredictTargetSettingNm())
            .reportPeriodUnitCd(chartPredictSetting.getScopePeriodUnitSeparateCd() != null
                    ? chartPredictSetting.getScopePeriodUnitSeparateCd().name()
                    : null)
            .targetList(targetList)
            .lastUpdateDt(lastUpdateDt)
            .recordHeaders(recordHeaders)
            .modelAccuracyVisible(modelAccuracyVisible)
            .targetAccuracyList(targetAccuracyList)
            .build();
    }
}
