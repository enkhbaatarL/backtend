package com.sk.domain.mi.dto.response.economiccalendar;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record EconomicCalendarListRes(

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

    @Schema(description = "경제지표의 값의 단위 ( K,M 등 )", example = "")
    String unitPrefix,

    @Schema(description = "경제지표 단위", example = "Percent")
    String unit,

    @Schema(description = "다음경제지표발표날짜", example = "2025-10-28")
    String nextEconomicIndicatorPresentationDay,

    @Schema(description = "Refinitiv 지표 이름", example = "United States, Chain Store Sales, Johnson Redbook Index, yoy% index, Chg Y/Y")
    String refinitivName,

    @Schema(description = "Refinitiv 지표 발표지역", example = "United States")
    String refinitivPresentationRegion,

    @Schema(description = "Refinitiv 지표 설명", example = "레드북 소매판매지수(YoY)")
    String refinitivExplanation,

    @Schema(description = "Refinitiv 지표 소스기관", example = "Redbook Research Inc.")
    String refinitivSourceOrganization,

    @Schema(description = "Refinitiv 지표 유형", example = "Sales, Orders & Inventories")
    String refinitivType,

    @Schema(description = "Refinitiv 지표 단위", example = "Percentage")
    String refinitivUnit,

    @Schema(description = "Refinitiv 지표 발표주기", example = "Monthly")
    String refinitivPresentationCycle,

    @Schema(description = "Refinitiv 지표 사용 여부", example = "true")
    Boolean refinitivIsUsed,

    @Schema(description = "Refinitiv 지표 우선순위", example = "1")
    Short refinitivPriority,

    @Schema(description = "Refinitiv 지표 발표일시", example = "2024-01-01T00:00:00")
    String refinitivPresentationDatetime,

    @Schema(description = "국가 코드 (Alpha-2)", example = "US")
    String countryNameCode,

    @Schema(description = "통화 코드", example = "USD")
    String countryCurrencyCode
) {


}
