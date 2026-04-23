package com.sk.domain.mi.dto.response.scenarioworkbench;

import com.sk.domain.shared.enums.scenarioworkbench.EventStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

public record EventDetailRes(

    @Schema(description = "이벤트 ID", example = "ev_8f3a91c2b7d0")
    String eventId,

    @Schema(description = "이벤트 제목")
    String title,

    @Schema(description = "이벤트 상태")
    EventStatus eventStatus,

    @Schema(description = "생성일시")
    LocalDateTime createdAt,

    @Schema(description = "업데이트일시")
    LocalDateTime updatedAt,

    @Schema(description = "영향도 분석")
    EventImpactAnalysisRes impactAnalysis,

    @Schema(description = "AI Insights")
    EventAiInsightsRes aiInsights,

    @Schema(description = "Fundamental Deep-dive")
    EventFundamentalRes fundamentalDeepdive

) {
}
