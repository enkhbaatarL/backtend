package com.sk.domain.mi.service.nextgen.keywordalert.dispatch;

import com.sk.domain.shared.enums.notification.NotificationChannel;
import org.springframework.stereotype.Component;

/**
 * 키워드 알림 SMS 디스패처 (뼈대, send() 미구현)
 * - 미정: 발송 게이트웨이, 수신 전화번호 출처
 * - 현재 EMAIL 발송 행만 생성되므로 처리 대상 없음
 */
@Component
public class SmsNotificationDispatcher extends AbstractNotificationDispatcher {

    public SmsNotificationDispatcher(NotificationDeliveryStateService stateService, NotificationDispatchProperties properties) {
        super(stateService, properties);
    }

    @Override
    public NotificationChannel channel() {
        return NotificationChannel.SMS;
    }

    @Override
    protected void send(NotificationDispatchTarget target) {
        throw new UnsupportedOperationException("SMS 발송이 아직 구현되지 않았습니다.");
    }

}
