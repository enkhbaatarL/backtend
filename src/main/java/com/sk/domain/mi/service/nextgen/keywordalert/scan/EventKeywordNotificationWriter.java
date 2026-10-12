package com.sk.domain.mi.service.nextgen.keywordalert.scan;

import com.sk.domain.shared.entity.master.tmd.EventKeywordNotification;
import com.sk.domain.shared.entity.master.tmd.EventKeywordNotificationDelivery;
import com.sk.domain.shared.entity.master.tmd.EventKeywordSetting;
import com.sk.domain.shared.enums.notification.NotificationChannel;
import com.sk.domain.shared.repository.master.tmd.EventKeywordNotificationDeliveryRepository;
import com.sk.domain.shared.repository.master.tmd.EventKeywordNotificationRepository;
import com.sk.domain.shared.repository.master.tmm.projection.KeywordAlertEventRow;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 이벤트 1건의 키워드 알림 생성 (이벤트 단위 트랜잭션)
 * - 생성: 알림(tmd_mi_evnt_keywdnotil) + 채널별 발송 대기(tmd_mi_evnt_keywdsendl, PENDING)
 * - 중복: 이미 알림이 있는 키워드 설정은 건너뜀 (재스캔 대비)
 */
@Component
@RequiredArgsConstructor
public class EventKeywordNotificationWriter {

    /** 1차 발송 채널 (SMS는 채널 설정 도입 시 추가) */
    private static final List<NotificationChannel> CHANNELS = List.of(NotificationChannel.EMAIL);

    private final EventKeywordNotificationRepository eventKeywordNotificationRepository;
    private final EventKeywordNotificationDeliveryRepository eventKeywordNotificationDeliveryRepository;

    /**
     * [이벤트 알림 생성]
     * - 스캔은 겹치는 구간을 다시 읽으므로, 이 이벤트에 이미 알림이 있는 키워드 설정은 건너뜀
     * - 다른 노드와 경합해 uq01(usr_no, mi_event_id, keywd_set_id)에 걸리면 예외로 이 이벤트 트랜잭션만 롤백됨.
     *   호출자가 로그를 남기고 다음 스캔에서 위의 존재 확인으로 정리됨
     *
     * @return 새로 만든 알림 수
     */
    @Transactional
    public int write(KeywordAlertEventRow event, List<EventKeywordSetting> matchedSettings) {
        Set<Long> alreadyNotified = eventKeywordNotificationRepository.findKeywordSettingIdsByMiEventId(event.getMiEventId());

        int created = 0;
        for (EventKeywordSetting setting : matchedSettings) {
            if (alreadyNotified.contains(setting.getKeywordSettingId())) {
                continue;
            }
            EventKeywordNotification notification = eventKeywordNotificationRepository.save(EventKeywordNotification.create(setting, event.getMiEventId(), event.getOneLineSummary()));
            for (NotificationChannel channel : CHANNELS) {
                eventKeywordNotificationDeliveryRepository.save(EventKeywordNotificationDelivery.createPending(notification, channel));
            }
            created++;
        }
        return created;
    }

}
