package com.sk.domain.mi.dto.response.charts;

import com.sk.domain.shared.enums.chart.ChartType;
import com.sk.domain.shared.enums.chart.Interval;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.util.Set;

@Builder
public record ChartMetadata(

    @Schema(description = "화폐", example = "$")
    String currency,

    @Schema(description = "현재 값 단위", example = "MMBtu")
    String currentUnitCd,

    @Schema(description = "차트 간격", example = "1M")
    String currentInterval,

    @Schema(description = "초기 차트 타입", example = "CANDLE", allowableValues = {"CANDLE", "LINE"})
    ChartType initialChartType,

    @Schema(description = "변경할 수 있는 단위코드 목록", example = "Brent#kbbl")
    Set<String> changeableUnitCd

) {

    public static ChartMetadata of(String currency, ChartType chartType, String unitConversionCd, Set<String> changeable, Interval interval) {
        return ChartMetadata.builder()
            .currency(currency)
            .initialChartType(chartType)
            .currentUnitCd(unitConversionCd)
            .currentInterval(interval.getCode())
            .changeableUnitCd(changeable)
            .build();
    }

}
