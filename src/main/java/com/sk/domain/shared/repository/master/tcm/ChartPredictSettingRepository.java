package com.sk.domain.shared.repository.master.tcm;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sk.domain.shared.entity.master.tcm.ChartPredictSetting;
import com.sk.domain.shared.entity.master.tmd.CustomerCompany;

public interface ChartPredictSettingRepository extends JpaRepository<ChartPredictSetting, Integer> {
    boolean existsByCustomerCompany(CustomerCompany customerCompany);
}
