package com.sk.domain.shared.client;

import com.sk.domain.shared.client.dto.request.ScenarioConfirmationAgentReq;
import com.sk.domain.shared.client.dto.request.ScenarioGroupCreateV4AgentReq;
import com.sk.domain.shared.client.dto.request.ScenarioSignpostsAgentReq;
import com.sk.domain.shared.client.dto.request.ScenarioValidationAgentReq;
import com.sk.domain.shared.client.dto.response.AdditionalDimensionsAgentRes;
import com.sk.domain.shared.client.dto.response.ScenarioDesignAgentRes;
import com.sk.domain.shared.client.dto.response.ScenarioPresentationAgentRes;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

/**
 * 시나리오 설계 Agent 통신 클라이언트 (B.E -> Agent).
 */
@HttpExchange
public interface ScenarioDesignAgentApi {

    /** 시나리오 그룹/시나리오 생성 — 설계 시작 - 단계1 진입 (콜백 호환 API, FE 의 v4 /stream 전환 후 폐기 예정) */
    @PostExchange("/v4/scenario-groups")
    ScenarioDesignAgentRes createScenarioGroup(@RequestBody ScenarioGroupCreateV4AgentReq req);

    /** 변경사항 검토 — 검토 요청 (v1/v4 그룹 공용) */
    @PostExchange("/scenario-groups/{scenarioGroupId}/validation")
    ScenarioDesignAgentRes validate(@PathVariable Long scenarioGroupId, @RequestBody ScenarioValidationAgentReq req);

    /** Signpost 생성 — 단계2 진입 (v1/v4 그룹 공용) */
    @PostExchange("/scenario-groups/{scenarioGroupId}/signposts")
    ScenarioDesignAgentRes createSignposts(@PathVariable Long scenarioGroupId, @RequestBody ScenarioSignpostsAgentReq req);

    /** 시나리오 설계 확정 — 확정 바이패스 (콜백 없음, v1/v4 그룹 공용) */
    @PostExchange("/scenario-groups/{scenarioGroupId}/confirmation")
    ScenarioDesignAgentRes confirm(@PathVariable Long scenarioGroupId, @RequestBody ScenarioConfirmationAgentReq req);

    /** 설계 화면 데이터 조회 (설계 화면 프록시 + 그룹 상세 근거 보기). operationId는 생성 진행 중 중간 스냅샷 조회용 (null 허용) */
    @GetExchange("/scenario-groups/{scenarioGroupId}/presentation")
    ScenarioPresentationAgentRes getPresentation(@PathVariable Long scenarioGroupId, @RequestParam(value = "operationId", required = false) String operationId);

    /** 추가 분기항목 후보 조회 (v4 전용). 후보는 생성 중 background로 만들어지며 이 호출은 조회 전용이다 */
    @GetExchange("/scenario-groups/{scenarioGroupId}/additional-dimensions")
    AdditionalDimensionsAgentRes getAdditionalDimensions(@PathVariable Long scenarioGroupId);

}
