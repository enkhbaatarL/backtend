package com.sk.domain.mi.service.nextgen.support;

import com.sk.domain.shared.entity.master.tmm.MiScenarioGroupOperation;
import com.sk.domain.shared.enums.nextgen.DesignOperationStatus;
import com.sk.domain.shared.enums.nextgen.DesignOperationType;
import com.sk.domain.shared.repository.master.tmm.MiScenarioGroupOperationRepository;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 실패 방치(좀비) 시나리오 그룹 판정기 — 좀비 판정의 자바 측 단일 지점.
 *
 * 좀비 = 그룹 status가 설계중(creating)인데 최신 create operation이 failed/interrupted로 끝나
 * DB 두 테이블이 모순 상태로 방치된 그룹. Agent가 실패 시 그룹 상태를 되돌리지 않는 동안의
 * 잠정 규칙이며, Agent 규약 확정 후 실패 그룹이 방치되지 않으면 판정은 자연히 no-op이 된다.
 *
 * create 작업만 판정 대상 — 편집(validation 등) 작업의 실패는 세션 실패가 아니다.
 * operation이 없는 그룹(v1)은 좀비가 아니다.
 *
 * 페이징·집계와 결합되어 자바 후처리가 불가능한 조회(이벤트 목록, 설계 가능 상태)는
 * 같은 규칙의 쿼리 버전(CustomMiEventRepositoryImpl.notFailedCreating)을 사용한다. 규칙 변경 시 두 곳을 함께 고친다.
 */
@Component
@RequiredArgsConstructor
public class FailedCreatingGroupResolver {

    private final MiScenarioGroupOperationRepository operationRepository;

    /**
     * 단일 그룹 좀비 판정. 그룹 status가 creating인 경우에만 의미가 있다.
     */
    public boolean isFailedCreating(Long scenarioGroupId) {
        return !resolve(List.of(scenarioGroupId)).isEmpty();
    }

    /**
     * creating 그룹 ID들 중 좀비 그룹 ID 집합. (그룹 수와 무관하게 operation 조회 1회)
     */
    public Set<Long> resolve(List<Long> creatingGroupIds) {

        if (creatingGroupIds.isEmpty()) return Set.of();

        // operation_id ULID 내림차순 조회라 그룹별 첫 등장 create operation이 최신이다 (쿼리 버전 notFailedCreating과 동일 기준)
        Map<Long, MiScenarioGroupOperation> latestByGroup = new LinkedHashMap<>();
        List<MiScenarioGroupOperation> opers = operationRepository.findByScenarioGroup_ScenarioGroupIdInAndIsDeletedFalseOrderByOperationIdDesc(creatingGroupIds);
        for (MiScenarioGroupOperation operation : opers) {
            if (DesignOperationType.CREATE != DesignOperationType.fromNullable(operation.getOperationType())) {
                continue;
            }
            latestByGroup.putIfAbsent(operation.getScenarioGroup().getScenarioGroupId(), operation);
        }

        return latestByGroup.values().stream()
            .filter(operation -> DesignOperationStatus.isFailedTerminal(operation.getStatus()))
            .map(operation -> operation.getScenarioGroup().getScenarioGroupId())
            .collect(Collectors.toSet());
    }

}
