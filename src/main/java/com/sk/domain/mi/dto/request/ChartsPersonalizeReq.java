package com.sk.domain.mi.dto.request;

import com.sk.domain.shared.enums.chart.ChartSettingConditionCd;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.*;

public record ChartsPersonalizeReq(

    @Schema(
        description = "개인화 차트 조건 타입(RANGE, INTERVAL, INDICATOR, CHART, UNIT_CD, COMP_DATA_SOURCE)",
        example = "RANGE"
    )
    ChartSettingConditionCd chartConditionCd,

    @Schema(
        description = "개인화 차트 조건 값",
        example = "1W"
    )
    String chartConditionVal

) {

    public ChartsPersonalizeReq validateChartCondition() {
        if (this.chartConditionCd == ChartSettingConditionCd.UNIT_CD || this.chartConditionCd == ChartSettingConditionCd.COMP_DATA_SOURCE) {
            return this;
        } else if (this.chartConditionCd.getAllowedValues().contains(this.chartConditionVal)) {
            return this;
        } else {
            return null;
        }
    }

    public static List<ChartsPersonalizeReq> filterPersonalizeRequests(List<ChartsPersonalizeReq> dtoList) {

        if (dtoList == null || dtoList.isEmpty()) {
            return Collections.emptyList();
        }

        // Rule 1: 마지막 값만 유지 (RANGE, INTERVAL, CHART, UNIT_CD)
        Map<ChartSettingConditionCd, ChartsPersonalizeReq> singleValueItems = new LinkedHashMap<>();

        // Rule 2: 값 중복 제외하고 모두 유지 (INDICATOR, COMP_DATA_SOURCE)
        Map<ChartSettingConditionCd, Map<String, ChartsPersonalizeReq>> multiValueItems = new LinkedHashMap<>();

        for (ChartsPersonalizeReq req : dtoList) {

            ChartsPersonalizeReq validReq = req.validateChartCondition();

            if (validReq == null) {
                continue;
            }

            ChartSettingConditionCd cd = validReq.chartConditionCd();
            String val = validReq.chartConditionVal();

            switch (cd) {
                case RANGE, INTERVAL, CHART, UNIT_CD:
                    singleValueItems.put(cd, validReq);
                    break;
                case INDICATOR, COMP_DATA_SOURCE:
                    multiValueItems.computeIfAbsent(cd, k -> new LinkedHashMap<>()).put(val, validReq);
                    break;
                default:
                    break;
            }
        }

        List<ChartsPersonalizeReq> filteredList = new ArrayList<>(singleValueItems.values());

        for (Map<String, ChartsPersonalizeReq> valueMap : multiValueItems.values()) {
            filteredList.addAll(valueMap.values());
        }

        return filteredList;
    }

}
