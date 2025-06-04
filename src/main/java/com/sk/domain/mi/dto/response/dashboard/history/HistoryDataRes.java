package com.sk.domain.mi.dto.response.dashboard.history;

import com.sk.common.utils.DateUtil;
import com.sk.common.utils.MathUtil;
import com.sk.domain.shared.enums.PriceType;
import com.sk.domain.shared.mapper.dto.DoubleValueResultData;
import com.sk.domain.shared.mapper.dto.TargetPredictResultData;
import lombok.Builder;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Builder
public record HistoryDataRes(
    LocalDate date,
    PriceEntry baseline,
    PriceEntry counterpart
) {

    public static List<HistoryDataRes> ofList(List<TargetPredictResultData> forecastResults, List<DoubleValueResultData> counterPartResults, Boolean isFuture, Boolean isCounterPartPermitted) {

        Map<LocalDate, BigDecimal> forecastMap = forecastResults.stream()
            .collect(Collectors.toMap(r -> DateUtil.fromYmd(r.getPredictResultDate()), TargetPredictResultData::getPredictResultValue));

        Map<LocalDate, BigDecimal> counterpartMap = counterPartResults.stream()
            .collect(Collectors.toMap(DoubleValueResultData::getDate, DoubleValueResultData::getValue));

        BigDecimal maxForecast = forecastMap.values().stream().max(BigDecimal::compareTo).orElse(null);
        BigDecimal minForecast = forecastMap.values().stream().min(BigDecimal::compareTo).orElse(null);
        BigDecimal maxCounter  = counterpartMap.values().stream().max(BigDecimal::compareTo).orElse(null);
        BigDecimal minCounter  = counterpartMap.values().stream().min(BigDecimal::compareTo).orElse(null);

        SortedSet<LocalDate> dates = new TreeSet<>();
        dates.addAll(forecastMap.keySet());
        dates.addAll(counterpartMap.keySet());

        List<HistoryDataRes> result = new ArrayList<>(dates.size());
        BigDecimal prevForecast = null;
        BigDecimal prevCounter  = null;

        for (LocalDate date : dates) {

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
