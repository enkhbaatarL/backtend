package com.sk.domain.shared.repository.master.tmm;

import com.sk.domain.shared.entity.master.tmm.MiScenarioPriceInterp;
import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MiScenarioPriceInterpRepository extends JpaRepository<MiScenarioPriceInterp, Long> {

    /**
     * 그룹에 속한 시나리오들의 가격전망 해석
     */
    @Query("""
        select p from MiScenarioPriceInterp p
        where p.scenario.scenarioGroup.scenarioGroupId = :scenarioGroupId
          and p.scenario.isDeleted = false
          and p.isDeleted = false
        """)
    List<MiScenarioPriceInterp> findByScenarioGroupId(@Param("scenarioGroupId") Long scenarioGroupId);

    /**
     * 단일 시나리오의 가격전망 해석 조회
     */
    @Query("""
        select p from MiScenarioPriceInterp p
        where p.scenario.scenarioId = :scenarioId
          and p.scenario.scenarioGroup.scenarioGroupId = :scenarioGroupId
          and p.scenario.isDeleted = false
          and p.scenario.scenarioGroup.isDeleted = false
          and p.isDeleted = false
        """)
    Optional<MiScenarioPriceInterp> findActiveByGroupIdAndScenarioId(@Param("scenarioGroupId") Long scenarioGroupId,
                                                                     @Param("scenarioId") Long scenarioId);

    /**
     * 수급모델 실행 전 시나리오의 활성 가격전망 해석을 write lock으로 조회한다.
     * 활성 row는 불변식상 시나리오당 1건이지만, 락 TTL 만료 등으로 중복이 생겨도 조회가 깨지지 않도록
     * List로 받는다 — 호출부(softDeleteForModelExecution)가 전부 soft delete 하므로 중복도 그 시점에 함께 정리된다.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        select p from MiScenarioPriceInterp p
        where p.scenario.scenarioId = :scenarioId
          and p.isDeleted = false
        """)
    List<MiScenarioPriceInterp> findActiveByScenarioIdForUpdate(@Param("scenarioId") Long scenarioId);

    /**
     * 여러 시나리오의 가격전망 해석 (생성 착수·접수 실패 복구용 일괄 조회).
     */
    List<MiScenarioPriceInterp> findByScenario_ScenarioIdInAndIsDeletedFalse(Collection<Long> scenarioIds);

}
