package com.sk.domain.shared.repository.master.tmm;

import com.sk.domain.shared.entity.master.tmm.MiScenarioGroupOperation;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MiScenarioGroupOperationRepository extends JpaRepository<MiScenarioGroupOperation, String> {

    /**
     * 사용자의 진행중 작업 조회 (설계 시작 선조회 정상 경로, 그룹 fetch join).
     * 그룹을 fetch join으로 즉시 로딩해 트랜잭션 밖에서도 그룹 필드를 읽을 수 있게 한다 (OSIV off).
     */
    @Query("""
        select o
        from MiScenarioGroupOperation o
        join fetch o.scenarioGroup g
        where g.requestUserId = :requestUserId
          and o.status in :statuses
          and o.isDeleted = false
          and g.isDeleted = false
        order by o.createDt desc
        """)
    List<MiScenarioGroupOperation> findActiveWithGroupByRequestUserId(
        @Param("requestUserId") Long requestUserId, @Param("statuses") Collection<String> statuses
    );

    /**
     * 시나리오 그룹들의 operation 이력 조회 (최근 생성 순 — 그룹별 첫 행이 최신 operation).
     */
    List<MiScenarioGroupOperation> findByScenarioGroup_ScenarioGroupIdInAndIsDeletedFalseOrderByCreateDtDesc(Collection<Long> scenarioGroupIds);

    /**
     * 그룹의 최신 operation의 presentation_snapshot 조회 (그룹 상세 '근거 보기' — 확정 후 근거는 DB 스냅샷이 원천).
     * presentation_snapshot은 무거운 JSON이라 엔티티에 매핑하지 않고 native로 필요한 곳에서만 읽는다.
     * operation_id는 ULID라 사전순 최대값이 최신이다. operation이 없는 그룹(v1)은 empty.
     */
    @Query(value = """
        select o.presentation_snapshot
        from tmm_mi_scenario_group_operation_m o
        where o.scenario_group_id = :scenarioGroupId
          and o.del_yn = 'N'
          and o.operation_type = :operationType
          and o.status = :status
        order by o.operation_id desc
        limit 1
        """, nativeQuery = true)
    Optional<String> findLatestPresentationSnapshot(
        @Param("scenarioGroupId") Long scenarioGroupId,
        @Param("operationType") String operationType,
        @Param("status") String status
    );

}
