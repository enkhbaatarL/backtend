package com.sk.domain.shared.repository.master.tmm;

import com.sk.domain.shared.entity.master.tmm.MiScenarioPriceInterp;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
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
     * 여러 시나리오의 가격전망 해석 (생성 착수·접수 실패 복구용 일괄 조회).
     */
    List<MiScenarioPriceInterp> findByScenario_ScenarioIdInAndIsDeletedFalse(Collection<Long> scenarioIds);

}
