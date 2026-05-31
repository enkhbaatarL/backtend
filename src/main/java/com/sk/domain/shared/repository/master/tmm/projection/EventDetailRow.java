package com.sk.domain.shared.repository.master.tmm.projection;

import java.time.LocalDateTime;
import java.util.List;

// note: 이벤트 상세 화면 — 영향도 분석 + AI Insights 영역에 필요한 모든 데이터를 Repository에서 조립
public record EventDetailRow(
    Long miEventId,
    String eventTitle,
    LocalDateTime createDt,
    LocalDateTime modifyDt,
    String level1Code,
    String level1Label,
    String level2Code,
    String level2Label,
    Boolean isSignal,
    Boolean isNew,
    String detailAnalysis,
    String impactSummary,
    List<EventIndexImpactRow> indexImpacts,
    List<String> keyBullets,
    List<String> sources,
    List<DriverTagScoreboardRow> driverTags
) {
}
