package com.sk.domain.mi.dto.response.nextgen;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record GeneratedScenarioRes(

    @Schema(description = "시나리오 ID", example = "101")
    Long scenarioId,

    @Schema(description = "시나리오명", example = "1~2주 내 협상 완료 상황 해소")
    String scenarioName,

    @Schema(description = "BC 배지 여부", example = "false")
    boolean baseCase,

    @Schema(description = "태그 배지")
    List<String> tags,

    @Schema(description = "생성일시")
    LocalDateTime createdAt,

    @Schema(description = "인덱스별 영향도 (JKM, TTF, HH)")
    List<ScenarioImpactIndexRes> indexImpacts,

    @Schema(description = "발생 가능성", example = "0.35")
    BigDecimal probability

) {
}
