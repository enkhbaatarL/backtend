package com.sk.domain.mi.dto.response.charts;

import com.sk.domain.mi.service.chart.dto.ChartSettingsContext;
import com.sk.domain.shared.entity.master.tmd.GoodsDataSourceDetail;
import com.sk.domain.shared.enums.chart.ChartType;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.util.List;
import java.util.Set;

@Builder
public record ChartMetadataSimple(

    @Schema(description = "화폐", example = "$")
    String currency,

    @Schema(description = "현재 값 단위", example = "MMBtu")
    String currentUnitCd,

    @Schema(description = "최소 차트 간격", example = "1D")
    String minInterval,

    @Schema(description = "개인화 또는 기본값으로 설정된 차트 간격", example = "1M")
    String currentInterval,

    @Schema(description = "개인화 또는 기본값으로 설정된 차트 범위", example = "1Y")
    String currentRange,

    @ArraySchema(
        arraySchema = @Schema(description = "개인화 또는 기본값으로 설정된 차트 범위 날짜 목록"),
        schema = @Schema(description = "시작일, 종료일", example = "20240101")
    )
    List<String> currentRangeBetween,

    @Schema(description = "개인화 또는 기본값으로 설정된 차트 타입", example = "LINE", allowableValues = {"CANDLE", "LINE"})
    ChartType initialChartType,

    @Schema(description = "변경할 수 있는 단위코드 목록", example = "brent#kbbl")
    Set<String> changeableUnitCd

) {

    public static ChartMetadataSimple of(GoodsDataSourceDetail gds, ChartSettingsContext contextV3, Set<String> allowedUnitCds) {
        return ChartMetadataSimple.builder()
            .currency(gds.getGoodsDataSourceCurrencyUnitNm())
            .currentUnitCd(contextV3.finalizedUnitCd().toString())
            .minInterval(gds.getPeriodUnitSeparateCd().minInterval().getCode())
            .currentInterval(contextV3.finalizedInterval().getCode())
            .currentRange(contextV3.finalizedRange().getCode())
            .currentRangeBetween(List.of(contextV3.finalizedRangeBetween().getLeft(), contextV3.finalizedRangeBetween().getRight()))
            .initialChartType(contextV3.finalizedChartType())
            .changeableUnitCd(allowedUnitCds)
            .build();
    }

}
