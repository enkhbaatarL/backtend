package com.sk.domain.shared.repository.master.tmm;

import com.sk.domain.shared.entity.master.tmm.MiScenario;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MiScenarioRepository extends JpaRepository<MiScenario, Long> {

    Optional<MiScenario> findByScenarioIdAndIsDeletedFalse(Long scenarioId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT s
          FROM MiScenario s
         WHERE s.scenarioId = :scenarioId
           AND s.isDeleted = false
        """)
    Optional<MiScenario> findByScenarioIdForUpdate(@Param("scenarioId") Long scenarioId);
}
