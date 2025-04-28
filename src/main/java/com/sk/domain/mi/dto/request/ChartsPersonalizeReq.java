package com.sk.domain.mi.dto.request;

import com.sk.domain.shared.enums.chart.ChartSettingConditionCd;
import io.swagger.v3.oas.annotations.media.Schema;

public record ChartsPersonalizeReq(

    @Schema(
        description = "개인화 차트 조건 타입(RANGE, INTERVAL, INDICATOR, CHART, UNIT, COMP_DATA_SOURCE)",
        example = "RANGE"
    )
    ChartSettingConditionCd chartSettingConditionCd,

    @Schema(
        description = "개인화 차트 조건 값",
        example = "1WEEK"
    )
    String chartConditionValue

) {

    public ChartsPersonalizeReq validThen() {
        if (this.chartSettingConditionCd == ChartSettingConditionCd.UNIT || this.chartSettingConditionCd == ChartSettingConditionCd.COMP_DATA_SOURCE) {
            return this;
        } else if (this.chartSettingConditionCd.getAllowedValues().contains(this.chartConditionValue)) {
            return this;
        } else {
            return null;
        }
    }

}
