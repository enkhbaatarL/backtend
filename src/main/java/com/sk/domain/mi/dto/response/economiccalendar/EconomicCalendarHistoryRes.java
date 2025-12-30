package com.sk.domain.mi.dto.response.economiccalendar;

import com.sk.domain.shared.enums.EconomicUnit;
import com.sk.domain.shared.enums.UnitPrefix;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record EconomicCalendarHistoryRes(

    @Schema(description = "경제달력데이터ID", example = "1")
    Long economicCalendarId,

    @Schema(description = "RIC코드", example = "USREDY=ECI")
    String code,

    @Schema(description = "수신일자", example = "2025-10-21 00:00:00")
    LocalDateTime date,

    @Schema(description = "경제지표이름", example = "Redbook YY *")
    String economicIndicatorName,

    @Schema(description = "경제지표발표날짜", example = "2025-10-21")
    String economicIndicatorPresentationDay,

    @Schema(description = "경제지표발표시간", example = "12:55:00")
    String economicIndicatorPresentationTime,

    @Schema(description = "경제지표실제값", example = "5")
    BigDecimal economicIndicatorRealValue,

    @Schema(description = "경제지표예측날짜", example = "")
    String economicIndicatorPredictionDay,

    @Schema(description = "경제지표발표직전값", example = "5.9")
    BigDecimal economicIndicatorPresentationPriorValue,

    @Schema(description = "경제지표예측값", example = "")
    BigDecimal economicIndicatorPredictionValue,

    @Schema(description = "경제지표의 값의 단위 ( T, B, M, K )", example = "M")
    UnitPrefix unitPrefix,

    @Schema(description = "경제지표 단위", example = "Bbl")
    EconomicUnit unit,

    @Schema(description = "다음경제지표발표날짜", example = "2025-10-28")
    String nextEconomicIndicatorPresentationDay
) {


}

