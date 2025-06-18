package com.sk.domain.mi.dto.response.marketData;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.ConstructorExpression;
import javax.annotation.processing.Generated;

/**
 * com.sk.domain.mi.dto.response.marketData.QMarketDataChartSettingRes is a Querydsl Projection type for MarketDataChartSettingRes
 */
@SuppressWarnings("this-escape")
@Generated("com.querydsl.codegen.DefaultProjectionSerializer")
public class QMarketDataChartSettingRes extends ConstructorExpression<MarketDataChartSettingRes> {

    private static final long serialVersionUID = 840992781L;

    public QMarketDataChartSettingRes(com.querydsl.core.types.Expression<Integer> marketDataSettingNo, com.querydsl.core.types.Expression<Short> chartMarkRank, com.querydsl.core.types.Expression<? extends com.sk.domain.shared.entity.master.tmd.GoodsDataSourceDetail> mainGoodsDataSource, com.querydsl.core.types.Expression<? extends com.sk.domain.shared.entity.master.tmd.GoodsData> mainGoodsData, com.querydsl.core.types.Expression<? extends com.sk.domain.shared.entity.master.tmd.GoodsDataSourceDetail> compareGoodsDataSource) {
        super(MarketDataChartSettingRes.class, new Class<?>[]{int.class, short.class, com.sk.domain.shared.entity.master.tmd.GoodsDataSourceDetail.class, com.sk.domain.shared.entity.master.tmd.GoodsData.class, com.sk.domain.shared.entity.master.tmd.GoodsDataSourceDetail.class}, marketDataSettingNo, chartMarkRank, mainGoodsDataSource, mainGoodsData, compareGoodsDataSource);
    }

}

