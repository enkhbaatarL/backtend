package com.sk.domain.mi.dto.response.scenarioworkbench;

import com.sk.domain.shared.enums.scenarioworkbench.EventCategoryLv1;
import com.sk.domain.shared.repository.master.tmm.projection.EventListRow;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public record EventListItemRes(

    @Schema(description = "이벤트 ID", example = "ev_abc123def456")
    String eventId,

    @Schema(description = "이벤트 제목", example = "호르무즈 해협 긴장 재확대")
    String title,

    @Schema(description = "이벤트 유형 Lv1", example = "geopolitical_risk")
    EventCategoryLv1 eventCategoryLv1,

    @Schema(description = "이벤트 유형 Lv1 라벨", example = "지정학적 리스크")
    String eventCategoryLv1Label,

    @Schema(description = "생성일시")
    LocalDateTime createdAt,

    @Schema(description = "수정일시")
    LocalDateTime updatedAt,

    @Schema(description = "인덱스별 영향도 (JKM, TTF, HH)")
    List<ImpactIndexSummaryRes> indexImpacts

) {

    public static EventListItemRes from(EventListRow row) {
        EventCategoryLv1 categoryLv1 = EventCategoryLv1.fromCode(row.level1Code());

        return new EventListItemRes(
            row.miEventId(),
            row.eventTitleKo(),
            categoryLv1,
            Optional.ofNullable(categoryLv1).map(EventCategoryLv1::getLabel).orElse(null),
            row.createDt(),
            row.modifyDt(),
            row.indexImpacts().stream().map(ImpactIndexSummaryRes::from).toList()
        );
    }

}
