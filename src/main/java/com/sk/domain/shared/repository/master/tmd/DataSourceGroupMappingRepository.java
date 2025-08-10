package com.sk.domain.shared.repository.master.tmd;

import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.sk.domain.shared.entity.compositeKey.tmd.DataSourceGroupMappingId;
import com.sk.domain.shared.entity.master.tmd.DataSourceGroupMapping;
import com.sk.domain.shared.entity.master.tmd.GoodsDataSourceDetail;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DataSourceGroupMappingRepository extends JpaRepository<DataSourceGroupMapping, DataSourceGroupMappingId> {

    @Query("""
        SELECT dsgm FROM DataSourceGroupMapping dsgm
        JOIN FETCH dsgm.compareGoodsDataSourceDetail c_gdsd
        JOIN FETCH c_gdsd.goodsData c_gd
        JOIN FETCH c_gd.goods c_g
        WHERE dsgm.mainGoodsDataSourceDetail = :mainGds AND dsgm.isDeleted = false
        """)
    List<DataSourceGroupMapping> queryWithFullComparisonDetailsByMainDataSource(@Param("mainGds") GoodsDataSourceDetail mainGds);

    @EntityGraph(attributePaths = {"compareGoodsDataSourceDetail"})
    List<DataSourceGroupMapping> findByMainGoodsDataSourceDetailAndIsDeletedFalse(GoodsDataSourceDetail goodsDataSourceDetail);

}
