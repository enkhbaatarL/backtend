package com.sk.domain.shared.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.sk.domain.shared.client.dto.request.ScenarioConfirmationAgentReq;
import com.sk.domain.shared.client.dto.request.ScenarioGroupCreateV4AgentReq;
import com.sk.domain.shared.client.dto.request.ScenarioSignpostsAgentReq;
import com.sk.domain.shared.client.dto.request.ScenarioValidationAgentReq;
import com.sk.domain.shared.client.dto.response.ScenarioDesignAgentRes;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

/**
 * 시나리오 설계 Agent 통신 클라이언트 (B.E -> Agent).
 *
 * 경로는 배포된 Agent OpenAPI(2026-08-13 실측) 기준이다: 검토/Signpost/확정은 무접두 경로 그대로이며
 * v1/v4 그룹 공용이다. 무접두 POST /scenario-groups 는 v4 SSE 생성이 차지했고 구 v1 create 는 제거되어,
 * 콜백 방식 설계 시작은 콜백 호환용 POST /v4/scenario-groups (miEventIds 배열, JSON 접수 응답)를 사용한다.
 * REST 호출은 전부 이 인터페이스가 담당하고, SSE 스트림 중계만 ScenarioDesignAgentSseClient 가 담당한다.
 * 오류는 RestClientResponseException으로 전파된다 — 호출부가 용도에 맞게 처리한다.
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
    JsonNode getPresentation(@PathVariable Long scenarioGroupId, @RequestParam(value = "operationId", required = false) String operationId);

}
