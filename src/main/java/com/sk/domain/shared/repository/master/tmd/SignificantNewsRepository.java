package com.sk.domain.shared.repository.master.tmd;

import com.sk.domain.shared.entity.master.tmd.SignificantNews;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SignificantNewsRepository extends JpaRepository<SignificantNews, Long> {

    Optional<SignificantNews> findByNwsSigId(Long newsSigId);

    /**
     * 날짜 범위 내 카테고리별 sentiment 개수 조회
     *
     * @param categoryCode 시장 카테고리 코드
     * @param startDate 시작일 (yyyyMMdd)
     * @param endDate 종료일 (yyyyMMdd)
     * @return [crtr_dt, upward_count, neutral_count, downward_count]
     */
    @Query(value = """
        SELECT
            sn.crtr_dt as date,
            SUM(CASE WHEN sn.scre >= 0.3 AND sn.scre <= 1.0 THEN 1 ELSE 0 END) as upward,
            SUM(CASE WHEN sn.scre > -0.3 AND sn.scre < 0.3 THEN 1 ELSE 0 END) as neutral,
            SUM(CASE WHEN sn.scre >= -1.0 AND sn.scre <= -0.3 THEN 1 ELSE 0 END) as downward
        FROM tmd_sgt_nws sn
        INNER JOIN tmd_nws_sig ns ON sn.nws_sig_id = ns.nws_sig_id
        WHERE ns.nws_mrkt_ctgr_cd = :categoryCode
          AND sn.del_yn = 'N'
          AND ns.del_yn = 'N'
          AND sn.crtr_dt BETWEEN :startDate AND :endDate
        GROUP BY sn.crtr_dt
        ORDER BY sn.crtr_dt DESC
        """, nativeQuery = true)
    List<Object[]> countSentimentsByDateRange(
        @Param("categoryCode") String categoryCode,
        @Param("startDate") String startDate,
        @Param("endDate") String endDate
    );
}
