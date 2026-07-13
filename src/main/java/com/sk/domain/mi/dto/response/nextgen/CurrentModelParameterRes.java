package com.sk.domain.mi.dto.response.nextgen;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.Map;

public record CurrentModelParameterRes(

    @Schema(description = "수급모델 파라미터명", example = "Temperature")
    String parameterName,

    @Schema(description = "Agent parameter의 모델 좌표와 field identity", requiredMode = Schema.RequiredMode.REQUIRED)
    CurrentModelParamTargetRes modelTarget,

    @Schema(description = "Agent parameter-level rationale")
    String rationale,

    @Schema(
        description = "화면 표시용 response-only rationale grouping. dynamic slot-axis field와 결합 rationale을 포함하며 A6 request에서는 제외",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    List<Map<String, Object>> groupedRationales,

    @Schema(description = "dynamic slot-axis key를 보존하는 Agent parameter value rows", requiredMode = Schema.RequiredMode.REQUIRED)
    List<Map<String, Object>> values
) {
}
