package com.sk.domain.shared.repository.master.tcm;

import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.sk.domain.shared.entity.master.tcm.MarketDataChartSetting;
import com.sk.domain.shared.entity.master.tmd.CustomerCompany;

public interface MarketDataChartSettingRepository extends JpaRepository<MarketDataChartSetting, Integer> {

    @EntityGraph(attributePaths = {"goodsDataSourceDetail", "goodsDataSourceDetail.goodsData"})
    List<MarketDataChartSetting> findByCustomerCompanyAndIsDeletedFalseOrderByChartMarkRankAsc(
            CustomerCompany customerCompany);
}
