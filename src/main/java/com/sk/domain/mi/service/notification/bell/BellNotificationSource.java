package com.sk.domain.mi.service.notification.bell;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.sk.domain.mi.dto.response.notification.BellNotificationRes;
import com.sk.domain.mi.service.notification.NotificationUser;
import com.sk.domain.shared.enums.notification.NotificationSourceType;

/**
 * 알림벨 원천 (알림 유형 1개 = 구현 1개)
 * - 정렬·페이징·건수는 BellNotificationRepository(UNION ALL)가 담당하고, 원천은 유형별 상세 조회와 읽음 처리만 맡는다
 */
public interface BellNotificationSource {

    NotificationSourceType type();

    /** 이 사용자의 알림벨에 포함하는지 (회사·모듈별 활성화) */
    boolean supports(NotificationUser user);

    /** 원천 ID → 알림벨 항목. 없거나 소유자가 아닌 ID는 결과에서 빠진다 */
    Map<Long, BellNotificationRes> load(NotificationUser user, List<Long> notificationIds);

    /** 읽음 처리 후 항목 반환. 없거나 소유자가 아니면 empty */
    Optional<BellNotificationRes> markRead(NotificationUser user, Long notificationId);
}
