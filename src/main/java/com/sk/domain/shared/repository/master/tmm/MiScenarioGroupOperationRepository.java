package com.sk.domain.shared.repository.master.tmm;

import com.sk.domain.shared.entity.master.tmm.MiScenarioGroupOperation;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MiScenarioGroupOperationRepository extends JpaRepository<MiScenarioGroupOperation, String> {

    /**
     * 사용자의 진행중 작업 조회 (설계 시작 선조회 정상 경로).
     * 소유자 판정은 그룹의 request_usr_id 기준이며, 최근 생성 순으로 반환한다.
     */
    List<MiScenarioGroupOperation> findByScenarioGroup_RequestUserIdAndStatusInAndIsDeletedFalseAndScenarioGroup_IsDeletedFalseOrderByCreateDtDesc(
        Long requestUserId, Collection<String> statuses);

    /**
     * 그룹들의 operation 이력 조회 (최근 생성 순 — 그룹별 첫 행이 최신 operation).
     * 이벤트 연결 그룹 패널의 실패 방치 그룹 필터에 사용한다.
     */
    List<MiScenarioGroupOperation> findByScenarioGroup_ScenarioGroupIdInAndIsDeletedFalseOrderByCreateDtDesc(
        Collection<Long> scenarioGroupIds);

}
