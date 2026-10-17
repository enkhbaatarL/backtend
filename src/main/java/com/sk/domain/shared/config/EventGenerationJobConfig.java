package com.sk.domain.shared.config;

import com.sk.common.config.AsyncConfig;
import com.sk.common.http.RestClientBuilder;
import com.sk.domain.shared.client.EventGenerationAgentApi;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.security.concurrent.DelegatingSecurityContextRunnable;

/**
 * [MCP Event Generate] 이벤트 생성 작업 설정. MCP 이벤트 생성 API 는 SK Gas 전용 서버에서만 쓰므로 dedicated profile 에서만 만든다 —
 * 일반 서버에는 실행 스레드 풀, 생성 서버 클라이언트(연결 풀), heartbeat 스케줄이 생기지 않는다.
 */
@Profile("dedicated")
@Configuration
@EnableConfigurationProperties(EventGenerationJobProperties.class)
public class EventGenerationJobConfig {

    public static final String EXECUTOR_BEAN_NAME = "eventGenerationJobExecutor";

    /**
     * 생성 작업 전용 실행기. 공용 @Async 풀(AsyncConfig)과 분리한다 — 한 작업이 최대 15분 스레드를 점유하므로 공용 풀을 쓰면 다른 비동기 작업이 밀린다.
     * 큐 없이 스레드 수(maxConcurrent)만큼만 받는다. 빈으로 등록해 종료 시 컨테이너가 풀을 정리한다.
     * MDC(로그 추적 ID)와 SecurityContext(감사 컬럼 수정자)를 요청 스레드에서 이어받는다.
     */
    @Bean(name = EXECUTOR_BEAN_NAME)
    public ThreadPoolTaskExecutor eventGenerationJobExecutor(EventGenerationJobProperties properties) {
        AsyncConfig.MdcTaskDecorator mdcDecorator = new AsyncConfig.MdcTaskDecorator();
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(properties.getMaxConcurrent());
        executor.setMaxPoolSize(properties.getMaxConcurrent());
        executor.setQueueCapacity(0);
        executor.setTaskDecorator(runnable -> DelegatingSecurityContextRunnable.create(mdcDecorator.decorate(runnable), null));
        executor.setThreadNamePrefix("event-gen-");
        // 종료 시 lifecycle stop 단계에서 실행 중 작업(최대 15분)을 기다리지 않는다. 기본값(false)이면 timeout-per-shutdown-phase(30초)만큼
        // 종료가 늦어져, 컨테이너 종료 유예(기본 30초) 안에 실행 중 작업을 INTERRUPTED 로 바꾸지(EventGenerationJobRunner) 못하고 강제 종료될 수 있다.
        // 웹 서버가 먼저 멈추므로 종료 중 새 작업이 들어오지는 않는다.
        executor.setAcceptTasksAfterContextClose(true);
        return executor;
    }

    /**
     * 이벤트 생성 서버 클라이언트. 웹 검색·LLM 생성을 동기로 기다리므로(최대 15분) read-timeout 을 길게 두고,
     * 동시 연결 수를 실행 스레드 수(maxConcurrent)에 맞춘다 — 기본 풀(호스트당 5)이면 6번째 작업부터 연결을 기다리기만 한다.
     * 주소는 api.event-generation-agent.url (profile yml 에 설정), 없으면 환경변수 EVENT_GENERATION_AGENT_URL.
     */
    @Bean
    public EventGenerationAgentApi eventGenerationAgentApi(EventGenerationJobProperties properties,
                                                           @Value("${api.event-generation-agent.url:${EVENT_GENERATION_AGENT_URL:}}") String url,
                                                           @Value("${api.event-generation-agent.connection-timeout:1s}") Duration connectTimeout,
                                                           @Value("${api.event-generation-agent.read-timeout:1200s}") Duration readTimeout) {
        return new RestClientBuilder()
            .url(url)
            .connectionTimeout(connectTimeout)
            .readTimeout(readTimeout)
            .maxConnections(properties.getMaxConcurrent())
            .build(EventGenerationAgentApi.class);
    }

}
