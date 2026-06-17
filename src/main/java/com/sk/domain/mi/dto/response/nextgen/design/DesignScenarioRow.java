package com.sk.domain.mi.dto.response.nextgen.design;

import com.sk.domain.mi.dto.response.nextgen.ScenarioDimensionRes;
import com.sk.domain.mi.dto.response.nextgen.ScenarioSignpostDefinitionViewRes;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * 설계 세션 시나리오 표의 한 행 (FE 컨벤션). 조회 응답과 편집 요청(검토/단계2/확정)이 동일 구조로 공유한다.
 * step1/step2 공용 — 단계 진행에 따라 채워지는 필드가 늘어난다(step1은 narrative/probability/signposts가 null).
 */
public record DesignScenarioRow(

    @Schema(description = "시나리오 ID (신규 추가 행이면 null)", example = "90001")
    Long scenarioId,

    @Schema(description = "신규 추가(미채번) 행 여부", example = "false")
    boolean isNew,

    @Schema(description = "표시 순서", example = "1")
    Integer displayOrder,

    @Schema(description = "시나리오명", example = "단기 외교 해결")
    String scenarioName,

    @Schema(description = "BC(Base Case) 여부", example = "false")
    boolean baseCase,

    @Schema(description = "차원값 목록 (dimensionColumns 순서와 매칭)")
    List<ScenarioDimensionRes> dimensions,

    @Schema(description = "시나리오 발생 확률 (0~100, 원본). step2 산출 전이면 null", example = "75")
    Integer probability,

    @Schema(description = "시나리오 내러티브 (step2 산출). 이전 단계면 null")
    String narrative,

    @Schema(description = "시나리오별 Signpost (step2 산출). 이전 단계면 null/빈 목록")
    List<ScenarioSignpostDefinitionViewRes> signposts

) {

    public static DesignScenarioRow of(Long scenarioId, Integer displayOrder, String scenarioName,
                                       boolean baseCase, List<ScenarioDimensionRes> dimensions,
                                       Integer probability, String narrative,
                                       List<ScenarioSignpostDefinitionViewRes> signposts) {
        return new DesignScenarioRow(
            scenarioId,
            scenarioId == null,
            displayOrder,
            scenarioName,
            baseCase,
            dimensions,
            probability,
            narrative,
            signposts
        );
    }

}
