package com.sk.domain.mi.dto.response.scenarioworkbench;

import com.sk.domain.shared.enums.scenarioworkbench.ContentStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record ScenarioListItemRes(

    @Schema(description = "시나리오 ID", example = "1")
    Long scenarioId,

    @Schema(description = "시나리오명", example = "호르무즈 봉쇄 3개월+ 유지")
    String scenarioName,

    @Schema(description = "상태 (NEW / UPDATED)", example = "NEW")
    ContentStatus status,

    @Schema(description = "상태 라벨", example = "신규")
    String statusLabel,

    @Schema(description = "생성일시")
    LocalDateTime createdAt,

    @Schema(description = "수정일시")
    LocalDateTime updatedAt,

    @Schema(description = "BC(Base Case) 여부", example = "false")
    boolean baseCase,

    @Schema(description = "인덱스별 영향도 (JKM, TTF, HH)")
    List<ScenarioImpactIndexRes> indexImpacts,

    @Schema(description = "발생 가능성 (0.00 ~ 1.00)", example = "0.325")
    BigDecimal probability

) {
}
