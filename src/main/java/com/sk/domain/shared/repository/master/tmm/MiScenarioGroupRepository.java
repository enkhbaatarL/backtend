package com.sk.domain.shared.repository.master.tmm;

import com.sk.domain.shared.entity.master.tmm.MiScenarioGroup;
import com.sk.domain.shared.repository.master.tmm.custom.CustomMiScenarioGroupRepository;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MiScenarioGroupRepository
    extends JpaRepository<MiScenarioGroup, Long>, CustomMiScenarioGroupRepository {

    Optional<MiScenarioGroup> findByScenarioGroupIdAndIsDeletedFalse(Long scenarioGroupId);

    boolean existsByScenarioGroupIdAndIsDeletedFalse(Long scenarioGroupId);

    /** 이벤트에 활성(비삭제) 시나리오 그룹이 존재하는지 선검사용. */
    boolean existsByMiEvent_MiEventIdAndIsDeletedFalse(Long miEventId);

}
