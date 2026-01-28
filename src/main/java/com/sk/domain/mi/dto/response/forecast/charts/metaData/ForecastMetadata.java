package com.sk.domain.mi.dto.response.forecast.charts.metaData;

import com.sk.domain.shared.entity.master.tmd.GoodsDataSourceDetail;
import com.sk.domain.shared.enums.dashboard.DashboardChartDataTableType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import lombok.Builder;

public record ForecastMetadata(
        @Schema(description = "시장지수분석대상코드", example = "EN_LNG") String marketIndexAnalysisTargetCd,
        @Schema(description = "마지막 업데이트 일시", example = "2021-01-01") LocalDate lastUpdateDt,
        @Schema(description = "화폐", example = "$") String currency,
        @Schema(description = "현재 값 단위", example = "MMBtu") String currentUnitCd,
        @Schema(description = "기준 날짜(YYYY-MM)", example = "2021-01") String baseDate,
        @Schema(description = "데이터 테이블 타입", example = "FORECAST_SINGLE_LAYER") DashboardChartDataTableType dataTableType,
        @Schema(description = "테이블 헤더 명", example = "Vehicles") String tableHeader) {

    @Builder
    public ForecastMetadata(String marketIndexAnalysisTargetCd, LocalDate lastUpdateDt, String currency,
            String currentUnitCd, String baseDate, DashboardChartDataTableType dataTableType, String tableHeader) {
        this.marketIndexAnalysisTargetCd = marketIndexAnalysisTargetCd;
        this.lastUpdateDt = lastUpdateDt;
        this.currency = currency;
        this.currentUnitCd = currentUnitCd;
        this.baseDate = baseDate;
        this.dataTableType = dataTableType;
        this.tableHeader = tableHeader;
    }

    public static ForecastMetadata of(String marketIndexAnalysisTargetCd, LocalDate lastUpdateDt,
            GoodsDataSourceDetail goodsDataSourceDetail, String baseDate, DashboardChartDataTableType dataTableType,
            String tableHeader) {
        return ForecastMetadata.builder()
                .marketIndexAnalysisTargetCd(marketIndexAnalysisTargetCd)
                .lastUpdateDt(lastUpdateDt)
                .currency(goodsDataSourceDetail.getGoodsDataSourceCurrencyUnitNm())
                .currentUnitCd(goodsDataSourceDetail.getGoodsDataSourceUnitNm())
                .baseDate(baseDate)
                .dataTableType(dataTableType)
                .tableHeader(tableHeader)
                .build();
    }
}
