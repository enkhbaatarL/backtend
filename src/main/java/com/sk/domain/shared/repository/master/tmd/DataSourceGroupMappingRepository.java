package com.sk.domain.shared.repository.master.tmd;

import com.sk.domain.shared.entity.compositeKey.tmd.DataSourceGroupMappingId;
import com.sk.domain.shared.entity.master.tmd.DataSourceGroupMapping;
import com.sk.domain.shared.entity.master.tmd.GoodsDataSourceDetail;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DataSourceGroupMappingRepository extends JpaRepository<DataSourceGroupMapping, DataSourceGroupMappingId> {

    List<DataSourceGroupMapping> findByMainGoodsDataSourceDetailAndIsDeletedFalse(GoodsDataSourceDetail goodsDataSourceDetail);

}
