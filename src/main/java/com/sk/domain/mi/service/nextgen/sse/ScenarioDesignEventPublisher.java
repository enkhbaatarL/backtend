package com.sk.domain.mi.service.nextgen.sse;

import com.sk.common.utils.JsonUtil;
import com.sk.domain.mi.dto.response.nextgen.design.ScenarioDesignSseEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * 설계 세션 SSE 이벤트 발행자
 *
 * 콜백 도착 시 이 노드에서 직접 emit 하지 않고 Redis 채널({@code design:{groupId}})로 발행한다.
 * 발행 노드 포함 모든 노드의 구독자가 수신하므로, 해당 세션 연결을 보유한 노드에서만 실제 SSE 푸시가 일어난다.
 *
 * @see ScenarioDesignEventSubscriber 구독자
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ScenarioDesignEventPublisher {

    public static final String CHANNEL_PREFIX = "scenario-design:";
    public static final String CHANNEL_PATTERN = CHANNEL_PREFIX + "*";

    private final StringRedisTemplate stringRedisTemplate;

    public static String channel(Long scenarioGroupId) {
        return CHANNEL_PREFIX + scenarioGroupId;
    }

    /**
     * 설계 세션 이벤트를 Redis 채널로 발행한다.
     */
    public void publish(ScenarioDesignSseEvent event) {
        try {
            String body = JsonUtil.toJson(event);
            stringRedisTemplate.convertAndSend(channel(event.scenarioGroupId()), body);
            log.debug("[Design SSE] published: groupId={}, type={}, stage={}", event.scenarioGroupId(), event.type(), event.stage());
        } catch (Exception e) {
            log.warn("[Design SSE] publish failed (serialize): groupId={}, {}", event.scenarioGroupId(), e.getMessage());
        }
    }

}
