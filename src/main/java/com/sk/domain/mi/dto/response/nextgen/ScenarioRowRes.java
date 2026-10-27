package com.sk.domain.mi.dto.response.nextgen;

import com.sk.domain.mi.service.nextgen.support.ScenarioProbabilityAllocator;
import com.sk.domain.shared.enums.nextgen.ScenarioDisplayStatus;
import com.sk.domain.shared.enums.nextgen.ScenarioRole;
import com.sk.domain.shared.repository.master.tmm.projection.ScenarioBaseRow;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * 시나리오 그룹 표 뷰의 한 행 (그룹에 속한 개별 시나리오).
 */
public record ScenarioRowRes(

    @Schema(description = "시나리오 ID", example = "11")
    Long scenarioId,

    @Schema(description = "시나리오명", example = "단기 외교 해결")
    String scenarioName,

    @Schema(description = "BC(Base Case) 여부", example = "false")
    boolean baseCase,

    @Schema(description = "시나리오 상대 역할 (ceiling/floor). 혼합(mixed)·역할 없음은 null", example = "ceiling", nullable = true)
    ScenarioRole scenarioRole,

    @Schema(description = "시나리오 상대 역할 라벨 (상단/하단)", example = "상단", nullable = true)
    String scenarioRoleLabel,

    @Schema(description = "표시 순서", example = "1")
    Integer displayOrder,

    @Schema(description = "차원값 목록 (dimensionColumns 순서와 매칭)")
    List<ScenarioDimensionRes> dimensions,

    @Schema(description = "시나리오 발생 확률 (0~100, 원본)", example = "30")
    Integer probability,

    @Schema(description = "재분배 확률 (0~100). 폐기 제외 비례 재분배로 활성 시나리오 합 100, 폐기는 0. 조회 시점 계산값(비영속)", example = "43")
    Integer adjustedProbability,

    @Schema(description = "판정 상태 (pending/realized/discarded)", example = "pending")
    ScenarioDisplayStatus status,

    @Schema(description = "판정 상태 라벨", example = "대기")
    String statusLabel

) {

    /** dimension 셀은 template 병합이 필요해 외부에서 조립한 값을 받는다. */
    public static ScenarioRowRes of(ScenarioBaseRow row, List<ScenarioDimensionRes> dimensions, Integer adjustedProbability) {
        ScenarioDisplayStatus status = ScenarioDisplayStatus.from(row.status());
        ScenarioRole scenarioRole = ScenarioRole.forBadge(row.scenarioRole());
        return new ScenarioRowRes(
            row.scenarioId(),
            row.title(),
            Boolean.TRUE.equals(row.isBaseline()),
            scenarioRole,
            scenarioRole == null ? null : scenarioRole.getLabel(),
            row.displayOrder(),
            dimensions,
            ScenarioProbabilityAllocator.toDisplayPercent(row.probabilityScore()),
            adjustedProbability,
            status,
            status.getLabel()
        );
    }

}
