package com.sk.domain.mi.dto.response.nextgen;

import com.sk.domain.shared.repository.master.tmm.projection.ScenarioGroupDetailBaseRow;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

/**
 * 시나리오 그룹이 파생된 기반 이벤트 (그룹 상세 요약 탭의 Base Event).
 */
public record ScenarioBaseEventRes(

    @Schema(description = "이벤트 ID", example = "1")
    Long eventId,

    @Schema(description = "이벤트명", example = "카타르 Ras Laffan 피해 (전쟁)")
    String eventName,

    @Schema(description = "이벤트 유형 Lv1", example = "exogenous_shock")
    String eventTypeLv1,

    @Schema(description = "이벤트 유형 Lv1 라벨", example = "외생충격")
    String eventTypeLv1Label,

    @Schema(description = "업데이트 일시")
    LocalDateTime updatedAt

) {

    public static ScenarioBaseEventRes from(ScenarioGroupDetailBaseRow row) {
        return new ScenarioBaseEventRes(
            row.eventId(),
            row.eventName(),
            row.eventTypeLv1Code(),
            row.eventTypeLv1Label(),
            row.eventUpdatedAt()
        );
    }

}
