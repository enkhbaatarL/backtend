package com.sk.domain.shared.repository.master.tmd;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sk.domain.admin.dto.response.provider.StructuredDataRes;
import com.sk.domain.shared.entity.master.tmd.GoodsDataSourceDetail;

public interface GoodsDataSourceDetailRepository extends JpaRepository<GoodsDataSourceDetail, Integer> {

    @Query(value = """
            SELECT new com.sk.domain.admin.dto.response.provider.StructuredDataRes(
            gdc.goodsDataKeywordNm,
            gdsd.goodsDataSourceNm,
            gdsd.createDt
            )
            FROM GoodsDataSourceDetail gdsd
            LEFT JOIN GoodsDataCategory gdc ON gdc.goodsDataManagementNo = gdsd.goodsDataSourceNo
            WHERE gdsd.isDeleted = false
            AND gdsd.goodsData.goodsDataNo = :goodsDataNo
            """)
    List<StructuredDataRes> findStructuredDataByGoodsDataNo(@Param("goodsDataNo") Integer goodsDataNo);

    Optional<GoodsDataSourceDetail> findByGoodsDataSourceNoAndIsDeletedFalse(Integer goodsDataSourceNo);
}
