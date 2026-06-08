package com.sk.domain.shared.repository.master.tmd;

import com.sk.domain.shared.entity.master.tmd.ScenarioCase;
import com.sk.domain.shared.repository.master.tmd.custom.CustomScenarioCaseRepository;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ScenarioCaseRepository extends JpaRepository<ScenarioCase, Long>, CustomScenarioCaseRepository {

    Optional<ScenarioCase> findByCaseIdAndIsDeletedFalse(Long caseId);
}
