package com.sk.domain.shared.repository.master.tmd;

import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.sk.domain.shared.entity.compositeKey.tmd.DataSourceGroupMappingId;
import com.sk.domain.shared.entity.master.tmd.DataSourceGroupMapping;
import com.sk.domain.shared.entity.master.tmd.GoodsDataSourceDetail;

public interface DataSourceGroupMappingRepository extends JpaRepository<DataSourceGroupMapping, DataSourceGroupMappingId> {
    @EntityGraph(attributePaths = {"compareGoodsDataSourceDetail"})
    List<DataSourceGroupMapping> findByMainGoodsDataSourceDetailAndIsDeletedFalse(GoodsDataSourceDetail goodsDataSourceDetail);
}
