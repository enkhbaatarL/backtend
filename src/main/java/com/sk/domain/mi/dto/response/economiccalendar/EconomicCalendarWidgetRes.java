package com.sk.domain.mi.dto.response.economiccalendar;

import com.sk.domain.shared.enums.EconomicUnit;
import com.sk.domain.shared.enums.UnitPrefix;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.List;

public record EconomicCalendarWidgetRes(

    @Schema(description = "최종 업데이트 시간", example = "Sep 15 09:45")
    String updateTime,

    @Schema(description = "경제 지표 목록")
    List<EconomicCalendarWidget> economicCalendarWidgetList

) {
    public record EconomicCalendarWidget(
        @Schema(description = "경제달력데이터ID", example = "1")
    Long economicCalendarId,

    @Schema(description = "RIC코드", example = "USREDY=ECI")
    String code,

    @Schema(description = "경제지표이름", example = "Redbook YY *")
    String economicIndicatorName,

    @Schema(description = "발표날짜", example = "20251021")
    String economicIndicatorPresentationDay,

    @Schema(description = "발표시간", example = "12:55:00")
    String economicIndicatorPresentationTime,

    @Schema(description = "실제값", example = "5")
    BigDecimal economicIndicatorRealValue,

    @Schema(description = "직전값", example = "5.9")
    BigDecimal economicIndicatorPresentationPriorValue,

    @Schema(description = "예측값", example = "")
    BigDecimal economicIndicatorPredictionValue,

    @Schema(description = "경제지표의 값의 단위 ( T, B, M, K )", example = "M")
    UnitPrefix unitPrefix,

    @Schema(description = "경제지표 단위", example = "Bbl")
    EconomicUnit unit,

    @Schema(description = "Refinitiv 지표 우선순위", example = "1")
    Short refinitivPriority,

    @Schema(description = "Refinitiv type", example = "Consumer Prices / Inflation")
    String refinitivType,

    @Schema(description = "발표 상태 (ANNOUNCED: 발표됨, SCHEDULED: 발표 예정)", example = "ANNOUNCED")
    String status
    ) {

    }

}
