package com.sk.domain.mi.dto.request.nextgen;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record ModelExecutionTableReq(

    @NotBlank(message = "tableId는 필수입니다")
    @Schema(description = "실행 파라미터 테이블 식별자", example = "table_monthly")
    String tableId,

    @NotNull(message = "rows는 필수입니다")
    @NotEmpty(message = "rows는 최소 1건 이상이어야 합니다")
    @Valid
    @ArraySchema(schema = @Schema(implementation = ModelExecutionTableRowReq.class))
    List<ModelExecutionTableRowReq> rows

) {
}
