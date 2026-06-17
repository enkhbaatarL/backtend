package com.sk.domain.mi.dto.request.nextgen.design;

import com.sk.domain.mi.dto.response.nextgen.design.ScenarioDesignGroup;
import com.sk.domain.mi.dto.response.nextgen.design.ScenarioDesignTable;
import com.sk.domain.shared.enums.nextgen.AgentCallbackResult;

/**
 * [Agent 콜백] Signpost 생성 완료 본문 (Agent → B.E).
 * Agent가 Signpost 생성 + DB 적재를 마친 뒤 호출한다(write-then-callback). stage=signpost_created.
 * table.rows[].signposts에 시나리오별 Signpost가 포함된다.
 */
public record ScenarioSignpostsCallbackReq(
    AgentCallbackResult result,
    String reason,
    ScenarioDesignGroup scenarioGroup,
    ScenarioDesignTable table
) {}
