package com.sk.domain.mi.service.nextgen.sse;

import com.sk.domain.mi.dto.response.nextgen.design.ScenarioDesignSseEvent;
import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * 시나리오 설계 세션 SSE 연결 레지스트리
 * <p>
 * scenarioGroupId별로 현재 이 노드에 붙어있는 SSE 연결을 보관하고, 이벤트를 전달한다.
 * {@link #emit(Long, ScenarioDesignSseEvent)}을 호출하고, 실제 연결을 보유한 노드만 전송한다.
 * (연결 없는 노드는 no-op)
 */
@Slf4j
@Component
public class ScenarioDesignSseRegistry {

    /** SSE 연결 타임아웃 */
    private static final long TIMEOUT_MS = Duration.ofMinutes(30).toMillis();

    /** scenarioGroupId -> 이 노드의 SSE 연결들 */
    private final Map<Long, Set<SseEmitter>> emitters = new ConcurrentHashMap<>();

    /**
     * 설계 세션 SSE 연결을 생성·등록한다. 종료/타임아웃/에러 시 자동 정리되고,
     * 연결 직후 {@code connected} 이벤트를 1회 보낸다. (FE는 이때 상태 재조회)
     */
    public SseEmitter connect(Long scenarioGroupId) {
        SseEmitter emitter = new SseEmitter(TIMEOUT_MS);
        emitters.computeIfAbsent(scenarioGroupId, k -> ConcurrentHashMap.newKeySet()).add(emitter);

        emitter.onCompletion(() -> remove(scenarioGroupId, emitter));
        emitter.onTimeout(() -> {
            emitter.complete();
            remove(scenarioGroupId, emitter);
        });
        emitter.onError(e -> remove(scenarioGroupId, emitter));

        send(emitter, "connected", Map.of("scenarioGroupId", scenarioGroupId));
        log.debug("[Design SSE] connected: groupId={}, localConnections={}", scenarioGroupId, count(scenarioGroupId));
        return emitter;
    }

    /**
     * 해당 세션의 이 노드 연결 전부에 이벤트를 전송한다. (Redis 구독자가 모든 노드에서 호출)
     * 이 노드에 연결이 없으면 아무것도 하지 않는다.
     */
    public void emit(Long scenarioGroupId, ScenarioDesignSseEvent event) {
        Set<SseEmitter> set = emitters.get(scenarioGroupId);
        if (set == null || set.isEmpty()) {
            return;
        }
        for (SseEmitter emitter : set) {
            if (!send(emitter, event.type().getCode(), event)) {
                remove(scenarioGroupId, emitter);
            }
        }
    }

    private boolean send(SseEmitter emitter, String eventName, Object data) {
        return trySend(emitter, SseEmitter.event().name(eventName).data(data));
    }

    /**
     * 단일 emitter에 한 프레임을 전송한다.
     * 하트비트 스레드와 이벤트(Redis subscriber) 스레드가 같은 연결에 동시에 써서 프레임이 깨지는 것을 막는다.
     * 전송 실패(끊긴 연결)면 false.
     */
    private boolean trySend(SseEmitter emitter, SseEmitter.SseEventBuilder event) {
        try {
            synchronized (emitter) {
                emitter.send(event);
            }
            return true;
        } catch (IOException | IllegalStateException e) {
            log.debug("[Design SSE] send failed, dropping emitter: {}", e.getMessage());
            return false;
        }
    }

    private void remove(Long scenarioGroupId, SseEmitter emitter) {
        Set<SseEmitter> set = emitters.get(scenarioGroupId);
        if (set != null) {
            set.remove(emitter);
            if (set.isEmpty()) {
                emitters.remove(scenarioGroupId);
            }
        }
    }

    private int count(Long scenarioGroupId) {
        Set<SseEmitter> set = emitters.get(scenarioGroupId);
        return set == null ? 0 : set.size();
    }

    /**
     * 설계 세션 SSE 연결에 주기적으로 keep-alive comment(:hb)를 전달
     * idle 연결이 프록시/LB의 idle timeout에 끊기는 것을 막고, write 실패로 죽은 연결을 정리한다.
     */
    @Scheduled(fixedRateString = "${features.scheduling.scenario-design-sse.heartbeat-rate-ms:20000}", scheduler = "sseHeartbeatScheduler")
    void heartbeat() {

        if (emitters.isEmpty()) return;

        emitters.forEach((groupId, set) -> {
            for (SseEmitter emitter : set) {
                if (!trySend(emitter, SseEmitter.event().comment("hb"))) {
                    remove(groupId, emitter);
                }
            }
        });
    }

}
