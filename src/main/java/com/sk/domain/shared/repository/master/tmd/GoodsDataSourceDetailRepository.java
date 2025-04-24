package com.sk.domain.shared.repository.master.tmd;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.sk.domain.shared.entity.master.tcm.ComponentInfo;
import com.sk.domain.shared.entity.master.tmd.GoodsData;
import com.sk.domain.shared.entity.master.tmd.GoodsDataSourceDetail;

public interface GoodsDataSourceDetailRepository extends JpaRepository<GoodsDataSourceDetail, Integer> {
    /**
     * 
     * 기본적으로 고객사나 사용자의 사용가능 상태는 체크 하지 않는다. 로그인 단에서 체크하는걸로 처리
     *
     */

    @Query(value = """
            SELECT gdsd
            FROM GoodsDataSourceDetail gdsd
            WHERE gdsd.isDeleted = false
                AND gdsd.goodsDataUseManagementCd = 'backend'
                AND gdsd.goodsDataSourceNo = :goodsDataSourceNo
            """)
    Optional<GoodsDataSourceDetail> findByGoodsDataSourceNoAndIsDeletedFalse(Integer goodsDataSourceNo);

    @Query(value = """
            SELECT gdsd
            FROM GoodsDataSourceDetail gdsd
            WHERE gdsd.isDeleted = false
            AND gdsd.goodsDataUseManagementCd = 'backend'
            AND gdsd.goodsData = :goodsData
            """)
    List<GoodsDataSourceDetail> findByGoodsDataAndIsDeletedFalse(GoodsData goodsData);

    Optional<GoodsDataSourceDetail> findByGoodsDataAndComponentInfoAndIsDeletedFalse(GoodsData goodsData, ComponentInfo componentInfo);
}
