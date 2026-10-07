package com.sk.domain.shared.repository.master.tmm.projection;

import com.querydsl.core.annotations.QueryProjection;
import java.time.LocalDateTime;

/**
 * 시나리오 그룹을 구성하는 이벤트 (Base Event 리스트 한 행, display_order 순).
 */
public record ScenarioGroupBaseEventRow(
    Long eventId,
    String eventName,
    String eventTypeLv1Code,
    String eventTypeLv1Label,
    Boolean isScenarioSuggested,
    LocalDateTime latestRecordFirstCreated,
    String createLoginUserId,
    String modifyLoginUserId
) {

    @QueryProjection
    public ScenarioGroupBaseEventRow {
    }

}
