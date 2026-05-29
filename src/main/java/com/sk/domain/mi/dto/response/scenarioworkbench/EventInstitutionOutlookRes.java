package com.sk.domain.mi.dto.response.scenarioworkbench;

import com.sk.domain.shared.repository.master.tmm.projection.EventOutlookRow;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

public record EventInstitutionOutlookRes(

    @Schema(description = "게시일시")
    LocalDateTime publishedAt,

    @Schema(description = "기관명 (Goldman Sachs, IEA, Wood Mackenzie 등)", example = "Goldman Sachs")
    String institutionName,

    @Schema(description = "전망 요약 (1~2줄)", example = "[북미 액화설비] Plaquemines 증강·CP2 확장 → 중기 공급 완화")
    String outlookSummary

) {

    public static EventInstitutionOutlookRes from(EventOutlookRow row) {
        return new EventInstitutionOutlookRes(
            row.getPublishedAt(),
            row.getInstitutionName(),
            row.getOutlookSummary()
        );
    }

}
