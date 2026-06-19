package com.sk.domain.shared.repository.master.tmm;

import com.sk.domain.shared.entity.master.tmm.MiScenario;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MiScenarioRepository extends JpaRepository<MiScenario, Long> {

    Optional<MiScenario> findByScenarioIdAndIsDeletedFalse(Long scenarioId);
}
