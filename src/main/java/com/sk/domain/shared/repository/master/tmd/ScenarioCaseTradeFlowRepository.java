package com.sk.domain.shared.repository.master.tmd;

import com.sk.domain.shared.entity.master.tmd.ScenarioCaseTradeFlow;
import com.sk.domain.shared.repository.master.tmd.projection.ScenarioCaseTradeFlowRow;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ScenarioCaseTradeFlowRepository extends JpaRepository<ScenarioCaseTradeFlow, Long> {

    @Query("""
        SELECT new com.sk.domain.shared.repository.master.tmd.projection.ScenarioCaseTradeFlowRow(
            f.fromHeaderKey,
            f.toHeaderKey,
            f.fromRegionCode,
            f.fromNationName,
            f.toRegionCode,
            f.toNationName,
            f.flowValue
        )
          FROM ScenarioCaseTradeFlow f
         WHERE f.isDeleted = false
           AND f.runSeq = :runSeq
           AND f.selectorCode = :selectorCode
           AND f.baseYear = :baseYear
        """)
    List<ScenarioCaseTradeFlowRow> findRowsByRunSeqAndSelectorAndYear(
        @Param("runSeq") Long runSeq,
        @Param("selectorCode") String selectorCode,
        @Param("baseYear") Integer baseYear
    );
}
