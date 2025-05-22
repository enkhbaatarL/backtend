package com.sk.domain.shared.repository.master.tcm.custom;

import java.util.List;

import com.sk.domain.shared.entity.master.tcm.ChartPredictSettingDetail;
import com.sk.domain.shared.entity.master.tmd.CustomerCompany;

public interface CustomChartPredictSettingDetailRepository {

    List<ChartPredictSettingDetail> findByCustomerCompany(CustomerCompany customerCompany);
    
}
