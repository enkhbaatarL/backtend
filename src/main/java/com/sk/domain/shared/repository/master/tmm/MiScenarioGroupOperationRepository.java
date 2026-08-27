package com.sk.domain.shared.repository.master.tmm;

import com.sk.domain.shared.entity.master.tmm.MiScenarioGroupOperation;
import java.util.Collection;
import java.util.List;
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
     * 시나리오 그룹들의 operation 이력 조회 (operation_id ULID 내림차순 — 그룹별 첫 행이 최신 operation).
     * 실패 방치 그룹 판정(FailedCreatingGroupResolver)에 사용한다. 최신 판정 기준을
     * 쿼리 버전(CustomMiEventRepositoryImpl.notFailedCreating의 operationId.max())과 동일하게 ULID로 맞춘다
     * — create_dt는 동시각 충돌이 가능해 기준으로 쓰지 않는다.
     */
    List<MiScenarioGroupOperation> findByScenarioGroup_ScenarioGroupIdInAndIsDeletedFalseOrderByOperationIdDesc(Collection<Long> scenarioGroupIds);

}
