package com.sk.domain.mi.dto.request.nextgen;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.Map;

public record ModelExecutionTableRowReq(

    @NotBlank(message = "rowId는 필수입니다")
    @Schema(description = "테이블 row 식별자", example = "row_001")
    String rowId,

    @NotNull(message = "values는 필수입니다")
    @NotEmpty(message = "values는 최소 1건 이상이어야 합니다")
    @Schema(description = "timePeriod별 입력값", example = "{\"2025-01\":1.05,\"2025-02\":1.01}")
    Map<String, BigDecimal> values

) {
}
