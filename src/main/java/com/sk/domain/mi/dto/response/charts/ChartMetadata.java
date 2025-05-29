package com.sk.domain.mi.dto.response.charts;

import com.sk.domain.mi.service.chart.dto.UnitCd;
import com.sk.domain.shared.entity.master.tmd.GoodsDataSourceDetail;
import com.sk.domain.shared.enums.chart.ChartType;
import com.sk.domain.shared.enums.chart.Interval;
import com.sk.domain.shared.enums.chart.Range;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Builder
public record ChartMetadata(

    @Schema(description = "화폐", example = "$")
    String currency,

    @Schema(description = "현재 값 단위", example = "MMBtu")
    String currentUnitCd,

    @Schema(description = "최소 차트 간격", example = "1D")
    String minInterval,

    @Schema(description = "현재 차트 간격", example = "1M")
    String currentInterval,

    @Schema(description = "현재 차트 범위", example = "1Y")
    String currentRange,

    @ArraySchema(
        arraySchema = @Schema(description = "현재 차트 범위 날짜 목록"),
        schema = @Schema(description = "시작일, 종료일", example = "20240101")
    )
    List<String> currentRangeBetween,

    @Schema(description = "초기 차트 타입", example = "CANDLE", allowableValues = {"CANDLE", "LINE"})
    ChartType initialChartType,

    @Schema(description = "변경할 수 있는 단위코드 목록", example = "Brent#kbbl")
    Set<String> changeableUnitCd,

    @Schema(description = "데이터소스 최신 업데이트 일자", example = "20250505")
    String latestDate

) {

    public static ChartMetadata of(GoodsDataSourceDetail gds, ChartType chartType, UnitCd unitConversionCd, Set<UnitCd> changeable, Interval interval, Range range, Pair<String, String> rangeBetween, String latestDate) {
        return ChartMetadata.builder()
            .currency(gds.getGoodsDataSourceCurrencyUnitNm())
            .currentUnitCd(unitConversionCd.toString())
            .minInterval(gds.getPeriodUnitSeparateCd().minInterval().getCode())
            .currentInterval(interval.getCode())
            .currentRange(range.getCode())
            .currentRangeBetween(List.of(rangeBetween.getLeft(), rangeBetween.getRight()))
            .initialChartType(chartType)
            .changeableUnitCd(changeable.stream().map(UnitCd::toString).collect(Collectors.toSet()))
            .latestDate(latestDate)
            .build();
    }

}
