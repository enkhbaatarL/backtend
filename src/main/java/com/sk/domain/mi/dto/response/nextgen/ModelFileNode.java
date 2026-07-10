package com.sk.domain.mi.dto.response.nextgen;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

public record ModelFileNode(

    @Schema(description = "모델 파일명", example = "Demand")
    String modelFile,

    @Schema(description = "해당 파일에 속한 모델 시트 목록")
    List<ModelSheetNode> modelSheets
) {
}
