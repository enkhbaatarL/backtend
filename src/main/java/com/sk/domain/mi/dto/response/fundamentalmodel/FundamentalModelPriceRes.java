package com.sk.domain.mi.dto.response.fundamentalmodel;

import com.sk.domain.shared.enums.fundamentalmodel.PriceSeriesType;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

public record FundamentalModelPriceRes(

    @Schema(description = "case ID", example = "106")
    Long caseId,

    @Schema(description = "Monthly Futures Shoulder Chart용 라인 그래프 데이터", implementation = PriceChart.class)
    PriceChart chart,

    @Schema(description = "연간 가격 테이블", implementation = PriceTable.class)
    PriceTable annualTable,

    @Schema(description = "월별 가격 테이블", implementation = PriceTable.class)
    PriceTable monthlyTable

) {

    public record PriceChart(

        @ArraySchema(schema = @Schema(implementation = PriceChartSeriesSet.class))
        List<PriceChartSeriesSet> seriesSets

    ) {
    }

    public record PriceChartSeriesSet(

        @Schema(description = "가격 지역 구분", example = "East Asia Spot")
        String priceRegion,

        @ArraySchema(schema = @Schema(implementation = PriceChartSeries.class))
        List<PriceChartSeries> series

    ) {
    }

    public record PriceChartSeries(

        @Schema(description = "series 구분", implementation = PriceSeriesType.class, example = "BASE_CASE")
        PriceSeriesType seriesType,

        @Schema(description = "범례 표시 이름", example = "Base case price")
        String label,

        @ArraySchema(schema = @Schema(implementation = PriceChartPoint.class))
        List<PriceChartPoint> points

    ) {
    }

    public record PriceChartPoint(

        @Schema(description = "연도", example = "2026")
        Integer year,

        @Schema(description = "월", example = "1")
        Integer month,

        @Schema(description = "가격 값", example = "8.51")
        Double value

    ) {
    }

    public record PriceTable(

        @ArraySchema(schema = @Schema(implementation = PriceColumn.class))
        List<PriceColumn> columns,

        @ArraySchema(schema = @Schema(implementation = PriceRow.class))
        List<PriceRow> rows

    ) {
    }

    public record PriceColumn(

        @Schema(description = "컬럼 키", example = "year")
        String key,

        @Schema(description = "컬럼 표시 이름", example = "Year")
        String label

    ) {
    }

    public record PriceRow(

        @Schema(description = "연도", example = "2023")
        Integer year,

        @Schema(description = "월. annualTable에서는 null입니다.", example = "10")
        Integer month,

        @Schema(description = "가격 지역", example = "East Asia Spot")
        String priceRegion,

        @Schema(description = "기준 가격 (US$/mmbtu)", example = "15.71")
        Double baseCasePrice,

        @Schema(description = "사용자 조정 가격 (US$/mmbtu)", example = "15.71")
        Double userAdjustedPrice

    ) {
    }
}
