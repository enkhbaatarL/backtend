package com.sk.domain.mi.dto.response.nextgen;

import com.sk.domain.shared.enums.nextgen.ScenarioDisplayStatus;
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

    public static ScenarioIndividualItemRes of(ScenarioGroupListItemRes group, ScenarioRowRes row) {
        return new ScenarioIndividualItemRes(
            row.scenarioId(),
            row.scenarioName(),
            row.baseCase(),
            row.displayOrder(),
            // mock 전용 경로: mock 데이터에 시나리오 자체 일시가 없어 그룹 일시로 대체
            group.createdAt(),
            group.updatedAt(),
            new ScenarioGroupRefRes(group.scenarioGroupId(), group.groupName(), group.createdAt()),
            row.dimensions(),
            row.probability(),
            row.adjustedProbability(),
            row.status(),
            row.statusLabel()
        );
    }

    /** 개별 모드 base row + 조립된 dimensions → 응답 행. */
    public static ScenarioIndividualItemRes of(ScenarioIndividualBaseRow row, List<ScenarioDimensionRes> dimensions,
                                               Integer adjustedProbability) {
        ScenarioDisplayStatus status = ScenarioDisplayStatus.from(row.status());
        return new ScenarioIndividualItemRes(
            row.scenarioId(),
            row.scenarioTitle(),
            Boolean.TRUE.equals(row.isBaseline()),
            row.displayOrder(),
            row.createdAt(),
            row.updatedAt(),
            new ScenarioGroupRefRes(row.scenarioGroupId(), row.groupTitle(), row.groupCreatedAt()),
            dimensions,
            row.probabilityScore() == null ? null : row.probabilityScore().intValue(),
            adjustedProbability,
            status,
            status.getLabel()
        );
    }

}
