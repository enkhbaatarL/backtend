package com.sk.domain.mi.dto.response.nextgen;

import com.sk.domain.shared.enums.nextgen.BasecaseReadMode;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

public record CurrentModelParamRenderingRes(

    @Schema(description = "표 렌더링 방식", implementation = BasecaseReadMode.class, example = "FIXED_COLUMNS")
    BasecaseReadMode readMode,

    @Schema(description = "표 컬럼 역할과 정렬 정보", requiredMode = Schema.RequiredMode.REQUIRED)
    List<ModelParamColumn> columns,

    @Schema(description = "의미 축 설명")
    String semanticAxes
) {
}
