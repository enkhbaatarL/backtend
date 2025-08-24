package com.sk.domain.mi.dto.response.marketData.charts.metaData;

import com.sk.domain.mi.dto.response.marketData.charts.calculatePeriodChange.CalculatePeriodChangeValue;
import com.sk.domain.shared.enums.marketData.CalculatePeriodChangeType;
import java.time.LocalDate;

import com.sk.domain.shared.entity.master.tcm.MarketDataChartSetting;
import com.sk.domain.shared.entity.master.tmd.GoodsData;
import com.sk.domain.shared.entity.master.tmd.GoodsDataSourceDetail;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Map;
import lombok.Builder;

public record MarketMetaDataSummary(

        @Schema(description = "차트 표시 순위", example = "1") Short chartMarkRank,

        @Schema(description = "상품번호", example = "1") Integer goodsNo,

        @Schema(description = "상품데이터번호", example = "1") Integer goodsDataNo,

        @Schema(description = "상품데이터소스번호", example = "1") Integer goodsDataSourceNo,

        @Schema(description = "상품데이터명", example = "Brent") String goodsDataNm,

        @Schema(description = "상품데이터소스명", example = "Brent") String goodsDataSourceNm,

        @Schema(description = "화폐", example = "$") String currency,

        @Schema(description = "현재 값 단위", example = "MMBtu") String currentUnitCd,

        @Schema(description = "간격", example = "1M") String interval,

        @Schema(description = "마지막 업데이트 일시", example = "2021-01-01") LocalDate lastUpdateDt,

        @Schema(description = "인덱스 시작일시", example = "2021-01-01") LocalDate indexStartDt,

        @Schema(description = "수치", example = "95.5") Double lastValue,

        @Schema(description = "최근 월 변동률 (MOM)", example = "10.5") Double momChangeRate,

        @Schema(description = "최근 월 변동 수치 (MOM)", example = "10.5") Double momChangeValue,

        @Schema(description = "최근 주 변동률 (WOW)", example = "10.5") Double wowChangeRate,

        @Schema(description = "최근 주 변동 수치 (WOW)", example = "10.5") Double wowChangeValue,

        @Schema(description = "최근 일 변동률 (DOD)", example = "10.5") Double dodChangeRate,

        @Schema(description = "최근 일 변동 수치 (DOD)", example = "10.5") Double dodChangeValue,
        @Schema(description = "최근 변동 수치", example = "10.5") Double recentChangeValue,
        @Schema(description = "최근 변동률", example = "10.5") Double recentChangeRate) {

    @Builder
    public MarketMetaDataSummary(Short chartMarkRank, Integer goodsNo, Integer goodsDataNo, Integer goodsDataSourceNo, String goodsDataNm,
            String goodsDataSourceNm, String currency, String currentUnitCd, String interval, LocalDate lastUpdateDt, LocalDate indexStartDt,
            Double lastValue, Double momChangeRate, Double momChangeValue, Double wowChangeRate, Double wowChangeValue, Double dodChangeRate,
            Double dodChangeValue, Double recentChangeValue, Double recentChangeRate) {
        this.chartMarkRank = chartMarkRank;
        this.goodsNo = goodsNo;
        this.goodsDataNo = goodsDataNo;
        this.goodsDataSourceNo = goodsDataSourceNo;
        this.goodsDataNm = goodsDataNm;
        this.goodsDataSourceNm = goodsDataSourceNm;
        this.currency = currency;
        this.currentUnitCd = currentUnitCd;
        this.interval = interval;
        this.lastUpdateDt = lastUpdateDt;
        this.indexStartDt = indexStartDt;
        this.lastValue = lastValue;
        this.momChangeRate = momChangeRate;
        this.momChangeValue = momChangeValue;
        this.wowChangeRate = wowChangeRate;
        this.wowChangeValue = wowChangeValue;
        this.dodChangeRate = dodChangeRate;
        this.dodChangeValue = dodChangeValue;
        this.recentChangeRate = recentChangeRate;
        this.recentChangeValue = recentChangeValue;
    }

    public static MarketMetaDataSummary of(MarketDataChartSetting marketDataChartSetting, GoodsData goodsData,
            GoodsDataSourceDetail goodsDataSourceDetail, Map<CalculatePeriodChangeType, CalculatePeriodChangeValue> calculatePeriodChangeMap, String interval,
            LocalDate indexStartDt, Double lastValue, LocalDate lastUpdateDt) {
        Double recentChangeValue = null;
        Double recentChangeRate = null;
        if(calculatePeriodChangeMap.containsKey(CalculatePeriodChangeType.DOD)){
            recentChangeValue = calculatePeriodChangeMap.get(CalculatePeriodChangeType.DOD).changeValue();
            recentChangeRate = calculatePeriodChangeMap.get(CalculatePeriodChangeType.DOD).changeRate();
        }else if(calculatePeriodChangeMap.containsKey(CalculatePeriodChangeType.WOW)){
            recentChangeValue = calculatePeriodChangeMap.get(CalculatePeriodChangeType.WOW).changeValue();
            recentChangeRate = calculatePeriodChangeMap.get(CalculatePeriodChangeType.WOW).changeRate();
        } else if (calculatePeriodChangeMap.containsKey(CalculatePeriodChangeType.MOM)) {
          recentChangeValue = calculatePeriodChangeMap.get(CalculatePeriodChangeType.MOM).changeValue();
          recentChangeRate = calculatePeriodChangeMap.get(CalculatePeriodChangeType.MOM).changeRate();
        }else if(calculatePeriodChangeMap.containsKey(CalculatePeriodChangeType.YOY)){
            recentChangeValue = calculatePeriodChangeMap.get(CalculatePeriodChangeType.YOY).changeValue();
            recentChangeRate = calculatePeriodChangeMap.get(CalculatePeriodChangeType.YOY).changeRate();
        }
        return MarketMetaDataSummary.builder()
                .chartMarkRank(marketDataChartSetting.getChartMarkRank())
                .goodsNo(goodsData.getGoods().getGoodsNo())
                .goodsDataNo(goodsData.getGoodsDataNo())
                .goodsDataSourceNo(goodsDataSourceDetail.getGoodsDataSourceNo())
                .goodsDataNm(goodsData.getGoodsDataNm())
                .goodsDataSourceNm(goodsDataSourceDetail.getGoodsDataSourceNm())
                .currency(goodsDataSourceDetail.getGoodsDataSourceCurrencyUnitNm())
                .currentUnitCd(goodsDataSourceDetail.getGoodsDataSourceUnitNm())
                .interval(interval)
                .lastUpdateDt(lastUpdateDt)
                .indexStartDt(indexStartDt)
                .lastValue(lastValue)
                .momChangeRate(
                    calculatePeriodChangeMap.containsKey(CalculatePeriodChangeType.MOM) ?
                        calculatePeriodChangeMap.get(CalculatePeriodChangeType.MOM).changeRate() : null)
                .momChangeValue(
                    calculatePeriodChangeMap.containsKey(CalculatePeriodChangeType.MOM) ?
                        calculatePeriodChangeMap.get(CalculatePeriodChangeType.MOM).changeValue() : null)
                .wowChangeRate(
                    calculatePeriodChangeMap.containsKey(CalculatePeriodChangeType.WOW) ?
                        calculatePeriodChangeMap.get(CalculatePeriodChangeType.WOW).changeRate() : null)
                .wowChangeValue(
                    calculatePeriodChangeMap.containsKey(CalculatePeriodChangeType.WOW) ?
                        calculatePeriodChangeMap.get(CalculatePeriodChangeType.WOW).changeValue() : null)
                .dodChangeRate(
                    calculatePeriodChangeMap.containsKey(CalculatePeriodChangeType.DOD) ?
                        calculatePeriodChangeMap.get(CalculatePeriodChangeType.DOD).changeRate() : null)
                .dodChangeValue(
                    calculatePeriodChangeMap.containsKey(CalculatePeriodChangeType.DOD) ?
                        calculatePeriodChangeMap.get(CalculatePeriodChangeType.DOD).changeValue() : null)
                .recentChangeValue(recentChangeValue)
                .recentChangeRate(recentChangeRate)
                .build();
    }

    public static MarketMetaDataSummary of(MarketDataChartSetting marketDataChartSetting) {
        return MarketMetaDataSummary.builder()
                .chartMarkRank(marketDataChartSetting.getChartMarkRank())
                .build();
    }
}
