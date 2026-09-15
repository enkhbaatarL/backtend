package com.sk.domain.shared.repository.master.tmd;

import com.sk.domain.shared.entity.master.tmd.ScenarioCasePrice;
import com.sk.domain.shared.repository.master.tmd.projection.ScenarioCasePriceRow;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ScenarioCasePriceRepository extends JpaRepository<ScenarioCasePrice, Long> {

    boolean existsByRunSeqAndIsDeletedFalse(Long runSeq);

    @Query("""
        SELECT new com.sk.domain.shared.repository.master.tmd.projection.ScenarioCasePriceRow(
            p.periodUnitSeparateCode,
            p.periodKey,
            p.priceRegionName,
            p.baseCasePrice,
            p.userAdjustedPrice
        )
          FROM ScenarioCasePrice p
         WHERE p.isDeleted = false
           AND p.runSeq = :runSeq
         ORDER BY p.periodUnitSeparateCode,
                  p.periodKey,
                  p.priceRegionName
        """)
    List<ScenarioCasePriceRow> findRowsByRunSeq(@Param("runSeq") Long runSeq);
}
