package com.sk.domain.shared.repository.master.tmd;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sk.domain.admin.dto.response.provider.CategoryDataRes;
import com.sk.domain.admin.dto.response.provider.StructuredDataRes;
import com.sk.domain.admin.service.UnStructuredDataRes;
import com.sk.domain.shared.entity.master.tmd.GoodsDataCategory;

public interface GoodsDataCategoryRepository extends JpaRepository<GoodsDataCategory, Integer> {

    // @Query(value = """
    // SELECT new com.sk.domain.admin.dto.response.provider.CategoryDataRes(
    // gdc.goodsDataCategoryCd
    // )
    // FROM GoodsDataCategory gdc
    // WHERE gdc.isDeleted = false
    // AND gdc.goodsDataManagementSeparateCd = 'GDAT'
    // AND gdc.goodsData.goodsDataNo = :goodsDataNo
    // """)
    // List<CategoryDataRes> findGDAT(@Param("goodsDataNo") Integer goodsDataNo);

    // @Query(value = """
    // SELECT new com.sk.domain.admin.dto.response.provider.StructuredDataRes(
    // '',
    // '',
    // 0
    // )
    // FROM GoodsDataCategory gdc
    // WHERE gdc.isDeleted = false
    // AND gdc.goodsDataManagementSeparateCd = 'GDATS'
    // AND gdc.goodsData.goodsDataNo = :goodsDataNo
    // """)
    // List<StructuredDataRes> findGDATS(@Param("goodsDataNo") Integer goodsDataNo);

    // @Query(value = """
    // SELECT new com.sk.domain.admin.dto.response.provider.UnStructuredDataRes(
    // '',
    // '',
    // 0
    // )
    // FROM GoodsDataCategory gdc
    // WHERE gdc.isDeleted = false
    // AND gdc.goodsDataManagementSeparateCd = 'GUDAT'
    // AND gdc.goodsData.goodsDataNo = :goodsDataNo
    // """)
    // List<UnStructuredDataRes> findGUDAT(@Param("goodsDataNo") Integer goodsDataNo);
}
