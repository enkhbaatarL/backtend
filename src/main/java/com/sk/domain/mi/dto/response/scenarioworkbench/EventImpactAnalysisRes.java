package com.sk.domain.mi.dto.response.scenarioworkbench;

import com.sk.domain.shared.repository.master.tmm.projection.EventDetailRow;
import com.sk.domain.shared.repository.master.tmm.projection.EventIndexImpactRow;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public record EventImpactAnalysisRes(

    @Schema(description = "인덱스별 영향도 (JKM, TTF, HH) — 인덱스마다 단기·중장기(shortTerm/midLongTerm) 슬롯과 인덱스별 분석 본문(analysisText) 포함")
    List<EventDetailImpactIndexRes> indexImpacts,

    @Schema(description = "전체 영향도 종합 텍스트. **mock 응답 전용** — 실 쿼리는 인덱스별 본문만 제공하므로 항상 null.",
        nullable = true)
    String analysisText,

    @Schema(description = "이벤트 유형 Lv1", example = "geopolitical_risk")
    String eventTypeLv1,

    @Schema(description = "이벤트 유형 Lv1 라벨", example = "지정학적 리스크")
    String eventTypeLv1Label,

    @Schema(description = "이벤트 유형 Lv2", example = "war_military_conflict")
    String eventTypeLv2,

    @Schema(description = "이벤트 유형 Lv2 라벨", example = "전쟁 / 군사 분쟁")
    String eventTypeLv2Label,

    @ArraySchema(
        arraySchema = @Schema(description = "분석에 사용된 출처(provider) 목록"),
        schema = @Schema(example = "Reuters")
    )
    List<String> sources

) {

    public static EventImpactAnalysisRes from(EventDetailRow row) {
        return new EventImpactAnalysisRes(
            buildIndexImpacts(row.indexImpacts(), row.indexAnalysisTexts()),
            null,
            row.level1Code(),
            row.level1Label(),
            row.level2Code(),
            row.level2Label(),
            row.sources()
        );
    }

    private static List<EventDetailImpactIndexRes> buildIndexImpacts(
        List<EventIndexImpactRow> rows,
        Map<String, String> indexAnalysisTexts
    ) {
        Set<String> orderedIndexes = new LinkedHashSet<>();
        rows.forEach(r -> orderedIndexes.add(r.targetId()));
        if (indexAnalysisTexts != null) {
            orderedIndexes.addAll(indexAnalysisTexts.keySet());
        }

        return orderedIndexes.stream()
            .map(indexName -> new EventDetailImpactIndexRes(
                indexName,
                null,
                null,
                indexAnalysisTexts == null ? null : indexAnalysisTexts.get(indexName)
            ))
            .toList();
    }

}
