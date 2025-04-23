package com.sk.domain.shared.repository.master.tcm;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sk.domain.admin.dto.response.provider.GoodsDataCategoryRes;
import com.sk.domain.shared.entity.compositeKey.tcm.CommonCodeDetailId;
import com.sk.domain.shared.entity.master.tcm.CommonCodeDetail;

public interface CommonCodeDetailRepository extends JpaRepository<CommonCodeDetail, CommonCodeDetailId> {

    // @Query(value = """
    // SELECT new com.sk.domain.admin.dto.response.provider.GoodsDataCategoryRes(
    // ccd.id.codeId,
    // ccd.codeNm,
    // ccd.codeExpn
    // )
    // FROM CommonCodeDetail ccd
    // WHERE ccd.isDeleted = false
    // AND ccd.isUsed = true
    // AND ccd.useBeginDt <= :#{T(java.time.LocalDate).now().format(T(java.time.format.DateTimeFormatter).ofPattern('yyyyMMdd'))}
    // AND ccd.useEndDt >= :#{T(java.time.LocalDate).now().format(T(java.time.format.DateTimeFormatter).ofPattern('yyyyMMdd'))}
    // AND ccd.id.codeGroupId = 'gds_data_ctgr_cd'
    // AND ccd.id.codeId IN (
    // SELECT sub.highCodeId
    // FROM CommonCodeDetail sub
    // WHERE sub.isDeleted = false
    // AND sub.id.codeGroupId = 'gds_data_ctgr_cd'
    // AND sub.highCodeId IS NOT NULL
    // GROUP BY sub.highCodeId)
    // ORDER BY ccd.codeRnk
    // """)
    // List<GoodsDataCategoryRes> findGoodsDataMainCategorys();

    // @Query(value = """
    // SELECT new com.sk.domain.admin.dto.response.provider.GoodsDataCategoryRes(
    // ccd.id.codeId,
    // ccd.codeNm,
    // ccd.codeExpn
    // )
    // FROM CommonCodeDetail ccd
    // WHERE ccd.isDeleted = false
    // AND ccd.isUsed = true
    // AND ccd.useBeginDt <= :#{T(java.time.LocalDate).now().format(T(java.time.format.DateTimeFormatter).ofPattern('yyyyMMdd'))}
    // AND ccd.useEndDt >= :#{T(java.time.LocalDate).now().format(T(java.time.format.DateTimeFormatter).ofPattern('yyyyMMdd'))}
    // AND ccd.id.codeGroupId = 'gds_data_ctgr_cd'
    // AND ccd.highCodeId = :codeId
    // ORDER BY ccd.codeRnk
    // """)
    // List<GoodsDataCategoryRes> findGoodsDataSubCategorysByCodeId(@Param("codeId") String codeId);

    @Query(value = """
            SELECT new com.sk.domain.admin.dto.response.provider.GoodsDataCategoryRes(
            ccd.id.codeId,
            ccd.codeNm,
            ccd.codeExpn
            )
            FROM CommonCodeDetail ccd
            WHERE ccd.isDeleted = false
            AND ccd.isUsed = true
            AND ccd.useBeginDt <= :#{T(java.time.LocalDate).now().format(T(java.time.format.DateTimeFormatter).ofPattern('yyyyMMdd'))}
            AND ccd.useEndDt >= :#{T(java.time.LocalDate).now().format(T(java.time.format.DateTimeFormatter).ofPattern('yyyyMMdd'))}
            AND ccd.id.codeGroupId = 'gds_data_ctgr_cd'
            AND ccd.highCodeId = 'A_TYPES'
            ORDER BY ccd.codeRnk
            """)
    List<GoodsDataCategoryRes> findDocTypeCategory();

    @Query(value = """
            SELECT new com.sk.domain.admin.dto.response.provider.GoodsDataCategoryRes(
            ccd.id.codeId,
            ccd.codeNm,
            ccd.codeExpn
            )
            FROM CommonCodeDetail ccd
            WHERE ccd.isDeleted = false
            AND ccd.isUsed = true
            AND ccd.useBeginDt <= :#{T(java.time.LocalDate).now().format(T(java.time.format.DateTimeFormatter).ofPattern('yyyyMMdd'))}
            AND ccd.useEndDt >= :#{T(java.time.LocalDate).now().format(T(java.time.format.DateTimeFormatter).ofPattern('yyyyMMdd'))}
            AND ccd.id.codeGroupId = 'gds_data_ctgr_cd'
            AND ccd.highCodeId = 'A_LANGUAGES'
            ORDER BY ccd.codeRnk
            """)
    List<GoodsDataCategoryRes> findLangTypeCategory();

    @Query(value = """
            SELECT MAX(ccd.codeRnk)
            FROM CommonCodeDetail ccd
            WHERE ccd.isDeleted = false
            AND ccd.isUsed = true
            AND ccd.useBeginDt <= :#{T(java.time.LocalDate).now().format(T(java.time.format.DateTimeFormatter).ofPattern('yyyyMMdd'))}
            AND ccd.useEndDt >= :#{T(java.time.LocalDate).now().format(T(java.time.format.DateTimeFormatter).ofPattern('yyyyMMdd'))}
            AND ccd.highCodeGroupId = :highCodeGroupId
            AND ccd.highCodeId = :highCodeId
            """)
    Integer findMaxCodeRnkByHighCodeId(@Param("highCodeGroupId") String highCodeGroupId, @Param("highCodeId") String highCodeId);
}
