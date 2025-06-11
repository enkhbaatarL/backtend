package com.sk.domain.mi.dto.response.dashboard.history;

import com.sk.common.utils.DateUtil;
import com.sk.common.utils.MathUtil;
import com.sk.domain.shared.entity.master.tcm.ChartPredictSetting;
import com.sk.domain.shared.enums.PeriodUnitSeparateCd;
import com.sk.domain.shared.enums.PriceType;
import com.sk.domain.shared.mapper.dto.DoubleValueResultData;
import com.sk.domain.shared.mapper.dto.TargetPredictResultData;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Builder
public record HistoryDataRes(

    @Schema(description = "예측결과 일자 또는 달", example = "20250601")
    String date,
    @Schema(description = "예측 데이터")
    PriceEntry baseline,
    @Schema(description = "실제 또는 선물 데이터")
    PriceEntry counterpart
) {

    public static List<HistoryDataRes> ofList(ChartPredictSetting setting, List<TargetPredictResultData> forecastResults, List<DoubleValueResultData> counterPartResults, Boolean isFuture, Boolean isCounterPartPermitted, LocalDate fulfillDate) {

        PeriodUnitSeparateCd periodCd = setting.getScopePeriodUnitSeparateCd();

        Map<String, BigDecimal> forecastMap = forecastResults.stream()
            .filter(r -> r != null && r.getPredictResultDate() != null && r.getPredictResultValue() != null)
            .collect(Collectors.toMap(
                r -> DateUtil.format(DateUtil.fromYmd(r.getPredictResultDate()), periodCd),
                TargetPredictResultData::getPredictResultValue,
                (v1, v2) -> v2, // 중복 키는 뒤에 온 값으로
                TreeMap::new
            ));

        Map<String, BigDecimal> counterpartMap = counterPartResults.stream()
            .filter(r -> r != null && r.getDate() != null && r.getValue() != null)
            .collect(Collectors.toMap(
                r -> DateUtil.format(r.getDate().toLocalDate(), periodCd),
                DoubleValueResultData::getValue,
                (v1, v2) -> v2,
                TreeMap::new
            ));

        BigDecimal maxForecast = forecastMap.values().stream().max(BigDecimal::compareTo).orElse(null);
        BigDecimal minForecast = forecastMap.values().stream().min(BigDecimal::compareTo).orElse(null);
        BigDecimal maxCounter  = counterpartMap.values().stream().max(BigDecimal::compareTo).orElse(null);
        BigDecimal minCounter  = counterpartMap.values().stream().min(BigDecimal::compareTo).orElse(null);

        SortedSet<String> dates = new TreeSet<>();
        dates.addAll(forecastMap.keySet());
//        dates.addAll(counterpartMap.keySet());

        List<HistoryDataRes> result = new ArrayList<>(dates.size());
        BigDecimal prevForecast = null;
        BigDecimal prevCounter  = null;

        // Monthly 예측값, 예측 시점 달 실제가 또는 선물가를 포함
        if (PeriodUnitSeparateCd.M == periodCd) {
            result.add(HistoryDataRes.builder()
                .date(DateUtil.format(fulfillDate, periodCd))
                .counterpart(PriceEntry.of(
                    isFuture ? PriceType.FUTURE : PriceType.ACTUAL,
                    PriceStat.ifPermittedOrNull(counterpartMap.get(DateUtil.format(fulfillDate, periodCd)), null, false, false, isCounterPartPermitted),
                    isCounterPartPermitted
                ))
                .build());
        }

        for (String date : dates) {

            // baseline = FORECAST
            PriceEntry baseline = null;
            BigDecimal fPrice = forecastMap.get(date);
            if (fPrice != null) {
                BigDecimal fChange = (prevForecast != null && prevForecast.signum() != 0) ? MathUtil.safeChangeRate(fPrice, prevForecast) : null;
                baseline = PriceEntry.of(
                    PriceType.FORECAST,
                    PriceStat.of(fPrice, fChange, fPrice.equals(maxForecast), fPrice.equals(minForecast)),
                    Boolean.TRUE
                );
                prevForecast = fPrice;
            }

            // counterpart = ACTUAL or FUTURE
            PriceEntry counterpart = null;
            BigDecimal cPrice = counterpartMap.get(date);
            if (cPrice != null) {
                BigDecimal cChange = (prevCounter != null && prevCounter.signum() != 0) ? MathUtil.safeChangeRate(cPrice, prevCounter) : null;
                counterpart = PriceEntry.of(
                    isFuture ? PriceType.FUTURE : PriceType.ACTUAL,
                    PriceStat.ifPermittedOrNull(cPrice, cChange, cPrice.equals(maxCounter), cPrice.equals(minCounter), isCounterPartPermitted),
                    isCounterPartPermitted
                );
                prevCounter = cPrice;
            }

            result.add(HistoryDataRes.builder()
                .date(date)
                .baseline(baseline)
                .counterpart(counterpart)
                .build());
        }
        return result;
    }

    public static List<HistoryDataRes> ofEmpty() {
        return List.of();
    }
}
