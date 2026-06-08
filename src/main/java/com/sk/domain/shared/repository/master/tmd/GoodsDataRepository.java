package com.sk.domain.shared.repository.master.tmd;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sk.domain.admin.dto.response.provider.*;
import com.sk.domain.mi.dto.response.dashboard.LatestInsightsDto;
import com.sk.domain.mi.dto.response.insights.FocusedDataDto;
import com.sk.domain.shared.entity.master.tmd.GoodsData;

public interface GoodsDataRepository extends JpaRepository<GoodsData, Integer> {
    /**
     *
     * 기본적으로 고객사나 사용자의 사용가능 상태는 체크 하지 않는다. 로그인 단에서 체크하는걸로 처리
     *
     */

    @Query(value = """
            SELECT new com.sk.domain.admin.dto.response.provider.GoodsDataRes(
                cc.customerCompanyNm,
                gd.goodsDataNo,
                gd.goodsDataNm,
                gd.goodsDataExpn,
                gd.publishedDate
            )
            FROM GoodsData gd
                LEFT JOIN gd.customerCompany cc
            WHERE gd.isDeleted = false
                AND gd.goods.provider.providerNo = :providerNo
                AND gd.goods.goodsNo = :goodsNo
                AND (:keyword IS NULL OR (LOWER(gd.goodsDataNm) LIKE CONCAT('%', LOWER(:keyword), '%')
                  OR LOWER(gd.goodsDataExpn) LIKE CONCAT('%', LOWER(:keyword), '%')))
                AND (:startDate IS NULL OR gd.publishedDate >= :startDate)
                AND (:endDate IS NULL OR gd.publishedDate <= :endDate)
            """, countQuery = """
            SELECT COUNT(DISTINCT gd.goodsDataNo)
            FROM GoodsData gd
                LEFT JOIN gd.customerCompany cc
            WHERE gd.isDeleted = false
                AND gd.goods.provider.providerNo = :providerNo
                AND gd.goods.goodsNo = :goodsNo
                AND (:keyword IS NULL OR (LOWER(gd.goodsDataNm) LIKE CONCAT('%', LOWER(:keyword), '%')
                  OR LOWER(gd.goodsDataExpn) LIKE CONCAT('%', LOWER(:keyword), '%')))
                AND (:startDate IS NULL OR gd.publishedDate >= :startDate)
                AND (:endDate IS NULL OR gd.publishedDate <= :endDate)
            """)
    Page<GoodsDataRes> findGoodsDataByProviderNoAndGoodsNo(@Param("providerNo") int providerNo, @Param("goodsNo") int goodsNo,
            @Param("keyword") String keyword, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate, Pageable pageable);

    @Query(value = """
            SELECT new com.sk.domain.admin.dto.response.provider.GoodsDataNameRes(
                gd.goodsDataNo,
                gd.goodsDataNm
            )
            FROM GoodsData gd
            WHERE gd.isDeleted = false
                AND gd.goods.provider.providerNo = :providerNo
                AND gd.goods.goodsNo = :goodsNo
            ORDER BY gd.goodsDataNm ASC
            """, countQuery = """
            SELECT COUNT(gd.goodsDataNo)
            FROM GoodsData gd
            WHERE gd.isDeleted = false
                AND gd.goods.provider.providerNo = :providerNo
                AND gd.goods.goodsNo = :goodsNo
                        """)
    List<GoodsDataNameRes> findGoodsDataNamesByProviderNoAndGoodsNo(@Param("providerNo") int providerNo, @Param("goodsNo") int goodsNo);

    @Query(value = """
            SELECT gd
            FROM GoodsData gd
            WHERE gd.isDeleted=false
                AND gd.goods.provider.providerNo=:providerNo
                AND gd.goods.provider.isDeleted=false
                AND gd.goods.goodsNo=:goodsNo
                AND gd.goods.isDeleted=false
                AND gd.goodsDataNo=:goodsDataNo
                """)
    Optional<GoodsData> findGoodsDataByProviderNoAndGoodsNoAndGoodsDataNo(@Param("providerNo") int providerNo, @Param("goodsNo") int goodsNo,
            @Param("goodsDataNo") int goodsDataNo);

    @EntityGraph(attributePaths = {"goods", "goods.provider"})
    Page<GoodsData> findAll(Pageable pageable);

