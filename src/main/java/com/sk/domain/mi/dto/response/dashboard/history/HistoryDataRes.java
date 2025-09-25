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
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Builder
public record HistoryDataRes(

    @Schema(description = "예측결과 일자 또는 달", example = "20250601")
    String date,
    @Schema(description = "예측 데이터")
    PriceEntry baseline,
    //TODO 바꿔야함.. 이러면 안됨....
    @Schema(description = "예측 데이터")
    PriceEntry baseline2,
    @Schema(description = "실제 또는 선물 데이터")
    PriceEntry counterpart,
    @Schema(description = "설명력 파일 존재 유무")
    Boolean download

) {

    public static List<HistoryDataRes> ofList(ChartPredictSetting setting,
        List<TargetPredictResultData> forecastResults1, List<TargetPredictResultData> forecastResults2,
        List<DoubleValueResultData> counterPartResults, Boolean isFuture, Boolean isCounterPartPermitted, LocalDate fulfillDate) {

        String suffix1 = null;
        String suffix2 = null;
        if(forecastResults2 != null){
            suffix1 = "M+1";
            suffix2 = "M+2";
        }

        PeriodUnitSeparateCd periodCd = setting.getScopePeriodUnitSeparateCd();

        Map<String, BigDecimal> forecastMap1 = forecastResults1.stream()
            .filter(r -> r != null && r.getPredictResultDate() != null && r.getPredictResultValue() != null)
            .collect(Collectors.groupingBy(
                r -> DateUtil.format(DateUtil.fromYmd(r.getPredictResultDate()), periodCd),
                Collectors.collectingAndThen(
                    Collectors.toList(),
                    r -> r.stream()
                        .map(TargetPredictResultData::getPredictResultValue)
                        .reduce(BigDecimal.ZERO, BigDecimal::add)
                        .divide(new BigDecimal(r.size()), 4, RoundingMode.HALF_UP)
                )
            ));
//            .collect(Collectors.toMap(
//                r -> DateUtil.format(DateUtil.fromYmd(r.getPredictResultDate()), periodCd),
//                TargetPredictResultData::getPredictResultValue,
//                (v1, v2) -> v2, // 중복 키는 뒤에 온 값으로
//                TreeMap::new
//            ));

        Map<String, BigDecimal> forecastMap2 = null;
        if(forecastResults2 != null){
            forecastMap2 = forecastResults2.stream()
                .filter(r -> r != null && r.getPredictResultDate() != null && r.getPredictResultValue() != null)
                .collect(Collectors.groupingBy(
                    r -> DateUtil.format(DateUtil.fromYmd(r.getPredictResultDate()), periodCd),
                    Collectors.collectingAndThen(
                        Collectors.toList(),
                        r -> r.stream()
                            .map(TargetPredictResultData::getPredictResultValue)
                            .reduce(BigDecimal.ZERO, BigDecimal::add)
                            .divide(new BigDecimal(r.size()), 4, RoundingMode.HALF_UP)
                    )
                ));
        }

        Map<String, BigDecimal> counterpartMap = counterPartResults.stream()
            .filter(r -> r != null && r.getDate() != null && r.getValue() != null)
            .collect(Collectors.groupingBy(
                r -> DateUtil.format(r.getDate().toLocalDate(), periodCd),
                Collectors.collectingAndThen(
                    Collectors.toList(),
                    r -> r.stream()
                        .map(DoubleValueResultData::getValue)
                        .reduce(BigDecimal.ZERO, BigDecimal::add)
                        .divide(new BigDecimal(r.size()), 4, RoundingMode.HALF_UP)
                )
            ));
//            .collect(Collectors.toMap(
//                r -> DateUtil.format(r.getDate().toLocalDate(), periodCd),
//                DoubleValueResultData::getValue,
//                (v1, v2) -> v2,
//                TreeMap::new
//            ));

        BigDecimal maxForecast1 = forecastMap1.values().stream().max(BigDecimal::compareTo).orElse(null);
        BigDecimal minForecast1 = forecastMap1.values().stream().min(BigDecimal::compareTo).orElse(null);
        BigDecimal maxForecast2 = null;
        BigDecimal minForecast2 = null;
        if(forecastMap2 != null){
            maxForecast2 = forecastMap1.values().stream().max(BigDecimal::compareTo).orElse(null);
            minForecast2 = forecastMap1.values().stream().min(BigDecimal::compareTo).orElse(null);
        }

        BigDecimal maxCounter  = counterpartMap.values().stream().max(BigDecimal::compareTo).orElse(null);
        BigDecimal minCounter  = counterpartMap.values().stream().min(BigDecimal::compareTo).orElse(null);

        SortedSet<String> dates = new TreeSet<>();
        dates.addAll(forecastMap1.keySet());
        if(forecastMap2 != null){
            dates.addAll(forecastMap2.keySet());
        }

//        dates.addAll(counterpartMap.keySet());

        List<HistoryDataRes> result = new ArrayList<>(dates.size());
        BigDecimal prevForecast1 = null;
        BigDecimal prevForecast2 = null;
        BigDecimal prevCounter  = null;

        // Monthly 예측값, 예측 시점 달 실제가 또는 선물가를 포함
//        if (PeriodUnitSeparateCd.M == periodCd) {
//            result.add(HistoryDataRes.builder()
//                .date(DateUtil.format(fulfillDate, periodCd))
//                .counterpart(PriceEntry.of(
//                    isFuture ? PriceType.FUTURE : PriceType.ACTUAL,
//                    PriceStat.ifPermittedOrNull(counterpartMap.get(DateUtil.format(fulfillDate, periodCd)), null, false, false, isCounterPartPermitted),
//                    isCounterPartPermitted
//                ))
//                .build());
//        }

        for (String date : dates) {

            // baseline = FORECAST
            PriceEntry baseline1 = null;
            PriceEntry baseline2 = null;
            BigDecimal fPrice1 = forecastMap1.get(date);
            if (fPrice1 != null) {
                BigDecimal fChange = (prevForecast1 != null && prevForecast1.signum() != 0) ? MathUtil.safeChangeRate(fPrice1, prevForecast1) : null;
                baseline1 = PriceEntry.of(
                    PriceType.FORECAST,
                    PriceStat.of(fPrice1, fChange, fPrice1.equals(maxForecast1), fPrice1.equals(minForecast1)),
                    Boolean.TRUE,
                    suffix1
                );
                prevForecast1 = fPrice1;
            }
            if(forecastMap2 != null){
                BigDecimal fPrice2 = forecastMap2.get(date);
                if (fPrice2 != null) {
                    BigDecimal fChange = (prevForecast2 != null && prevForecast2.signum() != 0) ? MathUtil.safeChangeRate(fPrice2, prevForecast2) : null;
                    baseline2 = PriceEntry.of(
                        PriceType.FORECAST,
                        PriceStat.of(fPrice2, fChange, fPrice2.equals(maxForecast2), fPrice2.equals(minForecast2)),
                        Boolean.TRUE,
                        suffix2
                    );
                    prevForecast2 = fPrice2;
                }
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
                .baseline(baseline1)
                .baseline2(baseline2)
                .counterpart(counterpart)
                .download(true)
                .build());
        }
        return result;
    }

    public static List<HistoryDataRes> ofEmpty() {
        return List.of();
    }
}
