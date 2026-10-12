package com.sk.domain.mi.service.nextgen.keywordalert.dispatch;

import com.sk.domain.shared.entity.master.tmd.EventKeywordNotification;
import com.sk.domain.shared.entity.master.tmd.EventKeywordNotificationDelivery;
import com.sk.domain.shared.enums.notification.NotificationChannel;
import java.time.LocalDateTime;

/**
 * 발송 1건 내용 (트랜잭션 밖에서 쓰도록 엔티티 대신 값으로 전달)
 * - 수신 주소는 담지 않음 (디스패처가 발송 시점에 결정)
 */
public record NotificationDispatchTarget(

    Long keywordSendId,

    NotificationChannel channel,

    Long keywordNotificationId,

    Long miEventId,

    String keywordName,

    String eventName,

    LocalDateTime notifiedAt,

    Integer userNo,

    String userLoginId,

    String userName

) {

    public static NotificationDispatchTarget of(EventKeywordNotificationDelivery delivery) {
        EventKeywordNotification notification = delivery.getNotification();
        return new NotificationDispatchTarget(
                delivery.getKeywordSendId(),
                delivery.getChannel(),
                notification.getKeywordNotificationId(),
                notification.getMiEventId(),
                notification.getKeywordSnapshotName(),
                notification.getEventSnapshotName(),
                notification.getCreateDt(),
                notification.getUser().getUserNo(),
                notification.getUser().getUserLoginId(),
                notification.getUser().getUserNm()
        );
    }

}
