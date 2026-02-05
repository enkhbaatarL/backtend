package com.sk.domain.mi.dto.response.forecast.summary;

import com.sk.common.model.PaginationRes;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "Forecast record 테이블 응답")
public record ForecastRecordTableRes(
    @Schema(description = "페이지 정보")
    PaginationRes<ForecastRecordRes> page,

    @Schema(description = "헤더 목록")
    List<ForecastRecordHeaderRes> headers
) {
}
