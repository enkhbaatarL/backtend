package com.sk.domain.mi.dto.response.forecast.summary;

import com.sk.domain.mi.dto.response.dashboard.summary.DashboardChartSummaryMap;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Schema(description = "Forecast report 응답")
public record ForecastReportRes(
        @Schema(description = "시장지수분석대상코드") String marketIndexAnalysisTargetCd,
        @Schema(description = "예측수행일자") String predictionFulfillDt,
        @Schema(description = "MNEMONIC코드") String mnemonicCd,
        @Schema(description = "예측차트상세설명") String predictChartDetailExpn,
        @Schema(description = "연관상품데이터소스번호") Integer associatedGoodsDataSourceNo,
        @Schema(description = "예측결과종료일자") String endPredictionResultDt,
        @Schema(description = "예측결과시작일자") String startPredictionResultDt,
        @Schema(description = "예측기준일자") String predictionCriteriaDt,
        @Schema(description = "예측결과값") Double predictionResultVal,
        @Schema(description = "예측기준값") Double predictionCriteriaVal,
        @Schema(description = "예측기준값-예측결과값") Double predictionResultDiffVal,
        @Schema(description = "예측결과변화율(%)") Double predictionResultDiffRate,
        @Schema(description = "정기예측여부") Boolean isScheduled) {

    @Builder
    public ForecastReportRes(String marketIndexAnalysisTargetCd, String predictionFulfillDt, String mnemonicCd,
            String predictChartDetailExpn, Integer associatedGoodsDataSourceNo, String endPredictionResultDt,
            String startPredictionResultDt, String predictionCriteriaDt, Double predictionResultVal,
            Double predictionCriteriaVal, Double predictionResultDiffVal, Double predictionResultDiffRate,
            Boolean isScheduled) {
        this.marketIndexAnalysisTargetCd = marketIndexAnalysisTargetCd;
        this.predictionFulfillDt = predictionFulfillDt;
        this.mnemonicCd = mnemonicCd;
        this.predictChartDetailExpn = predictChartDetailExpn;
        this.associatedGoodsDataSourceNo = associatedGoodsDataSourceNo;
        this.endPredictionResultDt = endPredictionResultDt;
        this.startPredictionResultDt = startPredictionResultDt;
        this.predictionCriteriaDt = predictionCriteriaDt;
        this.predictionResultVal = predictionResultVal;
        this.predictionCriteriaVal = predictionCriteriaVal;
        this.predictionResultDiffVal = predictionResultDiffVal;
        this.predictionResultDiffRate = predictionResultDiffRate;
        this.isScheduled = isScheduled;
    }

    public static ForecastReportRes of(DashboardChartSummaryMap summary) {
        return ForecastReportRes.builder()
                .marketIndexAnalysisTargetCd(summary.getMarketIndexAnalysisTargetCd())
                .predictionFulfillDt(summary.getPredictionFulfillDt())
                .mnemonicCd(summary.getMnemonicCd())
                .predictChartDetailExpn(null)
                .associatedGoodsDataSourceNo(null)
                .endPredictionResultDt(summary.getPredictionResultDt())
                .build();
    }
}
