package com.sk.domain.mi.dto.response.forecast.summary;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Schema(description = "Forecast report 상세 응답")
public record ForecastReportDetailRes(
        @Schema(description = "시장지수분석대상코드") String marketIndexAnalysisTargetCd,
        @Schema(description = "예측수행일자") String predictionFulfillDt,
        @Schema(description = "MNEMONIC코드") String mnemonicCd,
        @Schema(description = "예측차트상세설명") String predictChartDetailExpn,
        @Schema(description = "연관상품데이터소스번호") Integer associatedGoodsDataSourceNo,
        @Schema(description = "예측결과종료일자") String endPredictionResultDt,
        @Schema(description = "예측결과시작일자") String startPredictionResultDt,
        @Schema(description = "예측기준일자") String predictionCriteriaDt,
        @Schema(description = "예측결과값(평균)") Double avgPredictionResultVal,
        @Schema(description = "예측결과값(최소값)") Double minPredictionResultVal,
        @Schema(description = "예측결과값(최대값)") Double maxPredictionResultVal,
        @Schema(description = "예측기준값") Double predictionCriteriaVal,
        @Schema(description = "예측결과값-예측기준값") Double predictionResultDiffVal,
        @Schema(description = "(예측결과값-예측기준값)/예측기준값(%)") Double predictionResultDiffRate,
        @Schema(description = "정기예측여부") Boolean isScheduled,
        @Schema(description = "설명력제목내용") String explanatoryTitleContent,
        @Schema(description = "설명력요약내용") String explanatorySummaryContent,
        @Schema(description = "설명력내용파일경로명") String explanatoryFilePath) {

    @Builder
    public ForecastReportDetailRes(String marketIndexAnalysisTargetCd, String predictionFulfillDt, String mnemonicCd,
            String predictChartDetailExpn, Integer associatedGoodsDataSourceNo, String endPredictionResultDt,
            String startPredictionResultDt, String predictionCriteriaDt, Double avgPredictionResultVal,
            Double minPredictionResultVal, Double maxPredictionResultVal,
            Double predictionCriteriaVal, Double predictionResultDiffVal, Double predictionResultDiffRate,
            Boolean isScheduled, String explanatoryTitleContent, String explanatorySummaryContent, String explanatoryFilePath) {
        this.marketIndexAnalysisTargetCd = marketIndexAnalysisTargetCd;
        this.predictionFulfillDt = predictionFulfillDt;
        this.mnemonicCd = mnemonicCd;
        this.predictChartDetailExpn = predictChartDetailExpn;
        this.associatedGoodsDataSourceNo = associatedGoodsDataSourceNo;
        this.endPredictionResultDt = endPredictionResultDt;
        this.startPredictionResultDt = startPredictionResultDt;
        this.predictionCriteriaDt = predictionCriteriaDt;
        this.avgPredictionResultVal = avgPredictionResultVal;
        this.minPredictionResultVal = minPredictionResultVal;
        this.maxPredictionResultVal = maxPredictionResultVal;
        this.predictionCriteriaVal = predictionCriteriaVal;
        this.predictionResultDiffVal = predictionResultDiffVal;
        this.predictionResultDiffRate = predictionResultDiffRate;
        this.isScheduled = isScheduled;
        this.explanatoryTitleContent = explanatoryTitleContent;
        this.explanatorySummaryContent = explanatorySummaryContent;
        this.explanatoryFilePath = explanatoryFilePath;
    }
}
