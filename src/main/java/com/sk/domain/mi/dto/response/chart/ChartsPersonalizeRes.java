package com.sk.domain.mi.dto.response.chart;

import com.sk.domain.shared.entity.master.tcm.PersonalizedChartSetting;
import com.sk.domain.shared.enums.chart.ChartSettingConditionCd;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.util.List;

public record ChartsPersonalizeRes(

    @Schema(
        description = "차트 개인화 번호",
        example = "1"
    )
    Integer personalizedSettingNo,

    @Schema(
        description = "차트 조건 타입(RANGE, INTERVAL, INDICATOR, CHART, UNIT, COMP_DATA_SOURCE)",
        example = "RANGE"
    )
    ChartSettingConditionCd chartSettingConditionCd,

    @Schema(
        description = "차트 조건 값",
        example = "1WEEK"
    )
    String chartConditionValue

) {


    @Builder
    public ChartsPersonalizeRes(Integer personalizedSettingNo, ChartSettingConditionCd chartSettingConditionCd, String chartConditionValue) {
        this.personalizedSettingNo = personalizedSettingNo;
        this.chartSettingConditionCd = chartSettingConditionCd;
        this.chartConditionValue = chartConditionValue;
    }

    public static ChartsPersonalizeRes of(PersonalizedChartSetting entity) {
        return ChartsPersonalizeRes.builder()
            .personalizedSettingNo(entity.getPersonalizedChartSettingNo())
            .chartSettingConditionCd(entity.getChartSettingConditionCd())
            .chartConditionValue(entity.getChartSettingConditionVal())
            .build();
    }

    public static List<ChartsPersonalizeRes> ofList(List<PersonalizedChartSetting> entities) {
        return entities.stream().map(ChartsPersonalizeRes::of).toList();
    }

}
