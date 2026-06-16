package com.sk.domain.mi.dto.request.nextgen.design;

import com.sk.domain.mi.dto.response.nextgen.design.ScenarioDesignGroup;
import com.sk.domain.mi.dto.response.nextgen.design.ScenarioDesignTable;

/**
 * [Agent 콜백] 시나리오 생성 완료 본문 (Agent → B.E).
 */
public record ScenarioGenerationCallbackReq(
    ScenarioDesignGroup scenarioGroup,
    ScenarioDesignTable table
) {}
