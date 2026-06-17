package com.sk.domain.mi.dto.response.nextgen.design;

/**
 * 검토 결과 휘발 캐시 (Redis). DB에 저장하지 않는다.
 * <p>
 * 검토 콜백이 전달한 reconcile table(Agent 자동생성값 포함)과 정합성 결과를 임시 보관한다.
 * GET /design가 step1 위에 오버레이하고, reset/확정 시 삭제된다.
 */
public record ScenarioValidationCache(
    Boolean isValid,
    String reason,
    ScenarioDesignTable table
) {

    public static ScenarioValidationCache of(Boolean isValid, String reason, ScenarioDesignTable table) {
        return new ScenarioValidationCache(isValid, reason, table);
    }

}
