package com.sk.domain.mi.dto.response.forecast;

import com.sk.domain.shared.entity.master.tcm.ChartPredictSettingDetail;
import com.sk.domain.shared.enums.chart.ChartPredictLineType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

public record ForecastTargetRes(
        @Schema(description = "차트 예측 설정 상세 번호") Integer chartPredictSettingDetailNo,
        @Schema(description = "차트 예측 설정 번호") Integer chartPredictSettingNo,
        @Schema(description = "상품 데이터 소스 번호") Integer goodsDataSourceNo,
        @Schema(description = "예측 차트 상세 설명") String predictChartDetailExpn,
        @Schema(description = "차트 선 예측 값 여부") Boolean isChartLinePredictVal,
        @Schema(description = "차트 예측 선 구분 코드") ChartPredictLineType chartPredictLineSeparateCd,
        @Schema(description = "연관 상품 데이터 소스 번호") Integer associatedGoodsDataSourceNo,
        @Schema(description = "선물 여부") Boolean isFuture
) {
    @Builder
    public ForecastTargetRes(Integer chartPredictSettingDetailNo, Integer chartPredictSettingNo, Integer goodsDataSourceNo,
            String predictChartDetailExpn, Boolean isChartLinePredictVal, ChartPredictLineType chartPredictLineSeparateCd,
            Integer associatedGoodsDataSourceNo, Boolean isFuture) {
        this.chartPredictSettingDetailNo = chartPredictSettingDetailNo;
        this.chartPredictSettingNo = chartPredictSettingNo;
        this.goodsDataSourceNo = goodsDataSourceNo;
        this.predictChartDetailExpn = predictChartDetailExpn;
        this.isChartLinePredictVal = isChartLinePredictVal;
        this.chartPredictLineSeparateCd = chartPredictLineSeparateCd;
        this.associatedGoodsDataSourceNo = associatedGoodsDataSourceNo;
        this.isFuture = isFuture;
    }

    public static ForecastTargetRes of(ChartPredictSettingDetail detail) {
        return ForecastTargetRes.builder()
                .chartPredictSettingDetailNo(detail.getChartPredictSettingDetailNo())
                .chartPredictSettingNo(detail.getChartPredictSetting() != null
                        ? detail.getChartPredictSetting().getChartPredictSettingNo()
                        : null)
                .goodsDataSourceNo(detail.getGoodsDataSourceDetail() != null
                        ? detail.getGoodsDataSourceDetail().getGoodsDataSourceNo()
                        : null)
                .predictChartDetailExpn(detail.getPredictChartDetailExpn())
                .isChartLinePredictVal(detail.getIsChartLinePredictVal())
                .chartPredictLineSeparateCd(detail.getChartPredictLineSeparateCd())
                .associatedGoodsDataSourceNo(detail.getAssociatedGoodsDataSourceDetail() != null
                        ? detail.getAssociatedGoodsDataSourceDetail().getGoodsDataSourceNo()
                        : null)
                .isFuture(detail.getIsFuture())
                .build();
    }
}
