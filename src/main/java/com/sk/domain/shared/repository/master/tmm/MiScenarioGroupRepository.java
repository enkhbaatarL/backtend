package com.sk.domain.shared.repository.master.tmm;

import com.sk.domain.shared.entity.master.tmm.MiScenarioGroup;
import com.sk.domain.shared.repository.master.tmm.custom.CustomMiScenarioGroupRepository;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MiScenarioGroupRepository
    extends JpaRepository<MiScenarioGroup, Long>, CustomMiScenarioGroupRepository {

    Optional<MiScenarioGroup> findByScenarioGroupIdAndIsDeletedFalse(Long scenarioGroupId);

    boolean existsByScenarioGroupIdAndIsDeletedFalse(Long scenarioGroupId);

    /**
     * 활성 이벤트 구성이 요청 조합과 정확히 일치하는 활성 그룹 목록 (생성일시 최신순).
     * 구성 이벤트 수가 eventCount와 같고, 그 전부가 요청 조합에 속하는 그룹만 일치로 본다.
     */
    @Query("""
        select g
        from MiScenarioGroup g
        where g.isDeleted = false
          and g.scenarioGroupId in (
            select e.scenarioGroup.scenarioGroupId
            from MiScenarioGroupEvent e
            where e.isDeleted = false
            group by e.scenarioGroup.scenarioGroupId
            having count(distinct e.miEvent.miEventId) = :eventCount
               and count(distinct case when e.miEvent.miEventId in :miEventIds then e.miEvent.miEventId end) = :eventCount
          )
        order by g.createDt desc, g.scenarioGroupId desc
        """)
    List<MiScenarioGroup> findActiveByExactEventSet(@Param("miEventIds") Collection<Long> miEventIds, @Param("eventCount") long eventCount);

}
