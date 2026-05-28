package com.sk.domain.mi.dto.response.scenarioworkbench;

import io.swagger.v3.oas.annotations.media.Schema;

// 이벤트 상세 화면 — 인덱스별 영향도. 단기·중장기를 nested 슬롯으로 표현
public record EventDetailImpactIndexRes(

    @Schema(description = "인덱스명", example = "JKM")
    String indexName,

    @Schema(description = "단기 영향 (없으면 null)")
    EventImpactIndexTermRes shortTerm,

    @Schema(description = "중장기 영향 (없으면 null)")
    EventImpactIndexTermRes midLongTerm,

    @Schema(description = "해당 인덱스에 대한 영향 분석 본문 (tmm_impactresultm.impact_assessment_reason)")
    String analysisText

) {
}
