package com.sk.domain.mi.dto.response.nextgen;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

public record ModelSheetNode(

    @Schema(description = "모델 시트명", example = "PowerMixAdjustment")
    String modelSheet,

    @Schema(description = "해당 시트에 속한 테이블명 목록", example = "[\"table1\"]")
    List<String> tableNames,

    @Schema(description = "해당 시트에 속한 테이블 목록 (tableNames와 동일 순서, 화면 표시명 포함)")
    List<ModelTableNode> tables
) {
}
