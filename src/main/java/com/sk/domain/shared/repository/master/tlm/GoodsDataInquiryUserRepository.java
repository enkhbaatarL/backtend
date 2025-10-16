package com.sk.domain.shared.repository.master.tlm;

import com.sk.domain.mi.dto.response.marketData.RecentViewedInf;
import com.sk.domain.shared.entity.compositeKey.tlm.GoodsDataInquiryUserId;
import com.sk.domain.shared.entity.master.tlm.GoodsDataInquiryUser;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface GoodsDataInquiryUserRepository extends
    JpaRepository<GoodsDataInquiryUser, GoodsDataInquiryUserId> {

    /**
     * 마켓 데이터 최근 조회 (기존) 최근 값만 조회가능. Paging 기능 없음. count 가 없음.
     *
     * @param companyNo
     * @param userNo
     * @param userLoginId
     * @param pageable
     * @return
     */
    @Query(
        value =
            """
                    WITH ranked AS (
                      SELECT gds_data_no, MAX(inqr_dtm) AS max_inqr
                      FROM tlm_gdsdatainqrusrl
                      WHERE usr_logn_id = :userLoginId
                                      AND del_yn = 'N'
                      GROUP BY gds_data_no
                      ORDER BY max_inqr DESC
                      LIMIT 100   -- 안전 버퍼(권한/데이터소스 EXISTS로 걸러질 것을 감안)
                    )
                    SELECT
                        p.prvd_no, p.prvd_nm,
                        g.gds_no,
                        gd.gds_data_no, gd.gds_data_nm, gd.gds_data_expn,
                        r.max_inqr
                    FROM ranked r
                    JOIN tmd_gdsdatam gd ON gd.gds_data_no = r.gds_data_no
                    JOIN tmd_gdsm g      ON g.gds_no      = gd.gds_no
                    JOIN tmd_prvdm p     ON p.prvd_no     = g.prvd_no
                    WHERE EXISTS (
                      SELECT 1
                      FROM tam_custcpnyprvdmappl ccpm
                      JOIN tmd_custcpnym cc     ON cc.cust_cpny_no = ccpm.cust_cpny_no
                      JOIN tam_custcpnygdsmappl c1 ON c1.cust_cpny_no = cc.cust_cpny_no
                      JOIN tam_custcpnygdsmappl c2 ON c2.cust_cpny_no = cc.cust_cpny_no
                                                  AND c2.gds_auth_mnagt_spt_cd = c1.gds_auth_mnagt_spt_cd
                                                  AND c2.gds_no = g.gds_no
                      JOIN tam_authmnagtgdsmappl a  ON a.cust_cpny_no = c2.cust_cpny_no
                                                  AND a.gds_no = c2.gds_no
                                                  AND a.gds_auth_mnagt_spt_cd = c2.gds_auth_mnagt_spt_cd
                      WHERE ccpm.cust_cpny_no = :companyNo
                        AND ccpm.use_yn = 'Y' AND ccpm.del_yn = 'N'
                        AND c1.use_yn = 'Y'  AND c1.del_yn = 'N'
                        AND a.gds_auth_cd = 'VIEW' AND a.del_yn = 'N'
                        AND a.use_bgn_dt <= DATE_FORMAT(CURDATE(), '%Y%m%d')
                        AND a.use_end_dt >= DATE_FORMAT(CURDATE(), '%Y%m%d')
                        AND (c1.gds_auth_mnagt_spt_cd='COMPANY' OR a.usr_no=:userNo)
                    )
                    AND EXISTS (
                      SELECT 1
                      FROM tmd_gdsdatasrcd d
                      WHERE d.gds_data_no = gd.gds_data_no
                        AND d.del_yn = 'N'
                        AND d.gds_data_use_mnagt_cd = 'backend'
                    )
                    ORDER BY r.max_inqr DESC
                """, nativeQuery = true)
    List<RecentViewedInf> findRecentViewdByUserLoginId(
        @Param("companyNo") int companyNo,
        @Param("userNo") int userNo,
        @Param("userLoginId") String userLoginId,
        Pageable pageable);

    /**
     * 인사이트 최근 조회
     *
     * @param companyNo   고객사 번호
     * @param userNo      사용자 번호
     * @param userLoginId 사용자 로그인 ID
     * @param pageable    페이징 정보
     * @return 인사이트 최근 조회 목록
     */
    @Query(
        value =
            """
                    WITH ranked AS (
                      SELECT gds_data_no, MAX(inqr_dtm) AS max_inqr
                      FROM tlm_gdsdatainqrusrl
                      WHERE usr_logn_id = :userLoginId
                        AND del_yn = 'N'
                      GROUP BY gds_data_no
                      ORDER BY max_inqr DESC
                      LIMIT 100
                    )
                    SELECT
                        p.prvd_no, p.prvd_nm,
                        g.gds_no,
                        gd.gds_data_no, gd.gds_data_nm, gd.gds_data_expn,
                        r.max_inqr
                    FROM ranked r
                    JOIN tmd_gdsdatam gd ON gd.gds_data_no = r.gds_data_no
                    JOIN tmd_gdsm g      ON g.gds_no = gd.gds_no
                    JOIN tmd_prvdm p     ON p.prvd_no = g.prvd_no
                    WHERE EXISTS (
                      SELECT 1
                      FROM tam_custcpnyprvdmappl ccpm
                      JOIN tmd_custcpnym cc     ON cc.cust_cpny_no = ccpm.cust_cpny_no
                      JOIN tam_custcpnygdsmappl c1 ON c1.cust_cpny_no = cc.cust_cpny_no
                      JOIN tam_custcpnygdsmappl c2 ON c2.cust_cpny_no = cc.cust_cpny_no
                                                  AND c2.gds_auth_mnagt_spt_cd = c1.gds_auth_mnagt_spt_cd
                                                  AND c2.gds_no = g.gds_no
                      JOIN tam_authmnagtgdsmappl a  ON a.cust_cpny_no = c2.cust_cpny_no
                                                  AND a.gds_no = c2.gds_no
                                                  AND a.gds_auth_mnagt_spt_cd = c2.gds_auth_mnagt_spt_cd
                      WHERE ccpm.cust_cpny_no = :companyNo
                        AND ccpm.use_yn = 'Y' AND ccpm.del_yn = 'N'
                        AND c1.use_yn = 'Y'  AND c1.del_yn = 'N'
                        AND a.gds_auth_cd = 'VIEW' AND a.del_yn = 'N'
                        AND a.use_bgn_dt <= DATE_FORMAT(CURDATE(), '%Y%m%d')
                        AND a.use_end_dt >= DATE_FORMAT(CURDATE(), '%Y%m%d')
                        AND (c1.gds_auth_mnagt_spt_cd='COMPANY' OR a.usr_no=:userNo)
                    )
                    AND EXISTS (
                      SELECT 1
                      FROM tcm_filel f
                      WHERE f.file_rel_obj_dstg_no = gd.gds_data_no
                        AND f.file_rel_obj_spt_cd = 'GUDAT'
                        AND f.del_yn = 'N'
                    )
                    ORDER BY r.max_inqr DESC
                """, nativeQuery = true)
    List<RecentViewedInf> findRecentViewedInsightsByUserLoginId(
        @Param("companyNo") int companyNo,
        @Param("userNo") int userNo,
        @Param("userLoginId") String userLoginId,
        Pageable pageable);

    /**
     * 데이터 카탈로그 최근 조회 삭제 (기존)
     */
    @Modifying
    @Query(value = """
        UPDATE tlm_gdsdatainqrusrl t
        SET t.del_yn = 'Y'
        WHERE t.usr_logn_id = :userLoginId
          AND t.del_yn = 'N'
          AND t.gds_data_no IN (
            SELECT DISTINCT r.gds_data_no
            FROM (
              SELECT gds_data_no
              FROM tlm_gdsdatainqrusrl
              WHERE usr_logn_id = :userLoginId
                AND del_yn = 'N'
            ) r
            JOIN tmd_gdsdatam gd ON gd.gds_data_no = r.gds_data_no
            JOIN tmd_gdsm g ON g.gds_no = gd.gds_no
            WHERE EXISTS (
              SELECT 1
              FROM tmd_gdsdatasrcd d
              WHERE d.gds_data_no = gd.gds_data_no
                AND d.del_yn = 'N'
                AND d.gds_data_use_mnagt_cd = 'backend'
            )
          )
        """, nativeQuery = true)
    void softDeleteRecentViewedByUserLoginId(
        @Param("companyNo") int companyNo,
        @Param("userNo") int userNo,
        @Param("userLoginId") String userLoginId);

    /**
     * 인사이트 최근 조회 삭제
     */
    @Modifying
    @Query(value = """
        UPDATE tlm_gdsdatainqrusrl t
        SET t.del_yn = 'Y'
        WHERE t.usr_logn_id = :userLoginId
          AND t.del_yn = 'N'
          AND t.gds_data_no IN (
            SELECT DISTINCT r.gds_data_no
            FROM (
              SELECT gds_data_no
              FROM tlm_gdsdatainqrusrl
              WHERE usr_logn_id = :userLoginId
                AND del_yn = 'N'
            ) r
            JOIN tmd_gdsdatam gd ON gd.gds_data_no = r.gds_data_no
            JOIN tmd_gdsm g ON g.gds_no = gd.gds_no
            WHERE EXISTS (
              SELECT 1
              FROM tcm_filel f
              WHERE f.file_rel_obj_dstg_no = gd.gds_data_no
                AND f.file_rel_obj_spt_cd = 'GUDAT'
                AND f.del_yn = 'N'
            )
          )
        """, nativeQuery = true)
    void softDeleteRecentViewedInsightsByUserLoginId(
        @Param("companyNo") int companyNo,
        @Param("userNo") int userNo,
        @Param("userLoginId") String userLoginId);
}
