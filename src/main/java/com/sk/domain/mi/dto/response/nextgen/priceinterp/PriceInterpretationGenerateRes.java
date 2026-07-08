package com.sk.domain.mi.dto.response.nextgen.priceinterp;

import com.sk.domain.shared.enums.nextgen.PriceInterpretationStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * 가격전망 해석 생성 접수 응답.
 * 대상 시나리오별 접수 상태를 함께 내린다 — FE는 재조회 없이 이 응답만으로 대상 탭들을 생성중으로 렌더링할 수 있다.
 */
public record PriceInterpretationGenerateRes(

    @Schema(description = "시나리오 그룹 ID", example = "1")
    Long scenarioGroupId,

    @Schema(description = "생성 대상 시나리오 목록")
    List<TargetScenario> targetScenarios

) {

    public record TargetScenario(

        @Schema(description = "시나리오 ID", example = "11")
        Long scenarioId,

        @Schema(description = "접수 시점 상태 (항상 generating)", example = "generating")
        PriceInterpretationStatus status,

        @Schema(description = "상태 라벨", example = "생성중")
        String statusLabel

    ) {}

    public static PriceInterpretationGenerateRes of(Long scenarioGroupId, List<Long> targetScenarioIds) {
        List<TargetScenario> targetScenarios = targetScenarioIds.stream()
            .map(scenarioId -> new TargetScenario(
                scenarioId,
                PriceInterpretationStatus.GENERATING,
                PriceInterpretationStatus.GENERATING.getLabel()))
            .toList();
        return new PriceInterpretationGenerateRes(scenarioGroupId, targetScenarios);
    }

}
