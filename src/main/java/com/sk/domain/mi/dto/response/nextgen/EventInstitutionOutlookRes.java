package com.sk.domain.mi.dto.response.nextgen;

import com.sk.domain.shared.repository.master.tmm.projection.EventOutlookRow;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;

public record EventInstitutionOutlookRes(

    @Schema(description = "전망일자", example = "2026-02-28")
    LocalDate viewDate,

    @Schema(description = "기관명", example = "Goldman Sachs")
    String institutionName,

    @Schema(description = "전망 요약", example = "[북미 액화설비] Plaquemines 증강·CP2 확장 → 중기 공급 완화")
    String outlookSummary

) {

    public static EventInstitutionOutlookRes from(EventOutlookRow row) {
        return new EventInstitutionOutlookRes(
            row.viewDate(),
            row.institutionName(),
            row.outlookSummary()
        );
    }

}
