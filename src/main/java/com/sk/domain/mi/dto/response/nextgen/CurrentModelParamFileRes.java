package com.sk.domain.mi.dto.response.nextgen;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

public record CurrentModelParamFileRes(

    @Schema(description = "Agent parameter payload의 모델 파일 좌표", example = "Demand")
    String modelFile,

    @Schema(description = "해당 파일에 속한 모델 시트 tree", requiredMode = Schema.RequiredMode.REQUIRED)
    List<CurrentModelParamSheetRes> modelSheets
) {
}
