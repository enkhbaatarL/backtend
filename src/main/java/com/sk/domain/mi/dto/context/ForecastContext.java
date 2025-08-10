package com.sk.domain.mi.dto.context;

import com.sk.domain.shared.entity.master.tcm.ChartPredictSettingDetail;
import lombok.Builder;

import java.util.List;

@Builder
public record ForecastContext(

    ChartPredictSettingDetail forecast,
    List<String> fulfillDates,
    List<ChartPredictSettingDetail> actual,
    List<ChartPredictSettingDetail> futures

) {

    public static ForecastContext of(ChartPredictSettingDetail forecast, List<String> fulfillDates, List<ChartPredictSettingDetail> actual, List<ChartPredictSettingDetail> futures) {
        return ForecastContext.builder()
            .forecast(forecast)
            .fulfillDates(fulfillDates)
            .actual(actual)
            .futures(futures)
            .build();
    }

}
