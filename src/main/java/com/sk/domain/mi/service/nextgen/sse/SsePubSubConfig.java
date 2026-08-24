package com.sk.domain.mi.service.nextgen.sse;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

/**
 * SSE 멀티노드 팬아웃 공용 인프라 설정
 *
 * Redis pub/sub 컨테이너는 도메인별로 두지 않고 하나를 공유한다 — 컨테이너는 원래 여러 리스너/토픽을
 * 다중 등록하도록 설계된 컴포넌트라, 공유 시 SUBSCRIBE 전용 연결과 컨테이너 스레드가 노드당 1벌로 줄고
 * 이후 SSE 도메인 추가도 패턴 등록 한 줄로 끝난다.
 */
@Configuration
public class SsePubSubConfig {

    @Bean
    public RedisMessageListenerContainer sseListenerContainer(RedisConnectionFactory connectionFactory,
                                                              SseEventSubscriber subscriber) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener(subscriber, new PatternTopic(SseEventPublisher.DESIGN_CHANNEL_PATTERN));
        container.addMessageListener(subscriber, new PatternTopic(SseEventPublisher.PRICE_INTERP_CHANNEL_PATTERN));
        container.addMessageListener(subscriber, new PatternTopic(SseEventPublisher.MODEL_PARAM_PROPOSAL_CHANNEL_PATTERN));
        return container;
    }

    /**
     * SSE 전용 스케줄러.
     * registry 하트비트 3개(설계/가격전망해석/파라미터제안)가 올라간다.
     * 하트비트는 블로킹 소켓 쓰기라 한 태스크가 지연되면 다른 태스크가 굶을 수 있으므로 태스크 수만큼 확보한다.
     */
    @Bean
    public ThreadPoolTaskScheduler sseHeartbeatScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(3);
        scheduler.setThreadNamePrefix("sse-hb-");
        scheduler.setRemoveOnCancelPolicy(true);
        return scheduler;
    }

}
