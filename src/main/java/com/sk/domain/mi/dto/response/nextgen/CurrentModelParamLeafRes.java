package com.sk.domain.mi.dto.response.nextgen;

import io.swagger.v3.oas.annotations.media.Schema;

public record CurrentModelParamLeafRes(

    @Schema(description = "A6 confirmationParameters 원소로 사용할 Agent parameter shape", requiredMode = Schema.RequiredMode.REQUIRED)
    CurrentModelParameterRes parameter,

    @Schema(description = "해당 parameter의 화면 렌더링 정보", requiredMode = Schema.RequiredMode.REQUIRED)
    CurrentModelParamRenderingRes rendering
) {
}
