package com.sk.domain.mi.dto.response.nextgen;

import com.sk.domain.mi.service.nextgen.support.ScenarioProbabilityAllocator;
import com.sk.domain.shared.enums.nextgen.ScenarioDisplayStatus;
import com.sk.domain.shared.enums.nextgen.ScenarioRole;
import com.sk.domain.shared.repository.master.tmm.projection.ScenarioIndividualBaseRow;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 개별(individual) 모드의 평면 시나리오 목록 행.
 */
public record ScenarioIndividualItemRes(

    @Schema(description = "시나리오 ID", example = "11")
    Long scenarioId,

    @Schema(description = "시나리오명", example = "단기 외교 해결")
    String scenarioName,

    @Schema(description = "BC(Base Case) 여부", example = "false")
    boolean baseCase,

    @Schema(description = "시나리오 상대 역할 (ceiling/floor/mixed) - 시나리오명 앞 역할 배지. v4 대표 시나리오 3종에만 존재, 그 외 null", example = "ceiling", nullable = true)
    ScenarioRole scenarioRole,

    @Schema(description = "시나리오 상대 역할 라벨 (상단/하단/혼합). 역할 없으면 null", example = "상단", nullable = true)
    String scenarioRoleLabel,

    @Schema(description = "표시 순서 (그룹 내)", example = "1")
    Integer displayOrder,

    @Schema(description = "시나리오 생성일시 (최신순 정렬·기간 필터 기준)")
    LocalDateTime createdAt,

    @Schema(description = "시나리오 수정일시")
    LocalDateTime updatedAt,

    @Schema(description = "부모 시나리오 그룹")
    ScenarioGroupRefRes group,

    @Schema(description = "차원값 목록")
    List<ScenarioDimensionRes> dimensions,

    @Schema(description = "시나리오 발생 확률 (0~100, 원본). 라벨/등급 환산은 FE 처리", example = "30")
    Integer probability,

    @Schema(description = "재분배 확률 (0~100). 폐기 제외 비례 재분배로 활성 시나리오 합 100, 폐기는 0. 조회 시점 계산값(비영속)", example = "43")
    Integer adjustedProbability,

    @Schema(description = "판정 상태 (pending/realized/discarded)", example = "pending")
    ScenarioDisplayStatus status,

    @Schema(description = "판정 상태 라벨", example = "대기")
    String statusLabel

) {

    /** 개별 모드 base row + 조립된 dimensions → 응답 행. */
    public static ScenarioIndividualItemRes of(ScenarioIndividualBaseRow row, List<ScenarioDimensionRes> dimensions,
                                               Integer adjustedProbability) {
        ScenarioDisplayStatus status = ScenarioDisplayStatus.from(row.status());
        return new ScenarioIndividualItemRes(
            row.scenarioId(),
            row.scenarioTitle(),
            Boolean.TRUE.equals(row.isBaseline()),
            // 유리M 요청사항 - 역할 배지 미노출. DB에 값이 있어도 null로 내린다 (되돌릴 때는 ScenarioRole.fromNullable(row.scenarioRole()) 복원)
            null,
            null,
            row.displayOrder(),
            row.createdAt(),
            row.updatedAt(),
            new ScenarioGroupRefRes(row.scenarioGroupId(), row.groupTitle(), row.groupCreatedAt()),
            dimensions,
            ScenarioProbabilityAllocator.toDisplayPercent(row.probabilityScore()),
            adjustedProbability,
            status,
            status.getLabel()
        );
    }

}
