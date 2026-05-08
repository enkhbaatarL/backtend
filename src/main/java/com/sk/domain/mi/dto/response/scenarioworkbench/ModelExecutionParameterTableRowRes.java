package com.sk.domain.mi.dto.response.scenarioworkbench;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.Map;

public record ModelExecutionParameterTableRowRes(

    @Schema(description = "테이블 row 식별자", example = "row_001")
    String rowId,

    @Schema(description = "행 대상명", example = "카타르 LNG 공급량")
    String target,

    @Schema(description = "timePeriod별 기본 입력값", example = "{\"2025-01\":1.01,\"2025-02\":1.01}")
    Map<String, BigDecimal> values

) {
}
