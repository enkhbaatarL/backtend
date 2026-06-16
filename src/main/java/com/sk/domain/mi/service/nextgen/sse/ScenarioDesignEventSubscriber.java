package com.sk.domain.mi.service.nextgen.sse;

import com.sk.common.utils.JsonUtil;
import com.sk.domain.mi.dto.response.nextgen.design.ScenarioDesignSseEvent;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;

/**
 * 설계 세션 SSE 이벤트 구독자
 *
 * Redis 채널({@code design:*})에서 이벤트를 수신해, 노드가 보유한 해당 세션 SSE 연결로 메세지를 전달한다.
 * 연결이 없는 노드에서는 {@link ScenarioDesignSseRegistry#emit}이 no-op이므로 안전하게 무시된다.
 *
 * @see ScenarioDesignEventPublisher 발행자
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ScenarioDesignEventSubscriber implements MessageListener {

    private final ScenarioDesignSseRegistry sseRegistry;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            String body = new String(message.getBody(), StandardCharsets.UTF_8);
            ScenarioDesignSseEvent event = JsonUtil.fromJson(body, ScenarioDesignSseEvent.class);
            sseRegistry.emit(event.scenarioGroupId(), event);
        } catch (Exception e) {
            log.warn("[Design SSE] subscribe failed (deserialize): {}", e.getMessage());
        }
    }

}
