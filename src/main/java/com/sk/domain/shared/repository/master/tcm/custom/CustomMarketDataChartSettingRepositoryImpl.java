package com.sk.domain.shared.repository.master.tcm.custom;

import static com.sk.domain.shared.entity.master.tcm.QMarketDataChartSetting.marketDataChartSetting;
import static com.sk.domain.shared.entity.master.tmd.QDataSourceGroupMapping.dataSourceGroupMapping;

import com.sk.domain.mi.dto.response.marketData.MarketDataChartSettingRes;
import com.sk.domain.mi.dto.response.marketData.QMarketDataChartSettingRes;

import java.util.List;

import com.querydsl.jpa.impl.JPAQueryFactory;
import com.sk.domain.shared.entity.master.tmd.CustomerCompany;
import com.sk.domain.shared.entity.master.tmd.QGoodsData;
import com.sk.domain.shared.entity.master.tmd.QGoodsDataSourceDetail;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class CustomMarketDataChartSettingRepositoryImpl implements CustomMarketDataChartSettingRepository {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<MarketDataChartSettingRes> findChartSettingsWithDetailsByCustomerCompany(CustomerCompany customerCompany) {

        QGoodsDataSourceDetail mainGoodsDataSource = new QGoodsDataSourceDetail("mainGoodsDataSource");
        QGoodsData mainGoodsData = new QGoodsData("mainGoodsData");
        QGoodsDataSourceDetail compareGoodsDataSource = new QGoodsDataSourceDetail("compareGoodsDataSource");

        return queryFactory
                .select(
                        new QMarketDataChartSettingRes(
                                marketDataChartSetting.marketDataSettingNo,
                                marketDataChartSetting.chartMarkRank,
                                mainGoodsDataSource,
                                mainGoodsData,
                                compareGoodsDataSource
                        )
                )
                .from(marketDataChartSetting)
                .innerJoin(marketDataChartSetting.goodsDataSourceDetail, mainGoodsDataSource)
                .innerJoin(mainGoodsDataSource.goodsData, mainGoodsData)
                .leftJoin(dataSourceGroupMapping)
                .on(dataSourceGroupMapping.mainGoodsDataSourceDetail.goodsDataSourceNo
                        .eq(mainGoodsDataSource.goodsDataSourceNo))
                .leftJoin(dataSourceGroupMapping.compareGoodsDataSourceDetail, compareGoodsDataSource)
                .where(
                        marketDataChartSetting.isDeleted.isFalse()
                                .and(marketDataChartSetting.customerCompany.eq(customerCompany))
                )
                .orderBy(marketDataChartSetting.chartMarkRank.asc())
                .fetch();
    }

}
