package com.sk.domain.shared.repository.master.tmd.custom;

import java.util.List;

import com.sk.domain.shared.entity.master.tmd.GoodsDataSourceDetail;
import com.sk.domain.shared.entity.master.tmd.UnitConvert;

public interface CustomUnitConvertRepository {
    List<UnitConvert> findUnitConvertsByConditions(GoodsDataSourceDetail goodsDataSourceDetail);
}
