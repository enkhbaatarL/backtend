package com.sk.domain.mi.dto.response.scenarioworkbench;

import com.sk.domain.shared.enums.scenarioworkbench.EventCategoryLv1;
import com.sk.domain.shared.enums.scenarioworkbench.EventCategoryLv2;
import com.sk.domain.shared.repository.master.tmm.projection.EventDetailRow;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.Optional;

public record EventImpactAnalysisRes(

    @Schema(description = "Index별 영향도 요약 (JKM, TTF, Henry Hub)")
    List<ImpactIndexSummaryRes> indexImpacts,

    @Schema(description = "영향도 분석 텍스트")
    String analysisText,

    @Schema(description = "이벤트 유형 Lv1", example = "geopolitical_risk")
    EventCategoryLv1 categoryLv1,

    @Schema(description = "이벤트 유형 Lv1 라벨", example = "지정학적 리스크")
    String categoryLv1Label,

    @Schema(description = "이벤트 유형 Lv2", example = "war_military_conflict")
    EventCategoryLv2 categoryLv2,

    @Schema(description = "이벤트 유형 Lv2 라벨", example = "전쟁 / 군사 분쟁")
    String categoryLv2Label,

    @ArraySchema(schema = @Schema(description = "출처", example = "Reuters"))
    List<String> sources

) {

    public static EventImpactAnalysisRes from(EventDetailRow row) {
        EventCategoryLv1 lv1 = EventCategoryLv1.fromCode(row.level1Code());
        EventCategoryLv2 lv2 = EventCategoryLv2.fromCode(row.level2Code());

        return new EventImpactAnalysisRes(
            row.indexImpacts().stream().map(ImpactIndexSummaryRes::from).toList(),
            row.impactAnalysisText(),
            lv1,
            Optional.ofNullable(lv1).map(EventCategoryLv1::getLabel).orElse(null),
            lv2,
            Optional.ofNullable(lv2).map(EventCategoryLv2::getLabel).orElse(null),
            row.sources()
        );
    }

}
