package com.sk.domain.mi.dto.response.charts;

import com.sk.domain.mi.service.chart.dto.UnitCd;
import com.sk.domain.shared.enums.chart.ChartType;
import com.sk.domain.shared.enums.chart.Interval;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.util.Set;
import java.util.stream.Collectors;

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

    public static ChartMetadata of(String currency, ChartType chartType, UnitCd unitConversionCd, Set<UnitCd> changeable, Interval interval) {
        return ChartMetadata.builder()
            .currency(currency)
            .initialChartType(chartType)
            .currentUnitCd(unitConversionCd.toString())
            .currentInterval(interval.getCode())
            .changeableUnitCd(changeable.stream().map(UnitCd::toString).collect(Collectors.toSet()))
            .build();
    }

}
