package com.sk.domain.shared.config;

import java.time.Duration;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * [MCP Event Generate] 이벤트 생성 비동기 작업 설정 (mcp.event-generation.*). dedicated profile 에서만 등록된다 (EventGenerationJobConfig).
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "mcp.event-generation")
public class EventGenerationJobProperties {

    /**
     * 서버당 동시에 실행할 수 있는 이벤트 생성 작업 수. 넘는 요청은 즉시 거절한다 (서버가 늘면 전체 허용 수도 는다).
     * 실행 스레드 수와 생성 서버 동시 연결 수도 이 값으로 둔다.
     */
    private int maxConcurrent = 10;

    /**
     * heartbeat 가 이 시간 이상 끊긴 RUNNING 작업은 INTERRUPTED 로 정리한다.
     * heartbeat 주기(mcp.event-generation.heartbeat-rate-ms)보다 충분히 길어야 한다.
     */
    private Duration staleAfter = Duration.ofMinutes(5);

}
