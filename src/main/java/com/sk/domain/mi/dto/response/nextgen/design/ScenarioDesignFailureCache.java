package com.sk.domain.mi.dto.response.nextgen.design;

/**
 * Agent 작업 실패 휘발 캐시 (Redis 저장용)
 * 실패는 DB에 남지 않고 SSE 푸시도 best-effort(구독 레이스/연결 끊김으로 유실 가능)라, 마지막 실패(단계/사유)를 임시 보관해 상태 재조회(GET /design)로 복구할 수 있게 한다.
 */
public record ScenarioDesignFailureCache(

    ScenarioDesignSseEvent.Type type,
    String reason

) {

    public static ScenarioDesignFailureCache of(ScenarioDesignSseEvent.Type type, String reason) {
        return new ScenarioDesignFailureCache(type, reason);
    }

}
