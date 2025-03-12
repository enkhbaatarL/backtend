package com.sk.common.config;

import com.sk.common.utils.MDCUtil;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskDecorator;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.Map;
import java.util.concurrent.Executor;

@Configuration
public class AsyncConfig implements AsyncConfigurer {

    @Override
    public Executor getAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5); // 기본 스레드 수
        executor.setMaxPoolSize(10); // 최대 스레드 수
        executor.setQueueCapacity(100); // 큐 용량
        executor.setTaskDecorator(new MdcTaskDecorator()); // MDC 전달 설정
        executor.initialize();
        return executor;
    }

    static public class MdcTaskDecorator implements TaskDecorator {

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
