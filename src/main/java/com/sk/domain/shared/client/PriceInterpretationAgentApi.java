package com.sk.domain.shared.client;

import com.sk.domain.shared.client.dto.request.PriceInterpretationAgentReq;
import com.sk.domain.shared.client.dto.response.ScenarioDesignAgentRes;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

/**
 * 시나리오별 가격전망 해석 Agent 통신 클라이언트 (B.E → Agent). (contract 잠정)
 *
 * 생성 요청은 접수(ack)만 동기로 응답하고, 실제 해석 결과는 비동기 콜백으로 전달된다
 * (write-then-callback). 설계 파이프라인과 동일한 응답 스키마(ScenarioDesignAgentRes)를 재사용한다.
 */
@HttpExchange
public interface PriceInterpretationAgentApi {

    /** 가격전망 해석 생성 요청 (대상 시나리오 일괄). */
    @PostExchange("/scenario-groups/{scenarioGroupId}/price-interpretations")
    ScenarioDesignAgentRes generate(@PathVariable Long scenarioGroupId, @RequestBody PriceInterpretationAgentReq req);

}
