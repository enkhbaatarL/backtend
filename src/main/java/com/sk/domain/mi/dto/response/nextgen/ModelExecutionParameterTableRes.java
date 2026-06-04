package com.sk.domain.mi.dto.response.nextgen;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

public record ModelExecutionParameterTableRes(

    @Schema(description = "실행 파라미터 테이블 식별자", example = "table_monthly")
    String tableId,

    @Schema(description = "실행 파라미터 테이블명", example = "월별 공급량")
    String tableLabel,

    @Schema(description = "시간축 유형", example = "MONTHLY")
    String timeAxisType,

    @ArraySchema(schema = @Schema(description = "시간축 값", example = "2025-01"))
    List<String> timePeriods,

    @ArraySchema(schema = @Schema(implementation = ModelExecutionParameterTableRowRes.class))
    List<ModelExecutionParameterTableRowRes> rows

) {
}
