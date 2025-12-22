package com.sk.domain.shared.repository.master.tmd;

import com.sk.domain.shared.entity.master.tmd.EconomicIndicatorAnalysis;
import com.sk.domain.shared.entity.master.tmd.EconomicIndicatorAnalysisRelationship;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EconomicIndicatorAnalysisRelationshipRepository extends JpaRepository<EconomicIndicatorAnalysisRelationship, Long> {

    /**
     * 경제 지표 분석 ID로 관계 목록 조회 (경제 달력 데이터 포함)
     */
    @Query("""
        SELECT eiar
        FROM EconomicIndicatorAnalysisRelationship eiar
        JOIN FETCH eiar.economicCalendar ec
        WHERE eiar.economicIndicatorAnalysis = :analysis
          AND eiar.isDeleted = false
        ORDER BY ec.economicIndicatorPresentationTime ASC
        """)
    List<EconomicIndicatorAnalysisRelationship> findByEconomicIndicatorAnalysisWithEconomicCalendar(
        @Param("analysis") EconomicIndicatorAnalysis analysis
    );
}
