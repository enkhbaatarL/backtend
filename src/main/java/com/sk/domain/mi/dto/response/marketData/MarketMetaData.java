package com.sk.domain.mi.dto.response.marketData;

import java.time.LocalDateTime;

import com.sk.domain.shared.entity.master.tmd.GoodsData;
import com.sk.domain.shared.entity.master.tmd.GoodsDataSourceDetail;
import com.sk.domain.shared.enums.chart.ChartType;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
public record MarketMetaData(

        @Schema(description = "상품데이터번호", example = "1") Integer goodsDataNo,

        @Schema(description = "상품데이터소스번호", example = "1") Integer goodsDataSourceNo,

        @Schema(description = "상품데이터명", example = "Brent") String goodsDataNm,

        @Schema(description = "상품데이터소스명", example = "Brent") String goodsDataSourceNm,

        @Schema(description = "화폐", example = "$") String currency,

        @Schema(description = "현재 값 단위", example = "MMBtu") String currentUnitCd,

        @Schema(description = "차트 타입", example = "LINE") ChartType visibleChartType,

        @Schema(description = "간격", example = "1M") String interval,

        @Schema(description = "마지막 업데이트 일시", example = "2021-01-01T00:00:00") LocalDateTime lastUpdatedDt,

        @Schema(description = "인덱스 시작일시", example = "2021-01-01T00:00:00") LocalDateTime indexStartDate,

        @Schema(description = "차트 시작일시", example = "2021-01-01T00:00:00") LocalDateTime chartStartDate,

        @Schema(description = "차트 종료일시", example = "2021-01-01T00:00:00") LocalDateTime chartEndDate,

        @Schema(description = "수치", example = "95.5") Double value,

        @Schema(description = "최근 1년 변동률", example = "10.5") Double yoyChangeRate,

        @Schema(description = "최근 월 변동률", example = "10.5") Double momChangeRate,

        @Schema(description = "최근 주 변동률", example = "10.5") Double wowChangeRate) {

    @Builder
    public MarketMetaData(Integer goodsDataNo, Integer goodsDataSourceNo, String goodsDataNm, String goodsDataSourceNm, String currency,
            String currentUnitCd, ChartType visibleChartType, String interval, LocalDateTime lastUpdatedDt, LocalDateTime indexStartDate,
            LocalDateTime chartStartDate, LocalDateTime chartEndDate, Double value, Double yoyChangeRate, Double momChangeRate,
            Double wowChangeRate) {
        this.goodsDataNo = goodsDataNo;
        this.goodsDataSourceNo = goodsDataSourceNo;
        this.goodsDataNm = goodsDataNm;
        this.goodsDataSourceNm = goodsDataSourceNm;
        this.currency = currency;
        this.currentUnitCd = currentUnitCd;
        this.visibleChartType = visibleChartType;
        this.interval = interval;
        this.lastUpdatedDt = lastUpdatedDt;
        this.indexStartDate = indexStartDate;
        this.chartStartDate = chartStartDate;
        this.chartEndDate = chartEndDate;
        this.value = value;
        this.yoyChangeRate = yoyChangeRate;
        this.momChangeRate = momChangeRate;
        this.wowChangeRate = wowChangeRate;
    }

    public static MarketMetaData of(GoodsData goodsData, GoodsDataSourceDetail goodsDataSourceDetail, String interval, LocalDateTime lastUpdatedDt,
            LocalDateTime indexStartDate, LocalDateTime chartStartDate, LocalDateTime chartEndDate, Double value, Double yoyChangeRate,
            Double momChangeRate, Double wowChangeRate) {
        return MarketMetaData.builder()
                .goodsDataNo(goodsData.getGoodsDataNo())
                .goodsDataSourceNo(goodsDataSourceDetail.getGoodsDataSourceNo())
                .goodsDataNm(goodsData.getGoodsDataNm())
                .goodsDataSourceNm(goodsDataSourceDetail.getGoodsDataSourceNm())
                .currency(goodsDataSourceDetail.getGoodsDataSourceCurrencyUnitNm())
                .currentUnitCd(goodsDataSourceDetail.getGoodsDataSourceUnitNm())
                .visibleChartType(ChartType.LINE)
                .interval(interval)
                .lastUpdatedDt(lastUpdatedDt)
                .indexStartDate(indexStartDate)
                .chartStartDate(chartStartDate)
                .chartEndDate(chartEndDate)
                .value(value)
                .yoyChangeRate(yoyChangeRate)
                .momChangeRate(momChangeRate)
                .wowChangeRate(wowChangeRate)
                .build();
    }
}
