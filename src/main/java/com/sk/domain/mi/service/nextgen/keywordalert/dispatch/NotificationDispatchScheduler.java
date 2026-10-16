package com.sk.domain.mi.service.nextgen.keywordalert.dispatch;

import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 키워드 알림 발송 스케줄러
 * - 활성화: features.scheduling.event-keyword-alert-dispatch.enabled=true (기본 비활성)
 * - 실행: 모든 노드, 등록된 채널 디스패처를 차례로 실행 (채널 간 실패 격리)
 */
@Slf4j
@Component
@Profile("!dedicated")
@RequiredArgsConstructor
@ConditionalOnProperty(
    prefix = "features.scheduling.event-keyword-alert-dispatch",
    name = "enabled",
    havingValue = "true"
)
public class NotificationDispatchScheduler {

    private final List<AbstractNotificationDispatcher> dispatchers;

    @Scheduled(fixedDelayString = "${features.scheduling.event-keyword-alert-dispatch.fixed-delay-ms:30000}")
    public void dispatch() {
        for (AbstractNotificationDispatcher dispatcher : dispatchers) {
            try {
                dispatcher.dispatch();
            } catch (RuntimeException e) {
                log.error("Event keyword alert dispatch failed. channel={}", dispatcher.channel(), e);
            }
        }
    }

}
