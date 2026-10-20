package com.sk.domain.mi.service.nextgen.paramproposal;

import com.sk.domain.mi.dto.response.nextgen.MiBaseRefRes;
import com.sk.domain.shared.repository.master.tmd.ModelExecutionPreparationRepository;
import com.sk.domain.shared.repository.master.tmd.ScenarioCaseRepository;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 시나리오 그룹의 MI Base 판정.
 * 그룹 MI Base는 별도 저장 없이 시나리오별 최신 실행준비 행 → 제안(proposal_m.mi_base_case_id)에서 파생한다.
 * 그룹 내 MI Base는 하나로 통일하므로, 초기화 전까지 [생성]·재시도는 그룹에 적용된 MI Base를 이어 쓴다.
 */
@Component
@RequiredArgsConstructor
public class GroupMiBaseResolver {

    private final ModelExecutionPreparationRepository modelExecutionPreparationRepository;
    private final ScenarioCaseRepository scenarioCaseRepository;

    /** 그룹에 적용된 MI Base caseId. 여러 값이 섞여 있으면(이전 데이터) 가장 최근 제안의 값. 없으면 empty */
    public Optional<Long> findAppliedCaseId(Long scenarioGroupId) {
        return modelExecutionPreparationRepository.findGroupMiBaseCaseIds(scenarioGroupId).stream().findFirst();
    }

    public GroupMiBaseStatus status(Long scenarioGroupId) {
        List<Long> appliedCaseIds = modelExecutionPreparationRepository.findGroupMiBaseCaseIds(scenarioGroupId);

        MiBaseRefRes applied = appliedCaseIds.stream().findFirst()
            .flatMap(scenarioCaseRepository::findById)
            .map(MiBaseRefRes::of)
            .orElse(null);
        MiBaseRefRes current = scenarioCaseRepository.findCurrentMiBase()
            .map(MiBaseRefRes::of)
            .orElse(null);

        // 그룹 시나리오 중 하나라도 current와 다른 MI Base로 생성됐으면 변경
        boolean changed = current != null && appliedCaseIds.stream().anyMatch(caseId -> !caseId.equals(current.caseId()));
        return new GroupMiBaseStatus(applied, current, changed);
    }

    public record GroupMiBaseStatus(MiBaseRefRes applied, MiBaseRefRes current, boolean changed) {
    }
}
