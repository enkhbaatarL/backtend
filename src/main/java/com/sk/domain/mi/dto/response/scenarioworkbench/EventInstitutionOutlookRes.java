package com.sk.domain.mi.dto.response.scenarioworkbench;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

public record EventInstitutionOutlookRes(

    @Schema(description = "게시일시")
    LocalDateTime publishedAt,

    @Schema(description = "기관명", example = "Uniper")
    String institutionName,

    @Schema(description = "전망 요약")
    String outlookSummary

) {
}
