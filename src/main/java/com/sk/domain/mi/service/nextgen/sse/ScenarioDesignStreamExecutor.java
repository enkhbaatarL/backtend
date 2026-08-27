package com.sk.domain.mi.service.nextgen.sse;

import jakarta.annotation.PreDestroy;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Component;

/**
 * 시나리오 설계(v4) SSE 중계 전용 스레드풀.
 *
 * Agent 스트림을 읽어 FE emitter로 흘리는 중계 루프는 생성이 끝날 때까지 수 분간 스레드를 점유하는 블로킹 IO
 *  -> 톰캣 요청 스레드/공용 비동기 풀과 격리된 전용 풀에서 실행한다.
 * 활성 스트림 수 = 스레드 수(1:1) 워크로드라 cached pool을 쓰고, 유휴 스레드는 자동 회수된다.
 *
 * 데몬 스레드라 JVM 종료를 막지 않으며, 컨텍스트 종료 시 shutdownNow로 인터럽트를 걸어 블로킹 중인 중계 스레드를 즉시 회수한다.
 */
@Component
public class ScenarioDesignStreamExecutor implements Executor {

    private final AtomicLong threadSequence = new AtomicLong();

    private final ExecutorService delegate = Executors.newCachedThreadPool(runnable -> {
        Thread thread = new Thread(runnable, "design-v4-sse-%d".formatted(threadSequence.incrementAndGet()));
        thread.setDaemon(true);
        return thread;
    });

    @Override
    public void execute(Runnable command) {
        delegate.execute(command);
    }

    @PreDestroy
    void shutdown() {
        delegate.shutdownNow();
    }

}
