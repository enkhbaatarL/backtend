package com.sk.domain.mi.service.nextgen.sse.registry;

import com.sk.domain.mi.dto.response.nextgen.design.ScenarioDesignSseEvent;
import com.sk.domain.mi.service.nextgen.design.ScenarioDesignFailureStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * 시나리오 설계 세션 SSE 연결 레지스트리
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ScenarioDesignSseRegistry extends AbstractSseRegistry<ScenarioDesignSseEvent> {

    /**
     * 연결 시점에 미해결 실패를 재전송하기 위한 실패 캐시 조회용
     */
    private final ScenarioDesignFailureStore failureStore;

    @Override
    protected String eventName(ScenarioDesignSseEvent event) {
        return event.type().getCode();
    }

    @Override
    protected String logTag() {
        return "[ScenarioDesign SSE]";
    }

    /**
     * 연결 시점에 미해결 콜백 실패가 캐시에 있으면(레이스/끊김으로 실시간 푸시를 놓쳤을 수 있음) 해당 연결로 재전송한다.
     */
    @Override
    protected void onConnected(Long scenarioGroupId, SseEmitter emitter) {
        try {
            failureStore.find(scenarioGroupId).ifPresent(f ->
                send(emitter, f.type().getCode(), ScenarioDesignSseEvent.failure(scenarioGroupId, f.type(), f.reason())));
        } catch (Exception e) {
            log.warn("[Design SSE] failure replay on connect skipped: groupId={}, {}", scenarioGroupId, e.getMessage());
        }
    }

}
