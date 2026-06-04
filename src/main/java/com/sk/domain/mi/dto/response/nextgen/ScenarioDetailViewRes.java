package com.sk.domain.mi.dto.response.nextgen;

import io.swagger.v3.oas.annotations.media.Schema;
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

    @Schema(description = "시나리오 내러티브")
    String narrative,

    @Schema(description = "시나리오 발생 확률 (0~100)", example = "30")
    Integer probability,

    @Schema(description = "주요 가정 (= 차원값, 자기완결형)")
    List<ScenarioDimensionRes> dimensions,

    @Schema(description = "Signpost 정의")
    List<ScenarioSignpostDefinitionViewRes> signpostDefinitions,

    @Schema(description = "Signpost 모니터링 현황")
    List<ScenarioSignpostMonitoringRes> signpostMonitorings

) {}
