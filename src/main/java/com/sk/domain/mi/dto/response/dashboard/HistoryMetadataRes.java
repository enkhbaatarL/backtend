package com.sk.domain.mi.dto.response.dashboard;

import com.sk.domain.mi.dto.context.ForecastContext;
import com.sk.domain.shared.entity.master.tcm.ChartPredictSetting;
import com.sk.domain.shared.entity.master.tcm.ChartPredictSettingDetail;
import com.sk.domain.shared.enums.PeriodUnitSeparateCd;
import com.sk.domain.shared.enums.PriceType;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.util.ArrayList;
import java.util.List;

@Builder
public record HistoryMetadataRes(

    @Schema(description = "예측대상 설정 명", example = "Henry Hub Price Forecast")
    String predictTargetSettingNm,

    @Schema(description = "예측기간 단위구분 코드", example = "D", allowableValues = {"D", "M"})
    PeriodUnitSeparateCd predictPeriodUnitSeparateCd,

    @Schema(description = "화폐단위", example = "MMbtu")
    String currency,

    @Schema(description = "단위", example = "일")
    String unit,

    List<Target> targets

) {

    @Builder
    public record Target(

        @ArraySchema(
            arraySchema = @Schema(description = "예측가의 예측 수행일 목록"),
            schema = @Schema(description = "예측 수행일", example = "20250611")
        )
        List<String> fulfillmentDates,

        @Schema(description = "현물 & 선물 리스트")
        List<SettingDetail> settingDetails

    ) {

        @Builder
        public record SettingDetail(

            @Schema(description = "설정 상세번호", example = "1004")
            Integer settingDetailNo,

            @Schema(description = "설정 설명, 설정 명", example = "Actual")
            String settingDetailExpn,

            @Schema(description = "설정 타입", example = "SPOT", allowableValues = {"SPOT", "FUTURES", "FORECAST"})
            PriceType priceType,

            @Schema(description = "설정 타입 명", example = "현물가", allowableValues = {"현물가", "선물가", "예측가"})
            String priceTypeNm

        ) {

            public static SettingDetail of(PriceType priceType, ChartPredictSettingDetail settingDetail) {
                return SettingDetail.builder()
                    .settingDetailNo(settingDetail.getChartPredictSettingDetailNo())
                    .settingDetailExpn(settingDetail.getPredictChartDetailExplanation())
                    .priceType(priceType)
                    .priceTypeNm(priceType.getDescription())
                    .build();
            }

            public static List<SettingDetail> ofList(List<ChartPredictSettingDetail> spots, List<ChartPredictSettingDetail> futures) {
                ArrayList<SettingDetail> result = new ArrayList<>();
                result.addAll(spots.stream().map(spot -> SettingDetail.of(PriceType.SPOT, spot)).toList());
                result.addAll(futures.stream().map(future -> SettingDetail.of(PriceType.FUTURES, future)).toList());
                return result;
            }

        }

        private static Target of(ForecastContext forecastContext) {
            return Target.builder()
                .fulfillmentDates(forecastContext.fulfillDates())
                .settingDetails(SettingDetail.ofList(forecastContext.spots(), forecastContext.futures()))
                .build();
        }

        public static List<Target> ofList(List<ForecastContext> forecastContexts) {
            return forecastContexts.stream().map(Target::of).toList();
        }

    }

    public static HistoryMetadataRes of(ChartPredictSetting predictSetting, List<ForecastContext> forecastContexts) {
        return HistoryMetadataRes.builder()
            .predictTargetSettingNm(predictSetting.getPredictTargetSettingNm())
            .predictPeriodUnitSeparateCd(predictSetting.getPredictPeriodUnitSeparateCd())
            .targets(Target.ofList(forecastContexts))
            .build();
    }

    public static HistoryMetadataRes of(ChartPredictSetting predictSetting) {
        return HistoryMetadataRes.builder()
            .predictTargetSettingNm(predictSetting.getPredictTargetSettingNm())
            .predictPeriodUnitSeparateCd(predictSetting.getPredictPeriodUnitSeparateCd())
            .build();
    }

}
