package com.sk.domain.shared.repository.master.tmd;

import java.util.List;
import java.util.Optional;

import com.sk.domain.mi.dto.response.detail.GoodsDataCategoryProjection;
import jakarta.transaction.Transactional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
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
            WHERE gdc.goodsDataCategoryCd IN (SELECT ccd.id.codeId FROM CommonCodeDetail ccd WHERE ccd.id.codeGroupId = 'gds_data_ctgr_cd' AND ccd.highCodeGroupId = 'gds_data_ctgr_cd' AND (ccd.highCodeId = 'A_TYPES' OR ccd.highCodeId = 'B_THEMES'))
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

    @Query("""
            SELECT
                ccd.id.codeId as codeId,
                ccd.highCodeId as highCodeId,
                ccd.codeNm as codeNm
            FROM GoodsDataCategory gdc
            LEFT JOIN CommonCodeDetail ccd
                ON ccd.id.codeId = gdc.goodsDataCategoryCd
            WHERE gdc.isDeleted = false
                AND ccd.isDeleted = false
                AND ccd.id.codeGroupId = 'gds_data_ctgr_cd'
                AND gdc.goodsDataManagementSeparateCd = :goodsDataManagementSeparateCode
                AND gdc.goodsData.goodsDataNo = :goodsDataNo
            """)
    List<GoodsDataCategoryProjection> findGoodsDataAndIsDeleted(@Param("goodsDataNo") Integer goodsDataNo,
            @Param("goodsDataManagementSeparateCode") String goodsDataManagementSeparateCode);

    @Transactional
    @Modifying(clearAutomatically = true) // flush & 1차 캐시 동기화

    @Query("""
            UPDATE GoodsDataCategory gdc
            SET gdc.isDeleted = true
            WHERE gdc.goodsData.goodsDataNo = :goodsDataNo
            AND gdc.isDeleted = false
            AND gdc.goodsDataCategoryCd != 'PUBDATE'
            """)
    void softDeleteByGoodsDataNo(@Param("goodsDataNo") Integer goodsDataNo);

    @Query(value = """
            SELECT gdc
            FROM GoodsDataCategory gdc
            WHERE gdc.isDeleted = false
                AND gdc.goodsData.goodsDataNo = :goodsDataNo
            """)
    List<GoodsDataCategory> findGoodsDataCategoryByGoodsDataNo(@Param("goodsDataNo") Integer goodsDataNo);
}
