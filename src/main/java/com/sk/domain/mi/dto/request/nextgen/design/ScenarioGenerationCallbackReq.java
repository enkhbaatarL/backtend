package com.sk.domain.mi.dto.request.nextgen.design;

import com.sk.domain.mi.dto.response.nextgen.design.ScenarioDesignGroup;
import com.sk.domain.mi.dto.response.nextgen.design.ScenarioDesignTable;
import com.sk.domain.shared.enums.nextgen.AgentCallbackResult;

/**
 * [Agent 콜백] 시나리오 생성 완료 본문 (Agent → B.E).
 * Agent가 그룹/시나리오 생성 + DB 적재를 마친 뒤 호출한다(write-then-callback). stage=scenario_created.
 */
public record ScenarioGenerationCallbackReq(
    AgentCallbackResult result,
    String reason,
    ScenarioDesignGroup scenarioGroup,
    ScenarioDesignTable table
) {}
