package com.sk.domain.shared.repository.master.tmd;

import com.sk.domain.shared.entity.master.tmd.EconomicIndicatorAnalysis;
import com.sk.domain.shared.enums.news.MarketCategoryCode;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EconomicIndicatorAnalysisRepository extends JpaRepository<EconomicIndicatorAnalysis, Long> {

    /**
     * 기준일자와 카테고리로 경제 지표 분석 조회
     */
    @Query("""
        SELECT eia
        FROM EconomicIndicatorAnalysis eia
        WHERE eia.crtrDt = :criteriaDate
          AND eia.nwsMrktCtgrCd = :categoryCode
          AND eia.isDeleted = false
        """)
    Optional<EconomicIndicatorAnalysis> findByCriteriaDateAndCategory(
        @Param("criteriaDate") String criteriaDate,
        @Param("categoryCode") MarketCategoryCode categoryCode
    );
}
