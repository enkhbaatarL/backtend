package com.sk.domain.mi.dto.response.nextgen;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

public record CurrentModelParamSheetRes(

    @Schema(description = "Agent parameter payload의 모델 시트 좌표", example = "WeatherAdjustment")
    String modelSheet,

    @Schema(description = "해당 시트에 속한 A4a 모델 테이블 tree", requiredMode = Schema.RequiredMode.REQUIRED)
    List<CurrentModelParamTableRes> modelTables
) {
}
