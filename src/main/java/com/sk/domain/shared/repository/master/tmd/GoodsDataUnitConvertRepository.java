package com.sk.domain.shared.repository.master.tmd;

import com.sk.domain.shared.entity.master.tmd.GoodsDataSourceDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import com.sk.domain.shared.entity.master.tmd.GoodsDataUnitConvert;

import java.util.List;

public interface GoodsDataUnitConvertRepository extends JpaRepository<GoodsDataUnitConvert, Integer> {

    List<GoodsDataUnitConvert> findById_GoodsDataSourceNoAndIsDeletedFalse(Integer goodsDataSourceNo);

    List<GoodsDataUnitConvert> findByGoodsDataSourceDetailAndIsDeletedFalse(GoodsDataSourceDetail goodsDataSourceDetail);

}
