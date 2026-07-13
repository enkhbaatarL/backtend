package com.sk.domain.mi.dto.response.nextgen;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

public record CurrentModelParamTableRes(

    @Schema(description = "Agent parameter payload의 모델 테이블 좌표", example = "table1")
    String tableName,

    @Schema(description = "해당 테이블에 속한 parameter/rendering leaf", requiredMode = Schema.RequiredMode.REQUIRED)
    List<CurrentModelParamLeafRes> parameters
) {
}
