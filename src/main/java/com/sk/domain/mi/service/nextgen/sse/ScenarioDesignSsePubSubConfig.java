package com.sk.domain.mi.service.nextgen.sse;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

/**
 * 설계 세션 SSE 멀티노드 팬아웃을 위한 Redis Pub/Sub 설정.
 * {@code design:*} 채널을 구독해 {@link ScenarioDesignEventSubscriber}로 전달한다.
 */
@Configuration
public class ScenarioDesignSsePubSubConfig {

    @Bean
    public RedisMessageListenerContainer scenarioDesignSseListenerContainer(RedisConnectionFactory connectionFactory, ScenarioDesignEventSubscriber subscriber) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener(subscriber, new PatternTopic(ScenarioDesignEventPublisher.CHANNEL_PATTERN));
        return container;
    }
}
