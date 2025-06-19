package com.sk.domain.shared.repository.master.tcm;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sk.domain.admin.dto.response.provider.GoodsDataCategoryRes;
import com.sk.domain.shared.entity.compositeKey.tcm.CommonCodeDetailId;
import com.sk.domain.shared.entity.master.tcm.CommonCodeDetail;

public interface CommonCodeDetailRepository extends JpaRepository<CommonCodeDetail, CommonCodeDetailId> {

    @Query(value = """
            SELECT new com.sk.domain.admin.dto.response.provider.GoodsDataCategoryRes(
            ccd.id.codeId,
            ccd.codeNm,
            ccd.codeExpn
            )
            FROM CommonCodeDetail ccd
            JOIN Provider p ON CONCAT('A_', p.providerId) = ccd.id.codeId
            JOIN CustomerCompanyProviderMapping ccpm ON ccpm.provider.providerNo = p.providerNo
            WHERE ccd.isDeleted = false
                AND ccd.isUsed = true
                AND ccd.useBeginDt <= FUNCTION('DATE_FORMAT', CURRENT_DATE, '%Y%m%d')
                AND ccd.useEndDt >= FUNCTION('DATE_FORMAT', CURRENT_DATE, '%Y%m%d')
                AND ccd.id.codeGroupId = 'gds_data_ctgr_cd'
                AND ccd.highCodeId = 'A_PROVIDERS'
                AND ccpm.customerCompany.customerCompanyNo = :companyNo
                AND p.isUsed = true
                AND p.isDeleted = false
                AND ccpm.isUsed = true
                AND ccpm.isDeleted = false
            ORDER BY ccd.codeRnk
            """)
    List<GoodsDataCategoryRes> findProviderTypeCategory(@Param("companyNo") int companyNo);

    @Query(value = """
            SELECT new com.sk.domain.admin.dto.response.provider.GoodsDataCategoryRes(
            ccd.id.codeId,
            ccd.codeNm,
            ccd.codeExpn
            )
            FROM CommonCodeDetail ccd
            WHERE ccd.isDeleted = false
                AND ccd.isUsed = true
                AND ccd.useBeginDt <= FUNCTION('DATE_FORMAT', CURRENT_DATE, '%Y%m%d')
                AND ccd.useEndDt >= FUNCTION('DATE_FORMAT', CURRENT_DATE, '%Y%m%d')
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
                AND ccd.useBeginDt <= FUNCTION('DATE_FORMAT', CURRENT_DATE, '%Y%m%d')
                AND ccd.useEndDt >= FUNCTION('DATE_FORMAT', CURRENT_DATE, '%Y%m%d')
                AND ccd.id.codeGroupId = 'gds_data_ctgr_cd'
                AND ccd.highCodeId = 'B_THEMES'
            ORDER BY ccd.codeRnk
            """)
    List<GoodsDataCategoryRes> findThemeTypeCategory();

    @Query(value = """
            SELECT new com.sk.domain.admin.dto.response.provider.GoodsDataCategoryRes(
            ccd.id.codeId,
            ccd.codeNm,
            ccd.codeExpn
            )
            FROM CommonCodeDetail ccd
            WHERE ccd.isDeleted = false
                AND ccd.isUsed = true
                AND ccd.useBeginDt <= FUNCTION('DATE_FORMAT', CURRENT_DATE, '%Y%m%d')
                AND ccd.useEndDt >= FUNCTION('DATE_FORMAT', CURRENT_DATE, '%Y%m%d')
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
                AND ccd.useBeginDt <= FUNCTION('DATE_FORMAT', CURRENT_DATE, '%Y%m%d')
                AND ccd.useEndDt >= FUNCTION('DATE_FORMAT', CURRENT_DATE, '%Y%m%d')
                AND ccd.highCodeGroupId = :highCodeGroupId
                AND ccd.highCodeId = :highCodeId
            """)
    Integer findMaxCodeRnkByHighCodeId(@Param("highCodeGroupId") String highCodeGroupId, @Param("highCodeId") String highCodeId);
}
