package com.sk.domain.mi.dto.response.scenarioworkbench;

import com.sk.domain.shared.enums.scenarioworkbench.EventCategoryLv1;
import com.sk.domain.shared.enums.scenarioworkbench.EventCategoryLv2;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

public record EventImpactAnalysisRes(

    @Schema(description = "Index별 영향도 요약 (JKM, TTF, Henry Hub)")
    List<ImpactIndexSummaryRes> indexImpacts,

    @Schema(description = "영향도 분석 텍스트")
    String analysisText,

    @Schema(description = "이벤트 유형 Lv1")
    EventCategoryLv1 categoryLv1,

    @Schema(description = "이벤트 유형 Lv2")
    EventCategoryLv2 categoryLv2,

    @ArraySchema(schema = @Schema(description = "출처", example = "Reuters"))
    List<String> sources

) {
}
