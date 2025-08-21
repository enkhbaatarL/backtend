package com.sk.domain.mi.dto.response.marketData.charts.metaData;

import com.sk.domain.mi.dto.response.marketData.charts.graph.MarketDataChartRawData;
import java.time.LocalDate;

import com.sk.domain.mi.dto.response.marketData.charts.calculatePeriodChange.CalculatePeriodChangeRes;
import com.sk.domain.shared.entity.master.tcm.MarketDataChartSetting;
import com.sk.domain.shared.entity.master.tmd.GoodsData;
import com.sk.domain.shared.entity.master.tmd.GoodsDataSourceDetail;
import com.sk.domain.shared.enums.PeriodUnitSeparateCd;
import com.sk.domain.shared.enums.chart.ChartType;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.ArrayList;
import java.util.List;
import lombok.Builder;

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

    @Schema(description = "최근 주 변동률 (WOW)", example = "10.5") Double wowChangeRate,
    @Schema(description = "최근 일 변동률 (DOD)", example = "10.5") Double dodChangeRate,
    @Schema(description = "최근 변동률", example = "10.5") Double recentChangeRate,
    @Schema(description = "최고 값, 날짜") MarketDataChartRawData max,
    @Schema(description = "최저 값, 날짜") MarketDataChartRawData min,
    @Schema(description = "비교 수치 리스트") List<ChangeRate> changeRates
    ) {

    @Builder
    public MarketMetaData(Short chartMarkRank, Integer goodsNo, Integer goodsDataNo, Integer goodsDataSourceNo,
                          String goodsDataNm, String goodsDataSourceNm, String currency, String currentUnitCd,
                          ChartType visibleChartType, String interval, LocalDate lastUpdateDt, LocalDate indexStartDt,
                          Double lastValue, Double yoyChangeRate, Double momChangeRate, Double wowChangeRate,
                          Double dodChangeRate, Double recentChangeRate, MarketDataChartRawData max, MarketDataChartRawData min, List<ChangeRate> changeRates) {
        this.chartMarkRank = chartMarkRank;
        this.goodsNo = goodsNo;
        this.goodsDataNo = goodsDataNo;
        this.goodsDataSourceNo = goodsDataSourceNo;
        this.goodsDataNm = goodsDataNm;
        this.goodsDataSourceNm = goodsDataSourceNm;
        this.currency = currency;
        this.currentUnitCd = currentUnitCd;
        this.visibleChartType = visibleChartType;
        this.interval = interval;
        this.lastUpdateDt = lastUpdateDt;
        this.indexStartDt = indexStartDt;
        this.lastValue = lastValue;
        this.yoyChangeRate = yoyChangeRate;
        this.momChangeRate = momChangeRate;
        this.wowChangeRate = wowChangeRate;
        this.dodChangeRate = dodChangeRate;
        this.recentChangeRate = recentChangeRate;
        this.max = max;
        this.min = min;
        this.changeRates = changeRates;
    }

    public static MarketMetaData of(MarketDataChartSetting marketDataChartSetting, GoodsData goodsData, GoodsDataSourceDetail goodsDataSourceDetail, CalculatePeriodChangeRes calculatePeriodChangeRes, String interval,
            LocalDate indexStartDt, Double lastValue, LocalDate lastUpdateDt, Double recentChangeRate, MarketDataChartRawData max, MarketDataChartRawData min) {

        Double dodChangeRate = null;
        if(goodsDataSourceDetail.getPeriodUnitSeparateCd() == null || goodsDataSourceDetail.getPeriodUnitSeparateCd() == PeriodUnitSeparateCd.D){
            dodChangeRate = recentChangeRate;
        }
        // temp
        List<ChangeRate> changeRates = new ArrayList<>();
        if(dodChangeRate != null){
            changeRates.add(new ChangeRate("DoD", dodChangeRate));
        }
        if(calculatePeriodChangeRes != null){
            if(calculatePeriodChangeRes.wowChangeRate() != null){
                changeRates.add(new ChangeRate("WoW", calculatePeriodChangeRes.wowChangeRate()));
            }
            if(calculatePeriodChangeRes.momChangeRate() != null){
                changeRates.add(new ChangeRate("MoM", calculatePeriodChangeRes.momChangeRate()));
            }
            if(calculatePeriodChangeRes.yoyChangeRate() != null){
                changeRates.add(new ChangeRate("YoY", calculatePeriodChangeRes.yoyChangeRate()));
            }
        }
        changeRates = changeRates.stream().limit(2).toList();

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
                .yoyChangeRate(calculatePeriodChangeRes != null ? calculatePeriodChangeRes.yoyChangeRate() : null)
                .momChangeRate(calculatePeriodChangeRes != null ? calculatePeriodChangeRes.momChangeRate() : null)
                .wowChangeRate(calculatePeriodChangeRes != null ? calculatePeriodChangeRes.wowChangeRate() : null)
                .dodChangeRate(dodChangeRate)
                .recentChangeRate(recentChangeRate)
                .changeRates(changeRates)
                .min(min)
                .max(max)
                .build();
    }

    public static MarketMetaData of(MarketDataChartSetting marketDataChartSetting, GoodsData goodsData, GoodsDataSourceDetail goodsDataSourceDetail, String interval, LocalDate indexStartDt, LocalDate lastUpdateDt) {
        return MarketMetaData.builder()
                .chartMarkRank(marketDataChartSetting.getChartMarkRank())
                .goodsNo(goodsData.getGoods().getGoodsNo())
                .goodsDataNo(goodsData.getGoodsDataNo())
                .goodsDataSourceNo(goodsDataSourceDetail.getGoodsDataSourceNo())
                .goodsDataNm(goodsData.getGoodsDataNm())
                .goodsDataSourceNm(goodsDataSourceDetail.getGoodsDataSourceNm())
                .currency(goodsDataSourceDetail.getGoodsDataSourceCurrencyUnitNm())
                .currentUnitCd(goodsDataSourceDetail.getGoodsDataSourceUnitNm())
                .interval(interval)
                .indexStartDt(indexStartDt)
                .lastUpdateDt(lastUpdateDt)
                .build();
    }
}
