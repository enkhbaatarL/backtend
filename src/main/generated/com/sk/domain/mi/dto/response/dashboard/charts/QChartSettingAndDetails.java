package com.sk.domain.mi.dto.response.dashboard.charts;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.ConstructorExpression;
import javax.annotation.processing.Generated;

/**
 * com.sk.domain.mi.dto.response.dashboard.charts.QChartSettingAndDetails is a Querydsl Projection type for ChartSettingAndDetails
 */
@SuppressWarnings("this-escape")
@Generated("com.querydsl.codegen.DefaultProjectionSerializer")
public class QChartSettingAndDetails extends ConstructorExpression<ChartSettingAndDetails> {

    private static final long serialVersionUID = 1450126639L;

    public QChartSettingAndDetails(com.querydsl.core.types.Expression<? extends com.sk.domain.shared.entity.master.tcm.ChartPredictSetting> chartPredictSetting, com.querydsl.core.types.Expression<? extends java.util.List<com.sk.domain.shared.entity.master.tcm.ChartPredictSettingDetail>> chartPredictSettingDetails) {
        super(ChartSettingAndDetails.class, new Class<?>[]{com.sk.domain.shared.entity.master.tcm.ChartPredictSetting.class, java.util.List.class}, chartPredictSetting, chartPredictSettingDetails);
    }

}

