package com.sk.domain.shared.repository.master.tcm.custom;

import java.util.List;

import com.sk.domain.mi.dto.response.marketData.MarketDataChartSettingRes;
import com.sk.domain.shared.entity.master.tmd.CustomerCompany;

public interface CustomMarketDataChartSettingRepository {
    List<MarketDataChartSettingRes> findChartSettingsWithDetailsByCustomerCompany(CustomerCompany customerCompany);
}
