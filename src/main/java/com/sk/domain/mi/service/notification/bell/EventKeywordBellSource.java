package com.sk.domain.mi.service.notification.bell;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.sk.domain.mi.dto.response.notification.BellNotificationRes;
import com.sk.domain.mi.service.notification.NotificationUser;
import com.sk.domain.shared.enums.notification.NotificationSourceType;
import com.sk.domain.shared.repository.master.tmd.EventKeywordNotificationRepository;

import lombok.RequiredArgsConstructor;

/**
 * 알림벨 원천 — 이벤트 키워드 알림 (tmd_mi_evnt_keywdnotil, SKGAS 전용)
 * - 활성: 회사코드 SKGAS
 * - 조건: 본인(usr_no) + 미삭제 (기간·정렬은 BellNotificationRepository)
 */
@Component
@RequiredArgsConstructor
public class EventKeywordBellSource implements BellNotificationSource {

    private final EventKeywordNotificationRepository eventKeywordNotificationRepository;

    @Override
    public NotificationSourceType type() {
        return NotificationSourceType.EVENT_KEYWORD;
    }

    @Override
    public boolean supports(NotificationUser user) {
        return user.isEventKeywordEnabled();
    }

    @Override
    public Map<Long, BellNotificationRes> load(NotificationUser user, List<Long> notificationIds) {
        return eventKeywordNotificationRepository.findAllWithKeywordSetting(notificationIds, user.userNo()).stream()
            .map(BellNotificationRes::ofEventKeyword)
            .collect(Collectors.toMap(BellNotificationRes::notificationId, Function.identity()));
    }

    @Override
    public Optional<BellNotificationRes> markRead(NotificationUser user, Long notificationId) {
        return eventKeywordNotificationRepository
            .findByKeywordNotificationIdAndUser_UserNoAndIsDeletedFalse(notificationId, user.userNo())
            .map(notification -> {
                notification.markAsRead();
                return BellNotificationRes.ofEventKeyword(notification);
            });
    }
}
