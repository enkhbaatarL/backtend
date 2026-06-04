package com.sk.domain.mi.dto.response.nextgen;

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

    @Schema(description = "생성일시")
    LocalDateTime createdAt,

    @Schema(description = "부모 시나리오 그룹")
    ScenarioGroupRefRes group,

    @Schema(description = "차원값 목록")
    List<ScenarioDimensionRes> dimensions,

    @Schema(description = "시나리오 발생 확률 (0~100). 라벨/등급 환산은 FE 처리", example = "30")
    Integer probability

) {

    public static ScenarioIndividualItemRes of(ScenarioGroupListItemRes group, ScenarioRowRes row) {
        return new ScenarioIndividualItemRes(
            row.scenarioId(),
            row.scenarioName(),
            row.baseCase(),
            group.createdAt(),
            new ScenarioGroupRefRes(group.scenarioGroupId(), group.groupName(), group.createdAt()),
            row.dimensions(),
            row.probability()
        );
    }

}
