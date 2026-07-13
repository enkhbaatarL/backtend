package com.sk.domain.shared.client.dto.request;

import java.util.List;

/**
 * 시나리오별 가격전망 해석 생성 요청 (B.E → Agent). (contract 잠정)
 *
 * 대상 시나리오(수급모델 실행 완료 + 해석 미보유)들의 해석 생성을 요청한다.
 * Agent는 비동기로 해석을 산출한 뒤 콜백으로 전달한다.
 */
public record PriceInterpretationAgentReq(
    Long scenarioGroupId,
    Long requestUserId,
    List<String> scenarioIds
) {

    public static PriceInterpretationAgentReq of(Long scenarioGroupId, Long requestUserId, List<Long> scenarioIds) {
        List<String> scenarioIdTexts = scenarioIds == null
            ? List.of()
            : scenarioIds.stream().map(String::valueOf).toList();
        return new PriceInterpretationAgentReq(scenarioGroupId, requestUserId, scenarioIdTexts);
    }

}
