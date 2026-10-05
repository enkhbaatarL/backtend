package com.sk.domain.shared.repository.master.tmm;

import com.sk.domain.shared.entity.master.tmm.MiScenarioGroupEvent;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MiScenarioGroupEventRepository extends JpaRepository<MiScenarioGroupEvent, Long> {

    /**
     * 이벤트에 활성(비삭제) 시나리오 그룹이 존재하는지 선검사용.
     * 매핑과 그룹 양쪽 모두 비삭제인 연결만 활성으로 본다.
     */
    boolean existsByMiEvent_MiEventIdAndIsDeletedFalseAndScenarioGroup_IsDeletedFalse(Long miEventId);

    /**
     * 그룹의 활성 이벤트 매핑 엔티티 목록. 그룹 삭제 시 매핑을 함께 soft delete 하기 위한 조회용.
     */
    List<MiScenarioGroupEvent> findByScenarioGroup_ScenarioGroupIdAndIsDeletedFalse(Long scenarioGroupId);

}
