package com.sk.domain.mi.dto.response.nextgen;

import com.sk.domain.mi.service.nextgen.support.ScenarioProbabilityAllocator;
import com.sk.domain.shared.enums.nextgen.ScenarioDisplayStatus;
import com.sk.domain.shared.enums.nextgen.ScenarioRole;
import com.sk.domain.shared.repository.master.tmm.projection.ScenarioBaseRow;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * 이벤트 상세 '연관 시나리오'의 한 행.
 */
public record EventRelatedScenarioRes(

    @Schema(description = "시나리오 ID", example = "11")
    Long scenarioId,

    @Schema(description = "시나리오명", example = "단기 봉쇄와 제한적 공급 대응")
    String scenarioName,

    @Schema(description = "BC(Base Case) 여부", example = "false")
    boolean baseCase,

    @Schema(description = "시나리오 상대 역할 (ceiling/floor/mixed) - 시나리오명 앞 역할 배지. v4 대표 시나리오 3종에만 존재, 그 외 null", example = "ceiling", nullable = true)
    ScenarioRole scenarioRole,

    @Schema(description = "시나리오 상대 역할 라벨 (상단/하단/혼합). 역할 없으면 null", example = "상단", nullable = true)
    String scenarioRoleLabel,

    @Schema(description = "표시 순서", example = "1")
    Integer displayOrder,

    @Schema(description = "차원값 목록 (dimensionColumns 순서와 매칭)")
    List<ScenarioDimensionRes> dimensions,

    @Schema(description = "시나리오 발생 확률 (0~100, 원본)", example = "30")
    Integer probability,

    @Schema(description = "재분배 확률 (0~100). 폐기 제외 비례 재분배로 활성 시나리오 합 100, 폐기는 0. 폐기가 없으면 probability와 동일. 항상 존재", example = "43")
    Integer adjustedProbability,

    @Schema(description = "판정 상태 (pending/realized/discarded)", example = "pending")
    ScenarioDisplayStatus status,

    @Schema(description = "판정 상태 라벨", example = "대기")
    String statusLabel

) {

    public static EventRelatedScenarioRes of(ScenarioBaseRow row, List<ScenarioDimensionRes> dimensions, Integer adjustedProbability) {
        ScenarioDisplayStatus status = ScenarioDisplayStatus.from(row.status());
        ScenarioRole scenarioRole = ScenarioRole.fromNullable(row.scenarioRole());
        return new EventRelatedScenarioRes(
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
