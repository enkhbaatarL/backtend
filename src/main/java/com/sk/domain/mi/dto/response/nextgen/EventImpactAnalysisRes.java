package com.sk.domain.mi.dto.response.nextgen;

import com.sk.domain.shared.repository.master.tmm.projection.EventDetailRow;
import com.sk.domain.shared.repository.master.tmm.projection.EventIndexImpactRow;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

public record EventImpactAnalysisRes(

    @Schema(description = "인덱스별 영향도 (JKM, TTF, HH)")
    List<EventDetailImpactIndexRes> indexImpacts,

    @Schema(description = "전체 영향도 종합 텍스트", nullable = true)
    String analysisText,

    @Schema(description = "이벤트 유형 Lv1", example = "exogenous_shock")
    String eventTypeLv1,

    @Schema(description = "이벤트 유형 Lv1 라벨", example = "외생충격")
    String eventTypeLv1Label,

    @Schema(description = "이벤트 유형 Lv2", example = "geopolitical_tension_war")
    String eventTypeLv2,

    @Schema(description = "이벤트 유형 Lv2 라벨", example = "지정학적 긴장 및 전쟁·군사분쟁")
    String eventTypeLv2Label,

    @ArraySchema(
        arraySchema = @Schema(description = "분석에 사용된 출처(provider) 목록"),
        schema = @Schema(example = "Reuters")
    )
    List<String> sources

) {

    public static EventImpactAnalysisRes from(EventDetailRow row) {
        return new EventImpactAnalysisRes(
            buildIndexImpacts(row.indexImpacts()),
            row.impactSummary(),
            row.level1Code(),
            row.level1Label(),
            row.level2Code(),
            row.level2Label(),
            row.sources()
        );
    }

    private static List<EventDetailImpactIndexRes> buildIndexImpacts(List<EventIndexImpactRow> rows) {
        return rows.stream()
            .map(EventDetailImpactIndexRes::from)
            .toList();
    }

}
