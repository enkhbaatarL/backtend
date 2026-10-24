package com.sk.domain.mi.dto.response.nextgen;

import com.sk.domain.mi.service.nextgen.support.ScenarioProbabilityAllocator;
import com.sk.domain.shared.enums.nextgen.ScenarioRole;
import com.sk.domain.shared.repository.master.tmm.projection.ScenarioDetailBaseRow;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 개별 시나리오 상세. 내러티브 + 발생 확률 + 주요 가정 + Signpost 정의/모니터링.
 */
public record ScenarioDetailViewRes(

    @Schema(description = "시나리오 ID", example = "11")
    Long scenarioId,

    @Schema(description = "소속 시나리오 그룹 ID", example = "1")
    Long scenarioGroupId,

    @Schema(description = "시나리오명", example = "단기 봉쇄와 제한적 공급 대응")
    String scenarioName,

    @Schema(description = "BC(Base Case) 여부", example = "false")
    boolean baseCase,

    @Schema(description = "시나리오 상대 역할 (ceiling/floor) - 시나리오명 앞 역할 배지. v4 대표 시나리오에만 존재, 혼합(mixed)·그 외 null", example = "ceiling", nullable = true)
    ScenarioRole scenarioRole,

    @Schema(description = "시나리오 상대 역할 라벨 (상단/하단). 역할 없으면 null", example = "상단", nullable = true)
    String scenarioRoleLabel,

    @Schema(description = "표시 순서", example = "1")
    Integer displayOrder,

    @Schema(description = "시나리오 생성일시")
    LocalDateTime createdAt,

    @Schema(description = "시나리오 수정일시")
    LocalDateTime updatedAt,

    @Schema(description = "시나리오 내러티브")
    String narrative,

    @Schema(description = "시나리오 발생 확률 (0~100, 원본)", example = "30")
    Integer probability,

    @Schema(description = "재분배 확률 (0~100). 폐기 제외 비례 재분배로 활성 시나리오 합 100, 폐기는 0. 조회 시점 계산값(비영속)", example = "43")
    Integer adjustedProbability,

    @Schema(description = "주요 가정")
    List<ScenarioDimensionRes> dimensions,

    @Schema(description = "Signpost 정의")
    List<ScenarioSignpostDefinitionViewRes> signpostDefinitions,

    @Schema(description = "Signpost 모니터링 현황")
    List<ScenarioSignpostMonitoringRes> signpostMonitorings

) {

    public static ScenarioDetailViewRes of(ScenarioDetailBaseRow base,
                                           List<ScenarioDimensionRes> dimensions,
                                           Integer adjustedProbability,
                                           List<ScenarioSignpostDefinitionViewRes> signpostDefinitions,
                                           List<ScenarioSignpostMonitoringRes> signpostMonitorings) {
        ScenarioRole scenarioRole = ScenarioRole.forBadge(base.scenarioRole());
        return new ScenarioDetailViewRes(
            base.scenarioId(),
            base.scenarioGroupId(),
            base.title(),
            Boolean.TRUE.equals(base.isBaseline()),
            scenarioRole,
            scenarioRole == null ? null : scenarioRole.getLabel(),
            base.displayOrder(),
            base.createdAt(),
            base.updatedAt(),
            base.narrative(),
            ScenarioProbabilityAllocator.toDisplayPercent(base.probabilityScore()),
            adjustedProbability,
            dimensions,
            signpostDefinitions,
            signpostMonitorings
        );
    }

}
