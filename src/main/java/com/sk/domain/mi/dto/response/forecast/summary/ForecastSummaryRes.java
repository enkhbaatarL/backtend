package com.sk.domain.mi.dto.response.forecast.summary;

import com.sk.domain.mi.dto.response.dashboard.summary.DashboardChartSummaryMap;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

public record ForecastSummaryRes(
        @Schema(description = "조회 가능 날짜(예측 수행일)") String date,
        @Schema(description = "시장지수분석대상코드") String marketIndexAnalysisTargetCd,
        @Schema(description = "예측수행일자") String predictionFulfillDt,
        @Schema(description = "MNEMONIC코드") String mnemonicCd,
        @Schema(description = "예측결과일자") String predictionResultDt,
        @Schema(description = "설명력제목내용") String explanatoryTitleContent,
        @Schema(description = "설명력요약내용") String explanatorySummaryContent,
        @Schema(description = "설명력내용파일경로명") String explanatoryFilePath) {

    @Builder
    public ForecastSummaryRes(String date, String marketIndexAnalysisTargetCd, String predictionFulfillDt, String mnemonicCd,
            String predictionResultDt, String explanatoryTitleContent, String explanatorySummaryContent,
            String explanatoryFilePath) {
        this.date = date;
        this.marketIndexAnalysisTargetCd = marketIndexAnalysisTargetCd;
        this.predictionFulfillDt = predictionFulfillDt;
        this.mnemonicCd = mnemonicCd;
        this.predictionResultDt = predictionResultDt;
        this.explanatoryTitleContent = explanatoryTitleContent;
        this.explanatorySummaryContent = explanatorySummaryContent;
        this.explanatoryFilePath = explanatoryFilePath;
    }

    public static ForecastSummaryRes of(String date, DashboardChartSummaryMap summary) {
        return ForecastSummaryRes.builder()
                .date(date)
                .marketIndexAnalysisTargetCd(summary.getMarketIndexAnalysisTargetCd())
                .predictionFulfillDt(summary.getPredictionFulfillDt())
                .mnemonicCd(summary.getMnemonicCd())
                .predictionResultDt(summary.getPredictionResultDt())
                .explanatoryTitleContent(summary.getExplanatoryTitleContent())
                .explanatorySummaryContent(summary.getExplanatorySummaryContent())
                .explanatoryFilePath(summary.getExplanatoryFilePath())
                .build();
    }
}
