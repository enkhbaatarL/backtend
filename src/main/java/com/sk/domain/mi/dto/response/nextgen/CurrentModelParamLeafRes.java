package com.sk.domain.mi.dto.response.nextgen;

import io.swagger.v3.oas.annotations.media.Schema;

public record CurrentModelParamLeafRes(

    @Schema(
        description = "Agent parameter data에 response-only groupedRationales를 추가한 tree response shape. A6 전송 전 groupedRationales 제거 필요",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    CurrentModelParameterRes parameter,

    @Schema(description = "해당 parameter의 화면 렌더링 정보", requiredMode = Schema.RequiredMode.REQUIRED)
    CurrentModelParamRenderingRes rendering
) {
}
