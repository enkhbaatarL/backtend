package com.sk.domain.mi.service.notification.bell;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sk.common.enums.ResultCode;
import com.sk.common.exception.AppErrorException;
import com.sk.common.model.PaginationRes;
import com.sk.domain.mi.dto.response.notification.BellNotificationRes;
import com.sk.domain.mi.dto.response.notification.BellNotificationUnreadCountRes;
import com.sk.domain.mi.service.notification.NotificationUser;
import com.sk.domain.shared.entity.master.tmd.User;
import com.sk.domain.shared.enums.notification.NotificationSourceType;
import com.sk.domain.shared.repository.master.tlm.BellNotificationRepository;
import com.sk.domain.shared.repository.master.tlm.projection.NotificationKeyRow;
import com.sk.domain.shared.service.permissions.AuthorizationService;

import lombok.RequiredArgsConstructor;

/**
 * 알림벨 통합 조회
 * - 인가는 API(USER)에서, 어떤 알림을 합칠지는 여기서 사용자 회사에 따라 결정한다 (원천의 supports)
 * - 목록: UNION ALL 키 페이지(BellNotificationRepository) → 페이지 키만 원천별로 상세 조회 → 원래 순서로 조립
 * - 노출 기간: 발생 시각 기준 최근 7일 (기존 알림 발송 내역과 동일). 요청당 1회 계산해 모든 분기에 같은 값을 쓴다
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BellNotificationService {

    static final long RETENTION_DAYS = 7;

    private final List<BellNotificationSource> sources;
    private final BellNotificationRepository bellNotificationRepository;
    private final AuthorizationService authorizationService;

    /**
     * [알림벨 목록]
     */
    public PaginationRes<BellNotificationRes> getBellNotifications(int page, int size, User user) {

        NotificationUser notificationUser = toNotificationUser(user);

        Map<NotificationSourceType, BellNotificationSource> activeSources = activeSources(notificationUser);

        Pageable pageable = PageRequest.of(page - 1, size);

        Page<NotificationKeyRow> keys = bellNotificationRepository.findBellNotificationKeys(
            notificationUser.loginUserId(),
            notificationUser.userNo(),
            activeSources.containsKey(NotificationSourceType.EVENT_KEYWORD),
            since(),
            pageable
        );

        // 페이지 키만 유형별로 상세 조회 (키 조회 이후 삭제된 항목은 건너뜀)
        Map<NotificationSourceType, Map<Long, BellNotificationRes>> loaded = new EnumMap<>(NotificationSourceType.class);
        keys.getContent().stream()
            .collect(Collectors.groupingBy(key -> NotificationSourceType.valueOf(key.getNotificationType()), () -> new EnumMap<>(NotificationSourceType.class),
                Collectors.mapping(NotificationKeyRow::getNotificationId, Collectors.toList())))
            .forEach((type, ids) -> loaded.put(type, activeSources.get(type).load(notificationUser, ids)));

        List<BellNotificationRes> content = new ArrayList<>(keys.getNumberOfElements());
        for (NotificationKeyRow key : keys.getContent()) {
            BellNotificationRes item = loaded.getOrDefault(NotificationSourceType.valueOf(key.getNotificationType()), Map.of()).get(key.getNotificationId());
            if (item != null) content.add(item);
        }
        return PaginationRes.toRes(new PageImpl<>(content, pageable, keys.getTotalElements()));
    }

    /**
     * [알림벨 미조회 수]
     */
    public BellNotificationUnreadCountRes getUnreadCount(User user) {

        NotificationUser notificationUser = toNotificationUser(user);

        long unreadCount = bellNotificationRepository.countBellNotificationUnread(
            notificationUser.loginUserId(),
            notificationUser.userNo(),
            activeSources(notificationUser).containsKey(NotificationSourceType.EVENT_KEYWORD),
            since()
        );

        return new BellNotificationUnreadCountRes(unreadCount);
    }

    /**
     * [알림 읽음 처리]
     * - 사용자에게 활성이 아닌 유형이거나 본인 알림이 아니면 없는 알림으로 처리한다
     */
    @Transactional
    public BellNotificationRes markRead(NotificationSourceType notificationType, Long notificationId, User user) {

        NotificationUser notificationUser = toNotificationUser(user);

        BellNotificationSource source = activeSources(notificationUser).get(notificationType);

        if (source == null) {
            throw notFound(notificationType, notificationId);
        }
        return source.markRead(notificationUser, notificationId).orElseThrow(() -> notFound(notificationType, notificationId));
    }

    private Map<NotificationSourceType, BellNotificationSource> activeSources(NotificationUser user) {
        return sources.stream()
            .filter(source -> source.supports(user))
            .collect(Collectors.toMap(BellNotificationSource::type, source -> source, (first, second) -> first, () -> new EnumMap<>(NotificationSourceType.class)));
    }

    private NotificationUser toNotificationUser(User user) {
        return new NotificationUser(user.getUserNo(), user.getUserLoginId(), authorizationService.getCompanyCd());
    }

    private LocalDateTime since() {
        return LocalDateTime.now().minusDays(RETENTION_DAYS);
    }

    private static AppErrorException notFound(NotificationSourceType notificationType, Long notificationId) {
        return AppErrorException.of(ResultCode.Error.RSC_NOT_FOUND, "Notification not found: %s:%d".formatted(notificationType, notificationId));
    }

}
