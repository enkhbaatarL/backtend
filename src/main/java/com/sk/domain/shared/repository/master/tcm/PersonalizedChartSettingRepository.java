package com.sk.domain.shared.repository.master.tcm;

import com.sk.domain.shared.entity.master.tcm.PersonalizedChartSetting;
import com.sk.domain.shared.entity.master.tmd.GoodsDataSourceDetail;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PersonalizedChartSettingRepository extends JpaRepository<PersonalizedChartSetting, Integer> {

    List<PersonalizedChartSetting> findByGoodsDataSourceDetailAndUserLoginIdAndIsDeletedFalse(GoodsDataSourceDetail goodsDataSourceDetail, String userLoginId);

}
