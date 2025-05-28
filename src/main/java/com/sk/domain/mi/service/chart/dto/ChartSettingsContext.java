package com.sk.domain.mi.service.chart.dto;

import com.sk.domain.shared.entity.master.tmd.UnitConvert;
import com.sk.domain.shared.enums.chart.ChartType;
import com.sk.domain.shared.enums.chart.Interval;
import com.sk.domain.shared.enums.chart.Range;
import lombok.With;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;
import java.util.Map;

/**
 * 차트 설정 컨텍스트 레코드.
 * 차트 생성에 필요한 설정 정보를 담고 있습니다.
 */
@With
public record ChartSettingsContext(
    Map<UnitCd, UnitConvert> unitCdMap,
    Interval finalizedInterval,
    Range finalizedRange,
    Pair<String, String> finalizedRangeBetween,
    UnitCd finalizedUnitCd,
    ChartType finalizedChartType,
    List<String> finalizedIndicators,
    List<Integer> finalizedCompIds
) {
}
