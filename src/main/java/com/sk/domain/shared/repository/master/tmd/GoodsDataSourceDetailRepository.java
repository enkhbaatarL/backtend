package com.sk.domain.shared.repository.master.tmd;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.sk.domain.shared.entity.master.tcm.ComponentInfo;
import com.sk.domain.shared.entity.master.tmd.GoodsData;
import com.sk.domain.shared.entity.master.tmd.GoodsDataSourceDetail;
import com.sk.domain.shared.repository.master.tmd.custom.CustomGoodsDataSourceDetailRepository;

public interface GoodsDataSourceDetailRepository extends JpaRepository<GoodsDataSourceDetail, Integer>, CustomGoodsDataSourceDetailRepository {
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
    Optional<GoodsDataSourceDetail> findGoodsDataSourceNoAndIsDeletedFalse(Integer goodsDataSourceNo);

    @EntityGraph(attributePaths = {"goodsData", "goodsData.goods"})
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

    @Query(value = """
            SELECT gdsd
            FROM GoodsDataSourceDetail gdsd
            JOIN FETCH gdsd.goodsData gd
            JOIN FETCH gd.goods g
            WHERE gdsd.isDeleted = false
            AND gdsd.goodsDataUseManagementCd = 'backend'
            AND gd.isDeleted = false
            AND g.isDeleted = false
            AND gdsd.goodsDataSourceNo = :goodsDataSourceNo
            AND gd.goodsDataNo = :goodsDataNo
            AND g.goodsNo = :goodsNo
            """)
    Optional<GoodsDataSourceDetail> findGoodsDataSourceWithGoodsTree(Integer goodsDataSourceNo, Integer goodsDataNo, Integer goodsNo);

}
