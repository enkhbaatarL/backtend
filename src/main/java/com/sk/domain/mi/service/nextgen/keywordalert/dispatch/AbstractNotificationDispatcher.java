package com.sk.domain.mi.service.nextgen.keywordalert.dispatch;

import com.sk.domain.shared.enums.notification.NotificationChannel;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;

/**
 * 채널 공통 알림 디스패처 (모든 노드 실행)
 * - 흐름: SENDING 타임아웃 복구 → 대상 조회 → 행 단위 선점 → send() → 결과 기록
 * - 중복 방지: 선점(조건부 UPDATE)에 성공한 노드만 발송
 * - 재시도: FAIL 후 retry-backoff 경과 시, 최대 max-attempts회
 * - 하위 클래스: channel(), send()만 구현
 */
@Slf4j
public abstract class AbstractNotificationDispatcher {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}");

    private static final Pattern PHONE_PATTERN = Pattern.compile("(?<!\\d)\\+?\\d{2,3}[-\\s]?\\d{3,4}[-\\s]?\\d{4}(?!\\d)");

    private final NotificationDeliveryStateService stateService;

    private final NotificationDispatchProperties properties;

    protected AbstractNotificationDispatcher(NotificationDeliveryStateService stateService, NotificationDispatchProperties properties) {
        this.stateService = stateService;
        this.properties = properties;
    }

    /** 이 디스패처가 담당하는 채널 */
    public abstract NotificationChannel channel();

    /**
     * 채널별 실제 발송. 실패 시 예외를 던지면 공통 흐름이 FAIL로 기록함
     * - 트랜잭션 밖에서 호출됨
     */
    protected abstract void send(NotificationDispatchTarget target);

    /**
     * [채널 디스패치 1회]
     * - 한 건의 실패가 다른 건의 발송을 막지 않음
     *
     * @return 이번 실행에서 이 노드가 처리한 결과
     */
    public DispatchResult dispatch() {
        NotificationChannel channel = channel();
        LocalDateTime now = LocalDateTime.now();
        stateService.failStuckSending(channel, properties.getMaxAttempts(), now.minus(properties.getSendingTimeout()));

        LocalDateTime retryBefore = now.minus(properties.getRetryBackoff());
        List<Long> ids = stateService.findDispatchableIds(channel, properties.getMaxAttempts(), retryBefore, properties.getBatchSize());
        int sent = 0;
        int failed = 0;
        for (Long id : ids) {
            if (!stateService.claim(id, properties.getMaxAttempts(), retryBefore)) {
                continue;
            }
            if (dispatchClaimed(id)) {
                sent++;
            } else {
                failed++;
            }
        }

        DispatchResult result = new DispatchResult(channel, ids.size(), sent, failed);
        if (sent > 0 || failed > 0) {
            log.info("[Keyword Alert Dispatch] {}", result);
        }
        return result;
    }

    private boolean dispatchClaimed(Long keywordSendId) {
        try {
            Optional<NotificationDispatchTarget> target = stateService.loadTarget(keywordSendId);
            if (target.isEmpty()) {
                stateService.markFail(keywordSendId, "발송 대상 알림을 찾을 수 없습니다.");
                return false;
            }
            send(target.get());
            stateService.markSuccess(keywordSendId);
            return true;
        } catch (RuntimeException e) {
            String masked = maskPersonalInfo("%s: %s".formatted(e.getClass().getSimpleName(), e.getMessage()));
            log.warn("[Keyword Alert Dispatch] send failed: channel={}, keywordSendId={}, {}", channel(), keywordSendId, masked);
            stateService.markFail(keywordSendId, masked);
            return false;
        }
    }

    /**
     * 오류 메시지의 이메일 주소·전화번호 마스킹
     * - 메일 서버·SMS 게이트웨이 오류에는 수신 주소가 그대로 들어오는 경우가 많음. 발송 내역에 개인정보를 남기지 않음
     */
    static String maskPersonalInfo(String message) {
        if (message == null) {
            return null;
        }
        String masked = EMAIL_PATTERN.matcher(message).replaceAll("***@***");
        return PHONE_PATTERN.matcher(masked).replaceAll("***-****-****");
    }

    /**
     * 디스패치 1회 결과
     *
     * @param candidates 조회한 발송 대상 수
     * @param sent       이 노드가 선점해 발송에 성공한 수
     * @param failed     이 노드가 선점했으나 발송에 실패한 수
     */
    public record DispatchResult(

        NotificationChannel channel,

        int candidates,

        int sent,

        int failed

    ) {
    }

}
