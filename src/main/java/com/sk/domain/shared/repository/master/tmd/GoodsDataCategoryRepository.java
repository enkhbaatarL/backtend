package com.sk.domain.shared.repository.master.tmd;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sk.domain.admin.dto.response.provider.CategoryDataRes;
import com.sk.domain.admin.dto.response.provider.UnstructuredDataRes;
import com.sk.domain.shared.entity.master.tmd.GoodsDataCategory;

public interface GoodsDataCategoryRepository extends JpaRepository<GoodsDataCategory, Integer> {

    @Query(value = """
            SELECT gdc
            FROM GoodsDataCategory gdc
            WHERE gdc.isDeleted = false
            AND gdc.goodsData.goodsDataNo = :goodsDataNo
            AND gdc.goodsDataManagementSeparateCd = :goodsDataManagementSeparateCd
            AND gdc.goodsDataManagementNo = :goodsDataManagementNo
            """)
    Optional<GoodsDataCategory> getGoodsDataCategory(@Param("goodsDataNo") Integer goodsDataNo,
            @Param("goodsDataManagementSeparateCd") String goodsDataManagementSeparateCd,
            @Param("goodsDataManagementNo") Long goodsDataManagementNo);

    @Query(value = """
            SELECT new com.sk.domain.admin.dto.response.provider.CategoryDataRes(
            gdc.goodsDataCategoryCd
            )
            FROM GoodsDataCategory gdc
            WHERE gdc.isDeleted = false
            AND gdc.goodsDataManagementSeparateCd = 'GDAT'
            AND gdc.goodsData.goodsDataNo = :goodsDataNo
            """)
    List<CategoryDataRes> findGDAT(@Param("goodsDataNo") Integer goodsDataNo);

    @Query(value = """
            SELECT new com.sk.domain.admin.dto.response.provider.UnstructuredDataRes(
            gdc.goodsDataKeywordNm,
            uf.fileNo,
            uf.originalFileNm,
            uf.fileSize
            )
            FROM GoodsDataCategory gdc
            LEFT JOIN UploadedFile uf ON uf.fileNo = gdc.goodsDataManagementNo
            WHERE gdc.isDeleted = false
            AND gdc.goodsDataManagementSeparateCd = 'GUDAT'
            AND gdc.goodsData.goodsDataNo = :goodsDataNo
            """)
    List<UnstructuredDataRes> findGUDAT(@Param("goodsDataNo") Integer goodsDataNo);

    @Query(value = """
            SELECT gdc
            FROM GoodsDataCategory gdc
            WHERE gdc.isDeleted = false
            AND gdc.goodsDataManagementSeparateCd = 'GDAT'
            AND gdc.goodsData.goodsDataNo = :goodsDataNo
            """)
    Optional<GoodsDataCategory> findByCategoryCode(@Param("goodsDataNo") Integer goodsDataNo);
}
