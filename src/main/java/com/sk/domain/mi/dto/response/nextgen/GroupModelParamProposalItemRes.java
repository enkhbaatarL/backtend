package com.sk.domain.mi.dto.response.nextgen;

import com.sk.domain.mi.service.nextgen.support.ScenarioProbabilityAllocator;
import com.sk.domain.shared.entity.master.tmm.MiScenario;
import com.sk.domain.shared.enums.nextgen.CurrentModelParamProposalState;
import com.sk.domain.shared.enums.nextgen.ScenarioDisplayStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

/**
 * 그룹 파라미터 제안 현황의 시나리오 한 행.
 * 분기값·차원 헤더는 그룹 상세 API가 이미 제공하므로 여기서는 제안 상태 중심으로 내린다.
 * 제안 상태는 단건 현재 상태 조회와 동일한 currentState 단일 필드로 내리고,
 * 화면 5종(생성/생성 중/완료/실패/재시도) 매핑 규칙은 컨트롤러 API 문서에 명시한다.
 */
public record GroupModelParamProposalItemRes(

    @Schema(description = "시나리오 ID", example = "309")
    Long scenarioId,

    @Schema(description = "시나리오명", example = "항행 차질과 아프리카 공급 훼손 결합")
    String scenarioName,

    @Schema(description = "표시 순서", example = "1")
    Integer displayOrder,

    @Schema(description = "시나리오 발생 확률 (0~100, 원본). 폐기해도 원본은 유지됨", example = "15", nullable = true)
    Integer probability,

    @Schema(description = "재분배 확률 (0~100). 폐기 제외 비례 재분배로 활성 시나리오 합 100, 폐기는 0. 조회 시점 계산값(비영속) — 실행가능성 컬럼은 이 값을 표시", example = "20")
    Integer adjustedProbability,

    @Schema(description = "시나리오 판정 상태 (pending/realized/discarded). discarded는 제안 생성 불가", implementation = ScenarioDisplayStatus.class, example = "pending")
    ScenarioDisplayStatus scenarioStatus,

    @Schema(description = "시나리오 판정 상태 라벨 (대기/실현/폐기)", example = "대기")
    String scenarioStatusLabel,

    @Schema(description = "제안~실행 현재 상태 (단건 현재 상태 조회와 동일 규칙). 화면 5종 매핑은 API 설명 참조", implementation = CurrentModelParamProposalState.class, example = "PROPOSAL_READY")
    CurrentModelParamProposalState currentState,

    @Schema(description = "Agent model parameter proposal ID. 미발급이면 null", example = "10001", nullable = true)
    Long modelParamProposalId,

    @Schema(description = "Backend 요청 이력 ID. 요청 전에는 null", example = "51001", nullable = true)
    Long execPrepId,

    @Schema(description = "실행에 사용된 입력 파일 3개(DEMAND/SUPPLY/TRADE) 메타", nullable = true)
    ModelExecutionInputFilesRes executionInputFiles,

    @Schema(description = "실패 사유. 실패/재시도 상태 외에는 null", nullable = true)
    String failureReason,

    @Schema(description = "제안 요청 접수 시각. 요청 전에는 null", nullable = true)
    LocalDateTime requestedAt,

    @Schema(description = "Agent proposal 완료 시각. 완료 전에는 null", nullable = true)
    LocalDateTime completedAt,

    @Schema(description = "Agent confirmation 완료 시각. 확정 전에는 null", nullable = true)
    LocalDateTime confirmedAt
) {

    public static GroupModelParamProposalItemRes of(
        MiScenario scenario,
        CurrentModelParamProposalRes current,
        Integer adjustedProbability
    ) {
        ScenarioDisplayStatus scenarioStatus = ScenarioDisplayStatus.from(scenario.getStatus());
        return new GroupModelParamProposalItemRes(
            scenario.getScenarioId(),
            scenario.getTitle(),
            scenario.getDisplayOrder(),
            ScenarioProbabilityAllocator.toDisplayPercent(scenario.getProbabilityScore()),
            adjustedProbability,
            scenarioStatus,
            scenarioStatus.getLabel(),
            current.currentState(),
            current.modelParamProposalId(),
            current.execPrepId(),
            current.executionInputFiles(),
            current.failureReason(),
            current.requestedAt(),
            current.completedAt(),
            current.confirmedAt()
        );
    }
}
