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
     * 시나리오 그룹들의 operation 이력 조회 (최근 생성 순 — 그룹별 첫 행이 최신 operation).
     */
    List<MiScenarioGroupOperation> findByScenarioGroup_ScenarioGroupIdInAndIsDeletedFalseOrderByCreateDtDesc(Collection<Long> scenarioGroupIds);

}
