package com.sk.domain.mi.service.nextgen.sse.registry;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * scenarioGroupId 단위 SSE 연결 레지스트리 공통 추상 클래스
 * 연결 보관/팬아웃/프레임 전송/정리/하트비트 등 동시성 민감 전송 로직 모음
 */
@Slf4j
public abstract class AbstractSseRegistry<E> {

    /**
     * SSE 연결 타임아웃
     */
    private static final long TIMEOUT_MS = Duration.ofMinutes(30).toMillis();

    /**
     * scenarioGroupId -> 이 노드의 SSE 연결
     */
    private final Map<Long, Set<SseEmitter>> emitters = new ConcurrentHashMap<>();

    /**
     * SSE 연결을 생성 및 등록한다. (종료/타임아웃/에러 시 자동 정리)
     * 연결 직후 connected 이벤트를 1회 보낸 뒤 도메인 훅(onConnected)을 호출한다.
     */
    public SseEmitter connect(Long scenarioGroupId) {

        SseEmitter emitter = new SseEmitter(TIMEOUT_MS);

        emitters.compute(scenarioGroupId, (key, set) -> {
            Set<SseEmitter> target = set == null ? ConcurrentHashMap.newKeySet() : set;
            target.add(emitter);
            return target;
        });

        emitter.onCompletion(() -> remove(scenarioGroupId, emitter));

        emitter.onTimeout(() -> {
            emitter.complete();
            remove(scenarioGroupId, emitter);
        });

        emitter.onError(e -> remove(scenarioGroupId, emitter));

        send(emitter, "connected", Map.of("scenarioGroupId", scenarioGroupId));
        log.debug("{} connected: groupId={}, localConnections={}", logTag(), scenarioGroupId, count(scenarioGroupId));

        onConnected(scenarioGroupId, emitter);

        return emitter;
    }

    /**
     * 해당 세션의 이 노드 연결 전부에 이벤트를 전송한다. (Redis 구독자가 모든 노드에서 호출)
     * 이 노드에 연결이 없으면 아무것도 하지 않는다.
     */
    public void emit(Long scenarioGroupId, E event) {
        Set<SseEmitter> set = emitters.get(scenarioGroupId);
        if (set == null || set.isEmpty()) {
            return;
        }
        for (SseEmitter emitter : set) {
            if (!send(emitter, eventName(event), event)) {
                remove(scenarioGroupId, emitter);
            }
        }
    }

    /**
     * 이벤트 페이로드에서 SSE 이벤트명(event:)을 해석한다.
     */
    protected abstract String eventName(E event);

    /**
     * 로그 프리픽스
     */
    protected abstract String logTag();

    /**
     * 연결 직후 훅
     * 재조회로 복구 불가능한 휘발 상태(ex. 시나리오 설계)를 가진 도메인만 재정의
     */
    protected void onConnected(Long scenarioGroupId, SseEmitter emitter) {
    }

    /**
     * 단일 emitter에 이름 있는 이벤트 한 건을 전송한다.
     */
    protected final boolean send(SseEmitter emitter, String eventName, Object data) {
        return trySend(emitter, SseEmitter.event().name(eventName).data(data));
    }

    /**
     * 단일 emitter에 한 프레임을 전송한다.
     * emitter 단위 synchronized로 프레임 인터리빙을 막는다.
     * 전송 실패(끊긴 연결)면 false.
     */
    private boolean trySend(SseEmitter emitter, SseEmitter.SseEventBuilder event) {
        try {
            synchronized (emitter) {
                emitter.send(event);
            }
            return true;
        } catch (IOException | IllegalStateException e) {
            log.debug("{} send failed, dropping emitter: {}", logTag(), e.getMessage());
            return false;
        }
    }

    /**
     * 키 단위 원자 해제 - 마지막 연결이면 map 엔트리도 함께 제거한다 (connect의 compute와 경합 안전).
     */
    private void remove(Long scenarioGroupId, SseEmitter emitter) {
        emitters.computeIfPresent(scenarioGroupId, (key, set) -> {
            set.remove(emitter);
            return set.isEmpty() ? null : set;
        });
    }

    private int count(Long scenarioGroupId) {
        Set<SseEmitter> set = emitters.get(scenarioGroupId);
        return set == null ? 0 : set.size();
    }

    /**
     * 주기적으로 keep-alive comment(:hb)를 전달한다.
     * idle 연결이 프록시/LB의 idle timeout에 끊기는 것을 막고, write 실패로 죽은 연결을 정리한다.
     * (서브클래스 빈마다 각자 스케줄링됨)
     */
    @Scheduled(fixedRateString = "${features.scheduling.sse.heartbeat-rate-ms:20000}", scheduler = "sseHeartbeatScheduler")
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
