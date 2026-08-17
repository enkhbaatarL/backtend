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
    @Query("""
        select e.miEvent.miEventId
        from MiScenarioGroupEvent e
        where e.scenarioGroup.scenarioGroupId = :scenarioGroupId
          and e.isDeleted = false
        """)
    List<Long> findActiveEventIdsByScenarioGroupId(@Param("scenarioGroupId") Long scenarioGroupId);

    /**
     * 이벤트에 연결된 활성 그룹의 매핑 목록 (그룹 최신 생성 순, 그룹 fetch join).
     * 이벤트 조합 설계 상태 조회의 후보 축소용 — 정확 일치 그룹은 선택한 모든 이벤트에 연결되므로 첫 이벤트만으로 후보를 좁힌다.
     * 그룹을 fetch join으로 즉시 로딩해 트랜잭션 밖에서도 그룹 필드를 읽을 수 있게 한다 (OSIV off).
     */
    @Query("""
        select e
        from MiScenarioGroupEvent e
        join fetch e.scenarioGroup g
        where e.miEvent.miEventId = :miEventId
          and e.isDeleted = false
          and g.isDeleted = false
        order by g.createDt desc
        """)
    List<MiScenarioGroupEvent> findActiveWithGroupByMiEventId(@Param("miEventId") Long miEventId);

}
