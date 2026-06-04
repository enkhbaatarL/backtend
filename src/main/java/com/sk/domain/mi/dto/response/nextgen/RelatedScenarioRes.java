package com.sk.domain.mi.dto.response.nextgen;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.List;

public record RelatedScenarioRes(

    @Schema(description = "시나리오 ID", example = "2")
    Long scenarioId,

    @Schema(description = "시나리오명", example = "1~2주 내 협상 완료 상황 해소")
    String scenarioName,

    @Schema(description = "베이스케이스 여부 (BC 배지)", example = "false")
    boolean baseCase,

    @Schema(description = "인덱스별 영향도 (JKM, TTF, HH)")
    List<ScenarioImpactIndexRes> indexImpacts,

    @Schema(description = "발생 가능성 (0.00 ~ 1.00)", example = "0.325")
    BigDecimal probability

) {
}
