package com.sk.domain.mi.dto.response.nextgen;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

public record CurrentModelParamTargetRes(

    @Schema(description = "Agent payload 모델 파일 좌표", example = "Demand")
    String modelFile,

    @Schema(description = "Agent payload 모델 시트 좌표", example = "WeatherAdjustment")
    String modelSheet,

    @Schema(description = "Agent payload 모델 테이블 좌표", example = "table1")
    String tableName,

    @Schema(description = "값 field identity", example = "[\"Temperature\"]")
    List<String> valueFieldNames,

    @Schema(description = "dynamic slot-axis field identity", example = "[\"Country\", \"Year\", \"Month\"]")
    List<String> slotAxesFieldNames
) {
}
