package com.sk.domain.shared.repository.master.tcm.custom;

import com.sk.domain.mi.dto.response.dashboard.charts.ChartSettingAndDetails;
import com.sk.domain.shared.entity.master.tmd.CustomerCompany;

public interface CustomChartPredictSettingDetailRepository {

    ChartSettingAndDetails findByCustomerCompany(CustomerCompany customerCompany);

}
