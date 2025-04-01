package com.sk.domain.shared.repository.master.tmd;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sk.domain.admin.dto.response.provider.StructuredDataRes;
import com.sk.domain.shared.entity.master.tmd.GoodsDataSourceDetail;

public interface GoodsDataSourceDetailRepository extends JpaRepository<GoodsDataSourceDetail, Integer> {

    @Query(value = """
            SELECT new com.sk.domain.admin.dto.response.provider.StructuredDataRes(
            '',
            gdsd.goodsDataSourceNm,
            gdsd.createDt
            )
            FROM GoodsDataSourceDetail gdsd
            WHERE gdsd.isDeleted = false
            AND gdsd.goodsData.goodsDataNo = :goodsDataNo
            """)
    List<StructuredDataRes> findGDATS(@Param("goodsDataNo") Integer goodsDataNo);
}
