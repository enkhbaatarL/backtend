package com.sk.domain.mi.dto.response.nextgen.design;

/**
 * 설계 세션 SSE 푸시 이벤트 (신호).
 * payload는 가볍게 두고, FE는 수신 후 세션 상태를 GET으로 재조회해 화면을 갱신한다.
 * (진실원: 영속 단계=DB, 검토 임시값=Redis 키. push 유실은 재조회/폴백 폴링으로 복구)
 */
public record ScenarioDesignSseEvent(

    Long scenarioGroupId,

    /** 트리거 단계: generation / validation / signposts */
    String type,

    /** 전이된 그룹 stage (scenario_created / scenario_validated / signpost_created) */
    String stage

) {

    public static ScenarioDesignSseEvent of(Long scenarioGroupId, String type, String stage) {
        return new ScenarioDesignSseEvent(scenarioGroupId, type, stage);
    }
}
