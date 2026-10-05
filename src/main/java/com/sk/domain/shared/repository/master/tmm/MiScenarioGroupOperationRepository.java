package com.sk.domain.shared.repository.master.tmm;

import com.sk.domain.shared.entity.master.tmm.MiScenarioGroupOperation;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MiScenarioGroupOperationRepository extends JpaRepository<MiScenarioGroupOperation, String> {

    /**
     * 시나리오 그룹들의 operation 이력 조회 (operation_id ULID 내림차순 — 그룹별 첫 행이 최신 operation).
     * 실패 방치 그룹 판정(FailedCreatingGroupResolver)에 사용한다. 최신 판정 기준을
     * 쿼리 버전(CustomMiEventRepositoryImpl.notFailedCreating의 operationId.max())과 동일하게 ULID로 맞춘다
     * — create_dt는 동시각 충돌이 가능해 기준으로 쓰지 않는다.
     */
    List<MiScenarioGroupOperation> findByScenarioGroup_ScenarioGroupIdInAndIsDeletedFalseOrderByOperationIdDesc(Collection<Long> scenarioGroupIds);

    /**
     * 그룹의 진행 중(queued/running) operation 중 최신 1건 조회 (타입 한정).
     * 생성 스트림 재연결 대상(create) 조회에도 사용한다.
     */
    Optional<MiScenarioGroupOperation> findFirstByScenarioGroup_ScenarioGroupIdAndOperationTypeInAndStatusInAndIsDeletedFalseOrderByOperationIdDesc(
        Long scenarioGroupId,
        Collection<String> operationTypes,
        Collection<String> statuses
    );

}
