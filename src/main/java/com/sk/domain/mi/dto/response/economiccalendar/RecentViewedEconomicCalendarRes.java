package com.sk.domain.mi.dto.response.economiccalendar;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "최근 조회 경제 달력 응답")
public record RecentViewedEconomicCalendarRes(

    @Schema(description = "지표코드", example = "USREDY=ECI")
    String code,

    @Schema(description = "경제지표이름", example = "Redbook YY *")
    String economicIndicatorName,

    @Schema(description = "사용자가 조회한 일시", example = "2026-01-12T15:20:15")
    LocalDateTime inquiryDateTime,

    @Schema(description = "Refinitiv 지표 소스기관", example = "Redbook Research Inc.")
    String refinitivSourceOrganization
) {
}
