package com.sk.domain.shared.repository.master.tcm;

import com.sk.domain.shared.entity.master.tcm.ChartPredictSetting;
import org.springframework.data.jpa.repository.JpaRepository;

import com.sk.domain.shared.entity.master.tcm.ChartPredictSettingDetail;
import com.sk.domain.shared.repository.master.tcm.custom.CustomChartPredictSettingDetailRepository;

import java.util.List;

public interface ChartPredictSettingDetailRepository extends JpaRepository<ChartPredictSettingDetail, Integer>, CustomChartPredictSettingDetailRepository {

    List<ChartPredictSettingDetail> findByChartPredictSettingAndIsDeletedFalse(ChartPredictSetting chartPredictSetting);

}
