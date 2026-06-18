package com.sk.domain.shared.client;

import com.sk.domain.shared.client.dto.request.ScenarioConfirmationAgentReq;
import com.sk.domain.shared.client.dto.request.ScenarioGroupCreateAgentReq;
import com.sk.domain.shared.client.dto.request.ScenarioSignpostsAgentReq;
import com.sk.domain.shared.client.dto.request.ScenarioValidationAgentReq;
import com.sk.domain.shared.client.dto.response.ScenarioDesignAgentRes;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

/**
 * 시나리오 설계 Agent 통신 클라이언트 (B.E -> Agent).
 */
@HttpExchange
public interface ScenarioDesignAgentApi {

    /** 시나리오 그룹/시나리오 생성 — 설계 시작 - 단계1 진입 */
    @PostExchange("/scenario-groups")
    ScenarioDesignAgentRes createScenarioGroup(@RequestBody ScenarioGroupCreateAgentReq req);

    /** 변경사항 검토 — 검토 요청 */
    @PostExchange("/scenario-groups/{scenarioGroupId}/validation")
    ScenarioDesignAgentRes validate(@PathVariable Long scenarioGroupId, @RequestBody ScenarioValidationAgentReq req);

    /** Signpost 생성 — 단계2 진입 */
    @PostExchange("/scenario-groups/{scenarioGroupId}/signposts")
    ScenarioDesignAgentRes createSignposts(@PathVariable Long scenarioGroupId, @RequestBody ScenarioSignpostsAgentReq req);

    /** 시나리오 설계 확정 — 확정 바이패스 (콜백 없음) */
    @PostExchange("/scenario-groups/{scenarioGroupId}/confirmation")
    ScenarioDesignAgentRes confirm(@PathVariable Long scenarioGroupId, @RequestBody ScenarioConfirmationAgentReq req);

}
