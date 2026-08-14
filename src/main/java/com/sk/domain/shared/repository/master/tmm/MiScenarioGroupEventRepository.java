package com.sk.domain.shared.repository.master.tmm;

import com.sk.domain.shared.entity.master.tmm.MiScenarioGroupEvent;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MiScenarioGroupEventRepository extends JpaRepository<MiScenarioGroupEvent, Long> {

    /**
     * 이벤트에 활성(비삭제) 시나리오 그룹이 존재하는지 선검사용.
     * 매핑과 그룹 양쪽 모두 비삭제인 연결만 활성으로 본다.
     */
    boolean existsByMiEvent_MiEventIdAndIsDeletedFalseAndScenarioGroup_IsDeletedFalse(Long miEventId);

    /**
     * 그룹을 구성하는 활성 이벤트 ID 목록. 설계 시작 선조회의 이벤트 조합 비교용.
     */
    @Query("select e.miEvent.miEventId from MiScenarioGroupEvent e "
        + "where e.scenarioGroup.scenarioGroupId = :scenarioGroupId and e.isDeleted = false")
    List<Long> findActiveEventIdsByScenarioGroupId(@Param("scenarioGroupId") Long scenarioGroupId);

}
