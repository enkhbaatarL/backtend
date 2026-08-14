package com.sk.domain.mi.service.nextgen;

import com.sk.domain.mi.dto.request.nextgen.design.ScenarioDesignStartV4Req;
import com.sk.domain.mi.dto.response.nextgen.design.ScenarioDesignPresentationRes;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * 멀티 이벤트 시나리오 설계 (v4) 서비스.
 * 생성 진행은 Agent SSE 직결 중계, 설계 화면 데이터는 Agent presentation을 B.E 계약으로 가공해 제공한다 (2026-08-12 회의 확정).
 * 공개 API는 생성 스트림과 화면 데이터 조회 둘뿐이다 — 진행중 작업 재연결은 생성 스트림이 내부에서 자동 처리한다.
 */
public interface ScenarioDesignV4Service {

    SseEmitter startDesignStream(ScenarioDesignStartV4Req request);

    ScenarioDesignPresentationRes getPresentation(Long scenarioGroupId, String operationId);

}
