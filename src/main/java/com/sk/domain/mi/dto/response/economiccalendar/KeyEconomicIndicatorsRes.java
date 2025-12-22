package com.sk.domain.mi.dto.response.economiccalendar;

import com.sk.domain.shared.enums.EconomicUnit;
import com.sk.domain.shared.enums.UnitPrefix;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record KeyEconomicIndicatorsRes(

    @Schema(description = "경제지표분석ID", example = "1")
    Long economicIndicatorAnalysisId,

    @Schema(description = "뉴스 시장 카테고리 코드", example = "CROIL")
    String categoryCode,

    @Schema(description = "기준 일자", example = "20250729")
    String criteriaDate,

    @Schema(description = "내용", example = "Fed to announce interest rate decision...")
    List<String> content,

    @Schema(description = "경제 지표 목록")
    List<RelatedEconomicIndicator> relatedEconomicIndicators

) {

    public record RelatedEconomicIndicator(
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

        @Schema(description = "경제지표예측날짜", example = "2025-10-29")
        String economicIndicatorPredictionDay,

        @Schema(description = "경제지표발표직전값", example = "5.9")
        BigDecimal economicIndicatorPresentationPriorValue,

        @Schema(description = "경제지표예측값", example = "3.524")
        BigDecimal economicIndicatorPredictionValue,

        @Schema(description = "경제지표의 값의 단위 ( K,M 등 )", example = "Mln")
        UnitPrefix unitPrefix,

        @Schema(description = "경제지표 단위", example = "Barrel")
        EconomicUnit unit,

        @Schema(description = "Refinitiv 지표 우선순위", example = "1")
        Short refinitivPriority
    ) {

    }

}

