package com.sk.domain.mi.dto.response.fundamentalmodel;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

public record FundamentalModelPeriodFilterOptions(

    @ArraySchema(schema = @Schema(description = "데이터가 있는 연도 옵션. value는 yyyy 형식입니다.", example = "2026"))
    List<FilterOption> yearly,

    @ArraySchema(schema = @Schema(description = "데이터가 있는 월 옵션. value는 yyyy-MM 형식입니다.", example = "2026-01"))
    List<FilterOption> monthly

) {

    public static FundamentalModelPeriodFilterOptions empty() {
        return new FundamentalModelPeriodFilterOptions(List.of(), List.of());
    }
}
