package com.sk.domain.mi.service.nextgen.keywordalert.dispatch;

import java.time.Duration;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 알림 디스패치 공통 정책 (features.scheduling.event-keyword-alert-dispatch.*)
 */
@Getter
@Component
public class NotificationDispatchProperties {

    /** 1회 실행에서 채널별로 선점을 시도할 최대 건수 */
    private final int batchSize;

    /** 발송 최대 시도 수 (FAIL이 이 수 미만이면 다음 실행에서 재시도) */
    private final int maxAttempts;

    /** FAIL 후 이 시간이 지나야 다시 선점 대상 (재시도 간격을 발송 주기와 분리) */
    private final Duration retryBackoff;

    /** SENDING으로 이 시간 이상 머문 행은 결과 미확인으로 보고 재시도 없는 FAIL 처리 */
    private final Duration sendingTimeout;

    public NotificationDispatchProperties(
            @Value("${features.scheduling.event-keyword-alert-dispatch.batch-size:100}") int batchSize,
            @Value("${features.scheduling.event-keyword-alert-dispatch.max-attempts:3}") int maxAttempts,
            @Value("${features.scheduling.event-keyword-alert-dispatch.retry-backoff-minutes:5}") long retryBackoffMinutes,
            @Value("${features.scheduling.event-keyword-alert-dispatch.sending-timeout-minutes:10}") long sendingTimeoutMinutes
    ) {
        this.batchSize = batchSize;
        this.maxAttempts = maxAttempts;
        this.retryBackoff = Duration.ofMinutes(retryBackoffMinutes);
        this.sendingTimeout = Duration.ofMinutes(sendingTimeoutMinutes);
    }

}