    @EntityGraph(attributePaths = {"goods", "goods.provider"})
    Page<GoodsData> findByGoods_GoodsNoAndIsDeletedFalse(Integer goodsNo, Pageable pageable);

    @EntityGraph(attributePaths = {"goods", "goods.provider"})
    Optional<GoodsData> findByGoodsDataNoAndIsDeletedFalse(@Param("goodsDataNo") int goodsDataNo);

    @Query(value = """
            SELECT
                    tp.prvd_no AS providerNo,
                    tp.prvd_nm AS providerNm,
                    tg.gds_no AS goodsNo,
                    tg.gds_nm AS goodsNm,
                    tgd.gds_data_no AS goodsDataNo,
                    tgd.gds_data_nm AS goodsDataNm,
                    tgd.gds_data_expn AS goodsDataExpn,
                    tgd.gds_data_smry_cntt AS goodsDataSummaryCntt,
                    max(tgc.gds_data_keywd_nm) AS modifyDtStr,
                    GROUP_CONCAT(tgdc.gds_data_ctgr_cd) as goodsDataCategoryCd
                FROM midb.tam_custcpnyprvdmappl tcp
                JOIN midb.tam_custcpnygdsmappl tcg ON tcg.cust_cpny_no = tcp.cust_cpny_no AND tcg.del_yn = 'N' AND tcg.use_yn = 'Y'
                JOIN midb.tmd_prvdm tp ON tp.prvd_no = tcp.prvd_no AND tp.del_yn = 'N' AND tp.use_yn = 'Y' -- AND tp.prvd_no not in :exceptProviderNoList
                JOIN midb.tmd_gdsm tg ON tg.gds_no = tcg.gds_no AND tg.prvd_no = tcp.prvd_no AND tg.del_yn = 'N' AND tg.use_yn = 'Y'
                JOIN midb.tmd_gdsdatam tgd ON tgd.gds_no = tcg.gds_no AND tgd.gds_no = tcg.gds_no AND tgd.del_yn = 'N'
                JOIN midb.tmd_gdsdatactgrl tgdc ON tgdc.gds_data_no = tgd.gds_data_no AND tgdc.del_yn = 'N' AND tgdc.gds_data_mnagt_spt_cd = 'GDAT'
                JOIN midb.tmd_gdsdatactgrl as tgc ON tgc.gds_data_no = tgd.gds_data_no AND tgc.del_yn = 'N' AND tgc.gds_data_ctgr_cd = 'PUBDATE'
                WHERE tcp.del_yn = 'N'
                    AND tcp.use_yn = 'Y'
                    AND tcp.cust_cpny_no = :customerCompanyNo
                    AND tcp.defl_ustrc_yn = 'Y'
                    AND tgc.gds_data_keywd_nm < now()
                    AND tgc.gds_data_keywd_nm > DATE_SUB(now(), INTERVAL :threshold DAY)
                    -- AND MATCH(tgd.gds_data_nm) AGAINST(lower('LNG Oil Gas Upstream') IN BOOLEAN MODE)
                    AND EXISTS (SELECT 1
                                FROM midb.tcm_filel tf
                                WHERE tf.del_yn = 'N'
                                    AND tf.file_rel_obj_spt_cd = 'GUDAT'
                                    AND tf.file_rel_obj_dstg_no = tgd.gds_data_no)
                GROUP BY tp.prvd_no, tg.gds_no, tgd.gds_data_no
                ORDER BY tgc.gds_data_keywd_nm DESC
                        """, nativeQuery = true)
    List<FocusedDataDto> findFocusedData(@Param("customerCompanyNo") int customerCompanyNo, @Param("threshold") int threshold /*, @Param("exceptProviderNoList") List<Integer> exceptProviderNoList*/);

    Optional<GoodsData> findByGoodsDataNoAndGoods_GoodsNoAndIsDeletedFalse(Integer goodsDataNo, Integer goodsNo);

