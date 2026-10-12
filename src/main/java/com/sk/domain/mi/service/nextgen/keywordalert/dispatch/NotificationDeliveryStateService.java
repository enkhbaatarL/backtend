package com.sk.domain.mi.service.nextgen.keywordalert.dispatch;

import com.sk.domain.shared.entity.master.tmd.EventKeywordNotificationDelivery;
import com.sk.domain.shared.enums.notification.NotificationChannel;
import com.sk.domain.shared.enums.notification.NotificationSendStatus;
import com.sk.domain.shared.repository.master.tmd.EventKeywordNotificationDeliveryRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 발송 행 상태 전이 (tmd_mi_evnt_keywdsendl)
 * - 트랜잭션: 단계마다 짧게, 실제 발송은 트랜잭션 밖
 * - 중복 방지: claim()의 조건부 UPDATE (Redis 락 없음)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationDeliveryStateService {

    static final String STUCK_SENDING_MESSAGE = "발송 중 중단";

    private final EventKeywordNotificationDeliveryRepository deliveryRepository;

    /**
     * [발송 대상 조회]
     * - PENDING과, 최대 시도 수 미만이면서 마지막 시도가 retryBefore 이전인 FAIL. 오래된 순
     * - 조회만 하고 선점은 claim()에서 행 단위로 함
     */
    @Transactional(readOnly = true)
    public List<Long> findDispatchableIds(NotificationChannel channel, int maxAttempts, LocalDateTime retryBefore, int batchSize) {
        return deliveryRepository.findDispatchableIds(channel, NotificationSendStatus.PENDING, NotificationSendStatus.FAIL,
                maxAttempts, retryBefore, PageRequest.of(0, batchSize));
    }

    /**
     * [발송 선점]
     * - PENDING/재시도 가능 FAIL → SENDING 조건부 UPDATE. 갱신 1건이면 이 노드가 발송 권한을 가짐
     * - 같은 행을 다른 노드가 먼저 선점했으면 0건 → false. 조회 조건과 같은 조건을 UPDATE에 다시 걸어 조회~선점 사이 변경도 막음
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean claim(Long keywordSendId, int maxAttempts, LocalDateTime retryBefore) {
        return deliveryRepository.claim(keywordSendId, NotificationSendStatus.SENDING, NotificationSendStatus.PENDING,
                NotificationSendStatus.FAIL, maxAttempts, retryBefore, LocalDateTime.now()) == 1;
    }

    /** 선점한 발송 1건의 내용 조회 (알림·수신 사용자 포함) */
    @Transactional(readOnly = true)
    public Optional<NotificationDispatchTarget> loadTarget(Long keywordSendId) {
        return deliveryRepository.findWithNotificationAndUser(keywordSendId).map(NotificationDispatchTarget::of);
    }

    /**
     * [발송 성공 기록]
     * - SENDING일 때만 기록함. 발송이 sendingTimeout보다 오래 걸려 복구 처리로 FAIL이 됐으면 덮어쓰지 않음
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markSuccess(Long keywordSendId) {
        findSending(keywordSendId).ifPresent(EventKeywordNotificationDelivery::markSuccess);
    }

    /**
     * [발송 실패 기록]
     * - 시도 수를 올리고 FAIL로 둠. 최대 시도 수 미만이면 retry-backoff 후 다시 선점됨
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markFail(Long keywordSendId, String maskedErrorMessage) {
        findSending(keywordSendId).ifPresent(delivery -> delivery.markFail(maskedErrorMessage));
    }

    /**
     * [SENDING 타임아웃 복구]
     * - 선점 후 결과를 남기기 전에 노드가 죽으면 SENDING에 멈춤. 일정 시간 지난 행을 FAIL로 바꿈
     * - 메일이 실제로 나갔는지 알 수 없으므로 재시도하지 않음(시도 수를 최대값으로 설정). 중복 발송보다 누락을 택함
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int failStuckSending(NotificationChannel channel, int maxAttempts, LocalDateTime sendingBefore) {
        int recovered = deliveryRepository.failStuckSending(channel, NotificationSendStatus.SENDING, NotificationSendStatus.FAIL,
                maxAttempts, STUCK_SENDING_MESSAGE, sendingBefore, LocalDateTime.now());
        if (recovered > 0) {
            log.warn("[Keyword Alert Dispatch] stuck SENDING rows failed: channel={}, count={}", channel, recovered);
        }
        return recovered;
    }

    private Optional<EventKeywordNotificationDelivery> findSending(Long keywordSendId) {
        Optional<EventKeywordNotificationDelivery> delivery = deliveryRepository.findById(keywordSendId)
                .filter(EventKeywordNotificationDelivery::isSending);
        if (delivery.isEmpty()) {
            log.warn("[Keyword Alert Dispatch] result ignored (not SENDING anymore): keywordSendId={}", keywordSendId);
        }
        return delivery;
    }

}
