package com.sk.domain.mi.dto.response.nextgen;

import com.sk.domain.shared.entity.master.tmd.ScenarioCase;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * MI Base 케이스 식별 정보 (독립수급모델 케이스).
 */
public record MiBaseRefRes(

    @Schema(description = "MI Base 케이스 ID (독립수급모델 caseId)", example = "1000")
    Long caseId,

    @Schema(description = "MI Base 케이스 타이틀", example = "독립 수급모델 테스트 1000")
    String title
) {

    public static MiBaseRefRes of(ScenarioCase scenarioCase) {
        return new MiBaseRefRes(scenarioCase.getCaseId(), scenarioCase.getTitle());
    }
}
