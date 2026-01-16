package com.sk.domain.mi.dto.response.news;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

public record NewsTrendDailyRes(
    @Schema(description = "통계 기준 항목명", example = "Brent Crude Price")
    String criteriaItemName,

    @Schema(description = "일별 뉴스 트렌드 데이터")
    List<NewsTrendDailyItemRes> data
) {

    public record NewsTrendDailyItemRes(
        @Schema(description = "일자 (YYYY-MM-DD)", example = "2025-07-29")
        String date,

        @Schema(description = "상승 건수", example = "3")
        int upward,

        @Schema(description = "중립 건수", example = "5")
        int neutral,

        @Schema(description = "하락 건수", example = "2")
        int downward
    ) {
    }
}
