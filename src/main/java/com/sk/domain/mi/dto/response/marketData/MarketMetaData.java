package com.sk.domain.mi.dto.response.marketData;

import java.time.LocalDate;

import com.sk.domain.shared.entity.master.tcm.MarketDataChartSetting;
import com.sk.domain.shared.entity.master.tmd.GoodsData;
import com.sk.domain.shared.entity.master.tmd.GoodsDataSourceDetail;
import com.sk.domain.shared.enums.chart.ChartType;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
public record MarketMetaData(

        @Schema(description = "차트 표시 순위", example = "1") Short chartMarkRank,

        @Schema(description = "상품번호", example = "1") Integer goodsNo,

        @Schema(description = "상품데이터번호", example = "1") Integer goodsDataNo,

        @Schema(description = "상품데이터소스번호", example = "1") Integer goodsDataSourceNo,

        @Schema(description = "상품데이터명", example = "Brent") String goodsDataNm,

        @Schema(description = "상품데이터소스명", example = "Brent") String goodsDataSourceNm,

        @Schema(description = "화폐", example = "$") String currency,

        @Schema(description = "현재 값 단위", example = "MMBtu") String currentUnitCd,

        @Schema(description = "차트 타입", example = "LINE") ChartType visibleChartType,

        @Schema(description = "간격", example = "1M") String interval,

        @Schema(description = "마지막 업데이트 일시", example = "2021-01-01") LocalDate lastUpdateDt,

        @Schema(description = "인덱스 시작일시", example = "2021-01-01") LocalDate indexStartDt,

        @Schema(description = "수치", example = "95.5") Double lastValue,

        @Schema(description = "최근 1년 변동률 (YOY)", example = "10.5") Double yoyChangeRate,

        @Schema(description = "최근 월 변동률 (MOM)", example = "10.5") Double momChangeRate,

        @Schema(description = "최근 주 변동률 (WOW)", example = "10.5") Double wowChangeRate) {

    public static MarketMetaData of(MarketDataChartSetting marketDataChartSetting, GoodsData goodsData, GoodsDataSourceDetail goodsDataSourceDetail,
            ChangeRatesRes changeRates, String interval,
            LocalDate indexStartDt, Double lastValue, LocalDate lastUpdateDt) {
        return MarketMetaData.builder()
                .chartMarkRank(marketDataChartSetting.getChartMarkRank())
                .goodsNo(goodsData.getGoods().getGoodsNo())
                .goodsDataNo(goodsData.getGoodsDataNo())
                .goodsDataSourceNo(goodsDataSourceDetail.getGoodsDataSourceNo())
                .goodsDataNm(goodsData.getGoodsDataNm())
                .goodsDataSourceNm(goodsDataSourceDetail.getGoodsDataSourceNm())
                .currency(goodsDataSourceDetail.getGoodsDataSourceCurrencyUnitNm())
                .currentUnitCd(goodsDataSourceDetail.getGoodsDataSourceUnitNm())
                .visibleChartType(ChartType.LINE)
                .interval(interval)
                .lastUpdateDt(lastUpdateDt)
                .indexStartDt(indexStartDt)
                .lastValue(lastValue)
                .yoyChangeRate(changeRates.yoyChangeRate())
                .momChangeRate(changeRates.momChangeRate())
                .wowChangeRate(changeRates.wowChangeRate())
                .build();
    }

    public static MarketMetaData of(MarketDataChartSetting marketDataChartSetting) {
        return MarketMetaData.builder()
                .chartMarkRank(marketDataChartSetting.getChartMarkRank())
                .build();
    }
}
