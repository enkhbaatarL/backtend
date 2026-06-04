package com.sk.domain.mi.dto.response.nextgen;

import com.sk.domain.shared.repository.master.tmm.projection.EventDetailRow;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

public record EventAiInsightsRes(

    @Schema(description = "핵심요약")
    List<String> keySummaries,

    @Schema(description = "상세분석")
    String detailedAnalysis

) {

    public static EventAiInsightsRes from(EventDetailRow row) {
        return new EventAiInsightsRes(
            row.keyBullets(),
            row.detailAnalysis()
        );
    }

}
