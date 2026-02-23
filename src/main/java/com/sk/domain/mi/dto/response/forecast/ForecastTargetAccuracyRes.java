package com.sk.domain.mi.dto.response.forecast;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import lombok.Builder;

@Schema(description = "타겟별 누적 적중률")
public record ForecastTargetAccuracyRes(
        @Schema(description = "누적 적중률(%)", example = "78.25") Double cumulativeHitRate,
        @Schema(description = "예측 차트 상세 설명") String predictChartDetailExpn,
        @Schema(description = "누적 기준일(예측날짜중 가장 작은 값)", example = "24-01-01")
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yy-MM-dd")
        LocalDate asOfDate
) {
    @Builder
    public ForecastTargetAccuracyRes(Double cumulativeHitRate, String predictChartDetailExpn,
        LocalDate asOfDate) {
        this.cumulativeHitRate = cumulativeHitRate;
        this.predictChartDetailExpn = predictChartDetailExpn;
        this.asOfDate = asOfDate;
    }

    public static ForecastTargetAccuracyRes of(Double cumulativeHitRate,
        String predictChartDetailExpn, LocalDate asOfDate) {
        return ForecastTargetAccuracyRes.builder()
            .cumulativeHitRate(cumulativeHitRate)
            .predictChartDetailExpn(predictChartDetailExpn)
            .asOfDate(asOfDate)
            .build();
    }
}
