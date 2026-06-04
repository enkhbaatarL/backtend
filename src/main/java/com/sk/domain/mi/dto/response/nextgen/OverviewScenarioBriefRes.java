package com.sk.domain.mi.dto.response.nextgen;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.List;

public record OverviewScenarioBriefRes(

    @Schema(description = "시나리오 ID", example = "1")
    Long scenarioId,

    @Schema(description = "시나리오명", example = "호르무즈 봉쇄 3개월+ 유지")
    String scenarioName,

    @Schema(description = "BC(Base Case) 여부", example = "true")
    boolean baseCase,

    @Schema(description = "인덱스별 영향도 (impactLevel only)")
    List<ScenarioImpactIndexRes> indexImpacts,

    @Schema(description = "발생 가능성 (0.00 ~ 1.00)", example = "0.325")
    BigDecimal probability

) {}
