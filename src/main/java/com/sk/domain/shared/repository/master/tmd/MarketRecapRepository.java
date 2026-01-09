package com.sk.domain.shared.repository.master.tmd;

import com.sk.domain.shared.entity.master.tmd.MarketRecap;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MarketRecapRepository extends JpaRepository<MarketRecap, Long> {

    Optional<MarketRecap> findByCrtrDtAndNwsMrktCtgrCd(String crtrDt, String nwsMrktCtgrCd);

    /**
     * 마켓 리캡 목록 조회 (날짜 범위, 키워드 검색, 페이징)
     *
     * @param startDate 시작일 (yyyyMMdd)
     * @param endDate 종료일 (yyyyMMdd)
     * @param keyword 검색 키워드
     * @param pageable 페이징 정보
     * @return 마켓 리캡 페이지
     */
    @Query(value = """
        SELECT mr
        FROM MarketRecap mr
        WHERE (:startDate IS NULL OR mr.crtrDt >= :startDate)
          AND (:endDate IS NULL OR mr.crtrDt <= :endDate)
          AND (:keyword IS NULL OR :keyword = ''
               OR mr.ckptTitl LIKE %:keyword%
               OR mr.hlEvntTitl LIKE %:keyword%)
          AND mr.isDeleted = false
        """)
    Page<MarketRecap> findMarketRecaps(
        @Param("startDate") String startDate,
        @Param("endDate") String endDate,
        @Param("keyword") String keyword,
        Pageable pageable
    );

}
