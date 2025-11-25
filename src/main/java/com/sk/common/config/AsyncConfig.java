package com.sk.common.config;

import com.sk.common.utils.MDCUtil;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskDecorator;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.security.concurrent.DelegatingSecurityContextExecutorService;
import org.springframework.security.task.DelegatingSecurityContextAsyncTaskExecutor;

@Slf4j
@Configuration
public class AsyncConfig implements AsyncConfigurer {

    @Override
    public Executor getAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5); // 기본 스레드 수
        executor.setMaxPoolSize(10); // 최대 스레드 수
        executor.setQueueCapacity(1000); // 큐 용량
        executor.setTaskDecorator(new MdcTaskDecorator()); // MDC 계승 설정
        executor.setThreadNamePrefix("async-");
        executor.initialize();
        return new DelegatingSecurityContextAsyncTaskExecutor(executor); // SpringSecurity 컨텍스트 계승 위한 래퍼
    }

    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return (ex, method, params) -> log.error("[@Async] [{}] failed.", method.getName(), ex);
    }

    /**
     * SSE 스트리밍을 위한 ExecutorService
     * SecurityContext를 자동으로 전파합니다.
     */
    @Bean(name = "sseExecutor")
    public ExecutorService sseExecutor() {
        log.info("Creating SSE ExecutorService with SecurityContext propagation");

        int cores = Runtime.getRuntime().availableProcessors();

        int corePoolSize  = Math.max(8, cores * 2);   // 코어 2배, 최소 8
        int maxPoolSize   = corePoolSize * 2;         // 스파이크 고려해서 2배
        int queueCapacity = 200;                      // 토큰 전송 Runnable 큐

        ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(
            corePoolSize,
            maxPoolSize,
            60L,
            TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(queueCapacity),
            new ThreadPoolExecutor.CallerRunsPolicy()
        );

        return new DelegatingSecurityContextExecutorService(threadPoolExecutor);
    }

    public static class MdcTaskDecorator implements TaskDecorator {

        @Override
        public Runnable decorate(Runnable runnable) {

            // 현재 스레드의 MDC 컨텍스트
            Map<String, String> contextMap = MDCUtil.getAdapter().getCopyOfContextMap();

            return () -> {
                try {
                    // 새 스레드에 MDC 컨텍스트 설정
                    if (contextMap != null) {
                        MDCUtil.getAdapter().setContextMap(contextMap);
                    }
                    runnable.run();
                } finally {
                    // 작업 끝난 후 MDC 정리
                    MDCUtil.clear();
                }
            };

        }

    }

}
