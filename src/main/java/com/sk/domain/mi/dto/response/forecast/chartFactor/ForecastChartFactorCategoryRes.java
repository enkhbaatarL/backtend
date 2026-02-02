package com.sk.domain.mi.dto.response.forecast.chartFactor;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Builder;

public record ForecastChartFactorCategoryRes(
    @Schema(description = "카테고리 목록") List<String> explanatoryLevelNms,
    @Schema(description = "시장지수분석대상코드") String marketIndexAnalysisTargetCd,
    @Schema(description = "예측수행일자") String predictionFulfillDt,
    @Schema(description = "MNEMONIC코드") String mnemonicCd,
    @Schema(description = "예측차트상세설명") String predictChartDetailExpn,
    @Schema(description = "연관상품데이터소스번호") Integer associatedGoodsDataSourceNo,
    @Schema(description = "예측결과종료일자") String endPredictionResultDt,
    @Schema(description = "예측결과시작일자") String startPredictionResultDt,
    @Schema(description = "예측기준일자") String predictionCriteriaDt,
    @Schema(description = "예측결과값(평균)") Double avgPredictionResultVal,
    @Schema(description = "예측기준값") Double predictionCriteriaVal,
    @Schema(description = "예측기준값-예측결과값") Double predictionResultDiffVal,
    @Schema(description = "예측결과변화율(%)") Double predictionResultDiffRate,
    @Schema(description = "정기예측여부") Boolean isScheduled
) {
    @Builder
    public ForecastChartFactorCategoryRes(List<String> explanatoryLevelNms,
        String marketIndexAnalysisTargetCd,
        String predictionFulfillDt,
        String mnemonicCd,
        String predictChartDetailExpn,
        Integer associatedGoodsDataSourceNo,
        String endPredictionResultDt,
        String startPredictionResultDt,
        String predictionCriteriaDt,
        Double avgPredictionResultVal,
        Double predictionCriteriaVal,
        Double predictionResultDiffVal,
        Double predictionResultDiffRate,
        Boolean isScheduled) {
        this.explanatoryLevelNms = explanatoryLevelNms;
        this.marketIndexAnalysisTargetCd = marketIndexAnalysisTargetCd;
        this.predictionFulfillDt = predictionFulfillDt;
        this.mnemonicCd = mnemonicCd;
        this.predictChartDetailExpn = predictChartDetailExpn;
        this.associatedGoodsDataSourceNo = associatedGoodsDataSourceNo;
        this.endPredictionResultDt = endPredictionResultDt;
        this.startPredictionResultDt = startPredictionResultDt;
        this.predictionCriteriaDt = predictionCriteriaDt;
        this.avgPredictionResultVal = avgPredictionResultVal;
        this.predictionCriteriaVal = predictionCriteriaVal;
        this.predictionResultDiffVal = predictionResultDiffVal;
        this.predictionResultDiffRate = predictionResultDiffRate;
        this.isScheduled = isScheduled;
    }

    public static ForecastChartFactorCategoryRes of(List<String> explanatoryLevelNms) {
        return ForecastChartFactorCategoryRes.builder()
            .explanatoryLevelNms(explanatoryLevelNms)
            .build();
    }
}
