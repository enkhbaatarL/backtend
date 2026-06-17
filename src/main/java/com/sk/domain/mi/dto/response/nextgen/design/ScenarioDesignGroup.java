package com.sk.domain.mi.dto.response.nextgen.design;

import java.util.List;

/**
 * 시나리오 그룹 메타.
 * Agent 콜백 본문(생성/ Signpost 완료)에 포함된다.
 */
public record ScenarioDesignGroup(
    Long scenarioGroupId,
    Long miEventId,
    Long eventTypeL2Id,
    String title,
    String summary,
    String description,
    List<String> dimensionSelected,
    Integer scenarioCount,
    String status,
    String stage
) {}
