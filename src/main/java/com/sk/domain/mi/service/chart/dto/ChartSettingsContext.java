package com.sk.domain.mi.service.chart.dto;

import com.sk.domain.shared.entity.master.tmd.UnitConvert;
import com.sk.domain.shared.enums.chart.ChartType;
import com.sk.domain.shared.enums.chart.Interval;
import com.sk.domain.shared.enums.chart.Range;
import lombok.Builder;
import lombok.With;
import org.apache.commons.lang3.tuple.Pair;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

/**
 * 차트 설정 컨텍스트 레코드.
 * 차트 생성에 필요한 설정 정보를 담고 있습니다.
 */
@With
@Builder
public record ChartSettingsContext(

    // 요청 시점의 값들
    Interval requestedInterval,
    Range requestedRange,
    LocalDate requestedStartDateStr,
    LocalDate requestedEndDateStr,
    String requestedUnitCdStr,

    // 최종 결정/조정된 값들
    Interval finalizedInterval,
    Range finalizedRange,
    // inclusive
    LocalDate finalizedStartDate,
    // exclusive
    LocalDate finalizedEndDate,
    Pair<String, String> finalizedRangeBetween,
    UnitCd finalizedUnitCd,
    UnitConvert finalizedUnitConvert,
    ChartType finalizedChartType,
    List<String> finalizedIndicators,
    List<Integer> finalizedCompIds,
    Set<UnitCd> finalizedAllowedUnitCds

) {

    public static ChartSettingsContext ofEmpty() {
        return ChartSettingsContext.builder().build();
    }

}
