package com.sk.domain.shared.repository.master.tmd;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sk.domain.admin.dto.response.provider.CategoryDataRes;
import com.sk.domain.admin.dto.response.provider.StructuredDataRes;
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
    Optional<GoodsDataCategory> findGoodsDataCategoryByGoodsDataNo(@Param("goodsDataNo") Integer goodsDataNo,
            @Param("goodsDataManagementSeparateCd") String goodsDataManagementSeparateCd,
            @Param("goodsDataManagementNo") Long goodsDataManagementNo);

    @Query(value = """
            SELECT new com.sk.domain.admin.dto.response.provider.CategoryDataRes(
            gdc.goodsDataCategoryCd,
            ccd.codeNm
            )
            FROM GoodsDataCategory gdc
            LEFT JOIN CommonCodeDetail ccd ON ccd.id.codeId = gdc.goodsDataCategoryCd
            WHERE gdc.goodsDataCategoryCd IN (SELECT ccd.id.codeId FROM CommonCodeDetail ccd WHERE ccd.id.codeGroupId = 'gds_data_ctgr_cd' AND ccd.highCodeGroupId = 'gds_data_ctgr_cd' AND ccd.highCodeId = 'A_TYPES')
                AND gdc.isDeleted = false
                AND gdc.goodsDataManagementSeparateCd = 'GDAT'
                AND gdc.goodsData.goodsDataNo = :goodsDataNo
                AND ccd.id.codeGroupId = 'gds_data_ctgr_cd'
            """)
    List<CategoryDataRes> findDocCategoryDataByGoodsDataNo(@Param("goodsDataNo") Integer goodsDataNo);

    @Query(value = """
            SELECT new com.sk.domain.admin.dto.response.provider.CategoryDataRes(
            gdc.goodsDataCategoryCd,
            ccd.codeNm
            )
            FROM GoodsDataCategory gdc
            LEFT JOIN CommonCodeDetail ccd ON ccd.id.codeId = gdc.goodsDataCategoryCd
            WHERE gdc.goodsDataCategoryCd IN (SELECT ccd.id.codeId FROM CommonCodeDetail ccd WHERE ccd.id.codeGroupId = 'gds_data_ctgr_cd' AND ccd.highCodeGroupId = 'gds_data_ctgr_cd' AND ccd.highCodeId = 'A_LANGUAGES')
                AND gdc.isDeleted = false
                AND gdc.goodsDataManagementSeparateCd = 'GDAT'
                AND gdc.goodsData.goodsDataNo = :goodsDataNo
                AND ccd.id.codeGroupId = 'gds_data_ctgr_cd'
            """)
    List<CategoryDataRes> findLangCategoryDataByGoodsDataNo(@Param("goodsDataNo") Integer goodsDataNo);

    @Query(value = """
            SELECT new com.sk.domain.admin.dto.response.provider.StructuredDataRes(
            ccd.codeNm,
            gdc.createDt
            )
            FROM GoodsDataCategory gdc
            LEFT JOIN CommonCodeDetail ccd ON ccd.id.codeId = gdc.goodsDataCategoryCd
            WHERE gdc.isDeleted = false
                AND gdc.goodsDataManagementSeparateCd = 'GDATS'
                AND gdc.goodsData.goodsDataNo = :goodsDataNo
            """)
    List<StructuredDataRes> findStructuredDataByGoodsDataNo(@Param("goodsDataNo") Integer goodsDataNo);

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
                AND gdc.goodsDataCategoryCd = 'PUBDATE'
                AND gdc.goodsDataManagementSeparateCd = 'GUDAT'
                AND gdc.goodsData.goodsDataNo = :goodsDataNo
            """)
    List<UnstructuredDataRes> findUnstructuredDataByGoodsDataNo(@Param("goodsDataNo") Integer goodsDataNo);

    @Query(value = """
            SELECT gdc
            FROM GoodsDataCategory gdc
            WHERE gdc.isDeleted = false
                AND gdc.goodsDataManagementSeparateCd = 'GDAT'
                AND gdc.goodsData.goodsDataNo = :goodsDataNo
                AND gdc.goodsDataManagementSeparateCd = :codeId
            """)
    Optional<GoodsDataCategory> findByCategoryCode(@Param("goodsDataNo") Integer goodsDataNo, @Param("codeId") String codeId);
}
