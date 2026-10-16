package com.sk.domain.mi.service.nextgen.keywordalert.scan;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 키워드 알림 스캔 스케줄러
 * - 활성화: features.scheduling.event-keyword-alert-scan.enabled=true (기본 비활성)
 * - 실행: 모든 노드가 깨어나지만 실제 스캔은 락을 잡은 1개 노드만
 */
@Slf4j
@Component
@Profile("!dedicated")
@RequiredArgsConstructor
@ConditionalOnProperty(
    prefix = "features.scheduling.event-keyword-alert-scan",
    name = "enabled",
    havingValue = "true"
)
public class EventKeywordAlertScheduler {

    private final EventKeywordAlertScanService eventKeywordAlertScanService;

    @Scheduled(fixedDelayString = "${features.scheduling.event-keyword-alert-scan.fixed-delay-ms:60000}")
    public void scan() {
        try {
            eventKeywordAlertScanService.scan();
        } catch (RuntimeException e) {
            log.error("Event keyword alert scan scheduler failed.", e);
        }
    }

}
