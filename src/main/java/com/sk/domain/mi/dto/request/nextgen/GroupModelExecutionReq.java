package com.sk.domain.mi.dto.request.nextgen;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

/**
 * 그룹 일괄 실행 요청. 선택된 시나리오들의 확정 파라미터 묶음.
 */
public record GroupModelExecutionReq(

    @ArraySchema(schema = @Schema(implementation = GroupModelExecutionItemReq.class))
    @NotEmpty
    List<@Valid GroupModelExecutionItemReq> executions

) {
}
