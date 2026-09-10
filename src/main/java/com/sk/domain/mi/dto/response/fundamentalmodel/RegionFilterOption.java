package com.sk.domain.mi.dto.response.fundamentalmodel;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

public record RegionFilterOption(

    @Schema(description = "Region API 값", example = "ASIA")
    String value,

    @Schema(description = "Region 화면 표시명", example = "Asia")
    String label,

    @ArraySchema(schema = @Schema(
        implementation = FilterOption.class,
        description = "실제 데이터에 존재하는 Country 옵션. 데이터 범위가 없으면 빈 배열입니다."
    ))
    List<FilterOption> countries

) {
}
