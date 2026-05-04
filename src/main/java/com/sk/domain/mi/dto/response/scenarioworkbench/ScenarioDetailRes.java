package com.sk.domain.mi.dto.response.scenarioworkbench;

import com.sk.domain.shared.enums.scenarioworkbench.ContentStatus;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;

public record ScenarioDetailRes(

    @Schema(description = "시나리오 ID", example = "1")
    Long scenarioId,

    @Schema(description = "시나리오명", example = "호르무즈 봉쇄 3개월+ 유지")
    String scenarioName,

    @Schema(description = "상태 (NEW / UPDATED)", example = "NEW")
    ContentStatus status,

    @Schema(description = "상태 라벨", example = "신규")
    String statusLabel,

    @ArraySchema(
        arraySchema = @Schema(description = "태그 목록"),
        schema = @Schema(description = "태그", example = "장기봉쇄")
    )
    List<String> tags,

    @Schema(description = "생성일시")
    LocalDateTime createdAt,

    @Schema(description = "수정일시")
    LocalDateTime updatedAt,

    @Schema(description = "시나리오 요약 (Tab 1)")
    ScenarioSummaryRes summary,

    @Schema(description = "시나리오 주요 가정 (Tab 2)")
    List<ScenarioAssumptionRes> assumptions,

    @Schema(description = "Signpost 모니터링 현황 (Tab 3)")
    List<ScenarioSignpostRes> signposts,

    @Schema(description = "관련 이벤트 및 시나리오 (Tab 4)")
    RelatedEventScenarioRes relatedEventScenario

) {
}
