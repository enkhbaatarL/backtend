package com.sk.domain.shared.repository.master.tmd;

import com.sk.domain.shared.entity.master.tmd.ScenarioCaseTradeFlowHeader;
import com.sk.domain.shared.repository.master.tmd.projection.ScenarioCaseTradeFlowHeaderRow;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ScenarioCaseTradeFlowHeaderRepository extends JpaRepository<ScenarioCaseTradeFlowHeader, Long> {

    @Query("""
        SELECT new com.sk.domain.shared.repository.master.tmd.projection.ScenarioCaseTradeFlowHeaderRow(
            h.axisCode,
            h.headerKey,
            h.headerLabelName,
            h.headerKindCode,
            h.regionCode,
            h.nationName,
            h.headerOrder
        )
          FROM ScenarioCaseTradeFlowHeader h
         WHERE h.isDeleted = false
           AND h.runSeq = :runSeq
           AND h.selectorCode = :selectorCode
           AND h.baseYear = :baseYear
         ORDER BY h.axisCode,
                  h.headerOrder
        """)
    List<ScenarioCaseTradeFlowHeaderRow> findRowsByRunSeqAndSelectorAndYear(
        @Param("runSeq") Long runSeq,
        @Param("selectorCode") String selectorCode,
        @Param("baseYear") Integer baseYear
    );
}
