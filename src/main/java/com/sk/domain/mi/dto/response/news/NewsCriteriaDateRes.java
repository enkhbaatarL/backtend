package com.sk.domain.mi.dto.response.news;

import io.swagger.v3.oas.annotations.media.Schema;

public record NewsCriteriaDateRes(
    @Schema(description = "기준 일자 (yyyyMMdd)", example = "20250729")
    String criteriaDate,

    @Schema(description = "다음 업데이트 시각 (yyyy-MM-dd HH:mm)", example = "2025-07-30 10:00")
    String nextUpdate
) {
}
