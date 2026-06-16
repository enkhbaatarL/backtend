package com.sk.domain.mi.dto.request.nextgen.design;

import com.sk.domain.mi.dto.response.nextgen.design.ScenarioDesignTable;

/**
 * [Agent 콜백] 시나리오 검토 완료 본문 (Agent → B.E).
 * isValid=false면 stage 유지 + reason 전달. table은 편집 reconcile 결과(신규 채번 포함).
 */
public record ScenarioValidationCallbackReq(
    Long scenarioGroupId,
    Boolean isValid,
    String reason,
    String status,
    String stage,
    ScenarioDesignTable table
) {}
