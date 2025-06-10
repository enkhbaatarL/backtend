package com.sk.domain.shared.repository.master.tcm.custom;

import static com.sk.domain.shared.entity.master.tcm.QChartPredictSetting.chartPredictSetting;
import static com.sk.domain.shared.entity.master.tcm.QChartPredictSettingDetail.chartPredictSettingDetail;
import static com.sk.domain.shared.entity.master.tmd.QGoodsDataSourceDetail.goodsDataSourceDetail;
import static com.sk.domain.shared.entity.master.tmd.QGoodsData.goodsData;
import static com.sk.domain.shared.entity.master.tmd.QGoods.goods;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.querydsl.core.Tuple;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.sk.domain.mi.dto.response.dashboard.charts.ChartSettingAndDetails;
import com.sk.domain.shared.entity.master.tcm.ChartPredictSetting;
import com.sk.domain.shared.entity.master.tcm.ChartPredictSettingDetail;
import com.sk.domain.shared.entity.master.tmd.CustomerCompany;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class CustomChartPredictSettingDetailRepositoryImpl implements CustomChartPredictSettingDetailRepository {

    private final JPAQueryFactory queryFactory;

    @Override
    public ChartSettingAndDetails findByCustomerCompany(CustomerCompany customerCompany) {

        // 차트 예측 설정 목록 조회
        List<Tuple> tuples = queryFactory
                .select(chartPredictSetting, chartPredictSettingDetail, goodsDataSourceDetail, goodsData, goods)
                .from(chartPredictSetting)
                .leftJoin(chartPredictSettingDetail)
                    .on(chartPredictSettingDetail.chartPredictSetting.eq(chartPredictSetting))
                    .fetchJoin()
                .leftJoin(chartPredictSettingDetail.goodsDataSourceDetail, goodsDataSourceDetail)
                    .fetchJoin()
                .leftJoin(goodsDataSourceDetail.goodsData, goodsData)
                    .fetchJoin()
                .leftJoin(goodsData.goods, goods)
                    .fetchJoin()
                .where(chartPredictSetting.customerCompany.eq(customerCompany)
                        .and(chartPredictSetting.isDeleted.isFalse())
                        .and(chartPredictSettingDetail.isDeleted.isFalse())
                        .and(goodsDataSourceDetail.isDeleted.isFalse())
                        .and(goodsData.isDeleted.isFalse())
                        .and(goods.isDeleted.isFalse()))
                .fetch();

        // 단일 ChartPredictSetting 추출
        Set<ChartPredictSetting> settings = tuples.stream()
                .map(tuple -> tuple.get(chartPredictSetting))
                .filter(setting -> setting != null)
                .collect(Collectors.toSet());

        if (settings.isEmpty()) {
            return null; // 또는 예외 처리
        }
        if (settings.size() > 1) {
            throw new IllegalStateException("Multiple ChartPredictSetting found for CustomerCompany: " + customerCompany.getCustomerCompanyNo());
        }

        ChartPredictSetting setting = settings.iterator().next();

        // ChartPredictSettingDetail 목록 수집
        List<ChartPredictSettingDetail> details = tuples.stream()
                .map(tuple -> tuple.get(chartPredictSettingDetail))
                .filter(detail -> detail != null)
                .collect(Collectors.toList());

        return ChartSettingAndDetails.builder()
                .chartPredictSetting(setting)
                .chartPredictSettingDetails(details)
                .build();
    }
}
