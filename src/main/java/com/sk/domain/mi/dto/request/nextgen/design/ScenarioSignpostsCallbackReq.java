package com.sk.domain.mi.dto.request.nextgen.design;

import com.sk.domain.mi.dto.response.nextgen.design.ScenarioDesignGroup;
import com.sk.domain.mi.dto.response.nextgen.design.ScenarioDesignTable;

/**
 * [Agent 콜백] Signpost 생성 완료 본문 (Agent → B.E).
 * table.rows[].signposts에 시나리오별 Signpost가 포함된다.
 */
public record ScenarioSignpostsCallbackReq(
    ScenarioDesignGroup scenarioGroup,
    ScenarioDesignTable table
) {}
