package com.sk.domain.shared.repository.master.tcm;

import com.sk.domain.admin.dto.response.provider.GoodsDataCategoryRes;
import com.sk.domain.mi.dto.response.insights.ProviderCategoryRes;
import com.sk.domain.shared.entity.compositeKey.tcm.CommonCodeDetailId;
import com.sk.domain.shared.entity.master.tcm.CommonCodeDetail;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CommonCodeDetailRepository extends JpaRepository<CommonCodeDetail, CommonCodeDetailId> {

    @Query(value = """
            SELECT new com.sk.domain.mi.dto.response.insights.ProviderCategoryRes(
            ccd.id.codeId,
            ccd.codeNm,
            ccd.codeExpn,
            ccpm.isDefaultUnstructured
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
                AND p.isUnstructuredDataOwned = true
                AND ccpm.isUsed = true
                AND ccpm.isDeleted = false
            ORDER BY ccd.codeRnk
            """)
    List<ProviderCategoryRes> findProviderTypeCategory(@Param("companyNo") int companyNo);

    @Query(value = """
            SELECT new com.sk.domain.mi.dto.response.insights.ProviderCategoryRes(
            ccd.id.codeId,
            ccd.codeNm,
            ccd.codeExpn,
            false
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
                AND EXISTS (
                    SELECT 1
                    FROM CustomerCompanyGoodsMapping ccgm
                    JOIN GoodsData gd ON gd.goods.goodsNo = ccgm.goods.goodsNo
                    JOIN GoodsDataSourceDetail gdsd ON gdsd.goodsData.goodsDataNo = gd.goodsDataNo
                    WHERE ccgm.customerCompany.customerCompanyNo = :companyNo
                        AND ccgm.goods.provider.providerNo = p.providerNo
                        AND ccgm.isUsed = true
                        AND ccgm.isDeleted = false
                        AND gd.isDeleted = false
                        AND gd.isGoodsDataMarked = true
                        AND gdsd.isDeleted = false
                        AND gdsd.goodsDataUseManagementCd = 'backend'
                )
            ORDER BY ccd.codeRnk
            """)
    List<ProviderCategoryRes> findMarketDataProviderTypeCategory(@Param("companyNo") int companyNo);

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

    /**
     * 코드그룹ID로 유효한 공통코드상세 목록 조회
     * - 삭제되지 않고 (isDeleted = false)
     * - 사용중이며 (isUsed = true)
     * - 현재 날짜가 사용기간 내에 있는 데이터
     *
     * @param codeGroupId 코드그룹ID
     * @return 공통코드상세 Entity 목록 (코드순위 정렬)
     */
    @Query(value = """
            SELECT ccd
            FROM CommonCodeDetail ccd
            WHERE ccd.isDeleted = false
                AND ccd.isUsed = true
                AND ccd.useBeginDt <= FUNCTION('DATE_FORMAT', CURRENT_DATE, '%Y%m%d')
                AND ccd.useEndDt >= FUNCTION('DATE_FORMAT', CURRENT_DATE, '%Y%m%d')
                AND ccd.id.codeGroupId = :codeGroupId
            ORDER BY ccd.codeRnk
            """)
    List<CommonCodeDetail> findValidCommonCodeDetails(@Param("codeGroupId") String codeGroupId);
}
