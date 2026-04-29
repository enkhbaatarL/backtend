package com.sk.domain.mi.dto.response.scenarioworkbench;

import com.sk.domain.shared.repository.master.tmm.projection.EventDetailRow;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

public record EventDetailRes(

    @Schema(description = "이벤트 ID", example = "1")
    Long eventId,

    @Schema(description = "이벤트 제목")
    String title,

    @Schema(description = "생성일시")
    LocalDateTime createdAt,

    @Schema(description = "업데이트일시")
    LocalDateTime updatedAt,

    @Schema(description = "영향도 분석")
    EventImpactAnalysisRes impactAnalysis,

    @Schema(description = "AI Insights")
    EventAiInsightsRes aiInsights,

    @Schema(description = "Fundamental Deep-dive (테이블 미준비 상태이므로 현재는 null)")
    EventFundamentalRes fundamentalDeepdive

) {

    public static EventDetailRes from(EventDetailRow row) {
        return new EventDetailRes(
            row.miEventId(),
            row.eventTitleKo(),
            row.createDt(),
            row.modifyDt(),
            EventImpactAnalysisRes.from(row),
            EventAiInsightsRes.from(row),
            null
        );
    }

}
