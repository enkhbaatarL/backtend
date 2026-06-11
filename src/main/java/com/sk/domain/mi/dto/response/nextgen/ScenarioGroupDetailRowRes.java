package com.sk.domain.mi.dto.response.nextgen;

import com.sk.domain.shared.enums.nextgen.ScenarioDisplayStatus;
import com.sk.domain.shared.repository.master.tmm.projection.ScenarioBaseRow;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;


public record ScenarioGroupDetailRowRes(

    @Schema(description = "시나리오 ID", example = "11")
    Long scenarioId,

    @Schema(description = "시나리오명", example = "단기 봉쇄와 제한적 공급 대응")
    String scenarioName,

    @Schema(description = "BC(Base Case) 여부", example = "false")
    boolean baseCase,

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

    public static ScenarioGroupDetailRowRes of(
        ScenarioBaseRow row,
        List<ScenarioDimensionRes> dimensions,
        ScenarioDisplayStatus status,
        Integer adjustedProbability
    ) {
        return new ScenarioGroupDetailRowRes(
            row.scenarioId(),
            row.title(),
            Boolean.TRUE.equals(row.isBaseline()),
            dimensions,
            row.probabilityScore() == null ? null : row.probabilityScore().intValue(),
            adjustedProbability,
            status,
            status == null ? null : status.getLabel()
        );
    }

}
