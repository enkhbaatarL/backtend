package com.sk.domain.shared.repository.master.tmm;

import com.sk.domain.shared.entity.master.tmm.MiScenarioGroup;
import com.sk.domain.shared.repository.master.tmm.custom.CustomMiScenarioGroupRepository;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MiScenarioGroupRepository
    extends JpaRepository<MiScenarioGroup, Long>, CustomMiScenarioGroupRepository {

    Optional<MiScenarioGroup> findByScenarioGroupIdAndIsDeletedFalse(Long scenarioGroupId);

    boolean existsByScenarioGroupIdAndIsDeletedFalse(Long scenarioGroupId);

}
