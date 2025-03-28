package com.sk.domain.shared.repository.master.tcm;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sk.domain.admin.dto.response.provider.GoodsDataCatgoryRes;
import com.sk.domain.shared.entity.compositeKey.tcm.CommonCodeDetailId;
import com.sk.domain.shared.entity.master.tcm.CommonCodeDetail;

public interface CommonCodeDetailRepository extends JpaRepository<CommonCodeDetail, CommonCodeDetailId> {

    @Query(value = """
            SELECT new com.sk.domain.admin.dto.response.provider.GoodsDataCatgoryRes(
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
            AND ccd.id.codeId IN (
                SELECT sub.highCodeId
                FROM CommonCodeDetail sub
                WHERE sub.isDeleted = false
                AND sub.id.codeGroupId = 'gds_data_ctgr_cd'
                AND sub.highCodeId IS NOT NULL
                GROUP BY sub.highCodeId)
            ORDER BY ccd.codeRnk
            """)
    List<GoodsDataCatgoryRes> getGoodsDataMainCategorys();

    @Query(value = """
            SELECT new com.sk.domain.admin.dto.response.provider.GoodsDataCatgoryRes(
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
            AND ccd.highCodeId = :codeId
            ORDER BY ccd.codeRnk
            """)
    List<GoodsDataCatgoryRes> getGoodsDataSubCategorys(@Param("codeId") String codeId);
}