    @Query(value = """
            SELECT
                    tp.prvd_no AS providerNo,
                    tp.prvd_nm AS providerNm,
                    tg.gds_no AS goodsNo,
                    tg.gds_nm AS goodsNm,
                    tgd.gds_data_no AS goodsDataNo,
                    tgd.gds_data_nm AS goodsDataNm,
                    tgd.mod_dtm AS modifyDt
                FROM midb.tam_custcpnyprvdmappl tcp
                JOIN midb.tam_custcpnygdsmappl tcg ON tcg.cust_cpny_no = tcp.cust_cpny_no AND tcg.del_yn = 'N' AND tcg.use_yn = 'Y'
                JOIN midb.tmd_prvdm tp ON tp.prvd_no = tcp.prvd_no AND tp.del_yn = 'N' AND tp.use_yn = 'Y' AND tp.prvd_no not in :exceptProviderNoList
                JOIN midb.tmd_gdsm tg ON tg.gds_no = tcg.gds_no AND tg.prvd_no = tcp.prvd_no AND tg.del_yn = 'N' AND tg.use_yn = 'Y'
                JOIN midb.tmd_gdsdatam tgd ON tgd.gds_no = tcg.gds_no AND tgd.gds_no = tcg.gds_no AND tgd.del_yn = 'N'
                JOIN midb.tmd_gdsdatactgrl tgc ON tgc.gds_data_no = tgd.gds_data_no AND tgc.del_yn = 'N' AND tgc.gds_data_ctgr_cd = 'PUBDATE'
                WHERE
                    tcp.del_yn = 'N'
                    AND tcp.use_yn = 'Y'
                    AND tcp.cust_cpny_no = :customerCompanyNo
                    AND EXISTS (SELECT 1
                                FROM midb.tcm_filel tf
                                WHERE tf.del_yn = 'N'
                                    AND tf.file_rel_obj_spt_cd = 'GUDAT'
                                    AND tf.file_rel_obj_dstg_no = tgd.gds_data_no)
                    AND tgc.gds_data_keywd_nm <= DATE_FORMAT(CURDATE(), '%Y%m%d')
                GROUP BY tp.prvd_no, tg.gds_no, tgd.gds_data_no
                ORDER BY tgd.mod_dtm DESC
                        """, nativeQuery = true)
    List<LatestInsightsDto> findLatestInsights(@Param("customerCompanyNo") int customerCompanyNo, @Param("exceptProviderNoList") List<Integer> exceptProviderNoList, Pageable pageable);

    @EntityGraph(attributePaths = {"goods", "customerCompany"})
    @Query(value = """
            SELECT gd
            FROM GoodsData gd
            WHERE gd.isDeleted = false
                AND gd.goodsDataNm = :goodsDataNm
            """)
    List<GoodsData> findByGoodsDataNm(String goodsDataNm);

    List<GoodsData> findByGoodsDataDistinguishIdInAndIsDeletedFalse(List<String> distinguishIds);

    List<GoodsData> findByGoodsDataNoInAndIsDeletedFalse(List<Integer> goodsDataNos);

    /**
     * VDB의 product_num(gds_no) + document_id(gds_data_dstg_id) 조합으로 GoodsData를 조회합니다.
     */
    @EntityGraph(attributePaths = {"goods"})
    List<GoodsData> findByGoods_GoodsNoAndGoodsDataDistinguishIdInAndIsDeletedFalse(
        @Param("goodsNo") Integer goodsNo,
        @Param("distinguishIds") List<String> distinguishIds);

    @Query(value = """
            SELECT gd
            FROM GoodsData gd
            WHERE gd.isDeleted = false
                AND gd.goods = :goods
                AND gd.goodsDataNm = :goodsDataNm
                AND (gd.goodsDataExpn IS NULL OR gd.goodsDataExpn = :goodsDataExpn)
                AND (gd.goodsDataSummaryCntt IS NULL OR gd.goodsDataSummaryCntt = :goodsDataSummaryCntt)
            ORDER BY gd.modifyDt DESC
            """)
    List<GoodsData> findByGoodsDataNmAndGodsDataExpnAndGoodsDataSummaryCntt(@Param("goods") com.sk.domain.shared.entity.master.tmd.Goods goods, @Param("goodsDataNm") String goodsDataNm,
            @Param("goodsDataExpn") String goodsDataExpn, @Param("goodsDataSummaryCntt") String goodsDataSummaryCntt, Pageable pageable);

    @Query("""
            SELECT gd.goods.goodsNo
            FROM GoodsData gd
            WHERE gd.goodsDataNo = :goodsDataNo
                AND gd.isDeleted = false
            """)
    Optional<Integer> queryGoodsNoByGoodsDataNo(@Param("goodsDataNo") Integer goodsDataNo);

}
