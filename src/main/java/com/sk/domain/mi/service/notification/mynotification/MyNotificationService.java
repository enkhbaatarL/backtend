package com.sk.domain.mi.service.notification.mynotification;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sk.common.model.PaginationRes;
import com.sk.domain.mi.dto.response.notification.MyNotificationRes;
import com.sk.domain.mi.service.notification.NotificationUser;
import com.sk.domain.shared.entity.master.tmd.EventKeywordNotification;
import com.sk.domain.shared.entity.master.tmd.User;
import com.sk.domain.shared.enums.notification.NotificationSourceType;
import com.sk.domain.shared.repository.master.tlm.projection.NotificationKeyRow;
import com.sk.domain.shared.repository.master.tmd.EventKeywordNotificationRepository;
import com.sk.domain.shared.repository.master.tmd.GoodsCriticalValueSettingRepository;
import com.sk.domain.shared.repository.master.tmd.MyNotificationRepository;
import com.sk.domain.shared.service.permissions.AuthorizationService;

import lombok.RequiredArgsConstructor;

/**
 * My Notifications 통합 조회
 * - 상품 임계치 = 알림 설정한 정형 데이터(데이터소스 단위), 이벤트 키워드 = 키워드 알림 발송 내역 (SKGAS)
 * - 키워드 포함 여부는 알림벨과 같은 기준(NotificationUser.isEventKeywordEnabled)
 * - 목록: UNION ALL 키 페이지(MyNotificationRepository) → 페이지 키만 유형별로 상세 조회 → 원래 순서로 조립
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MyNotificationService {

    private static final char LIKE_ESCAPE = '!';

    private final MyNotificationRepository myNotificationRepository;
    private final GoodsCriticalValueSettingRepository goodsCriticalValueSettingRepository;
    private final EventKeywordNotificationRepository eventKeywordNotificationRepository;
    private final AuthorizationService authorizationService;

    /**
     * [My Notifications 목록]
     */
    public PaginationRes<MyNotificationRes> getMyNotifications(int page, int size, String search, User user) {

        Pageable pageable = PageRequest.of(page - 1, size);

        boolean hasSearch = search != null && !search.isEmpty();

        Page<NotificationKeyRow> keys = myNotificationRepository.findMyNotificationKeys(
            user.getUserNo(), includesEventKeyword(user), hasSearch, hasSearch ? toLikePattern(search) : "%", pageable);

        Map<NotificationSourceType, List<Long>> idsByType = keys.getContent().stream()
            .collect(Collectors.groupingBy(key -> NotificationSourceType.valueOf(key.getNotificationType()),
                Collectors.mapping(NotificationKeyRow::getNotificationId, Collectors.toList())));

        // 페이지 키만 유형별로 상세 조회 (키 조회 이후 삭제된 항목은 건너뜀)
        Map<Long, MyNotificationRes> goodsThresholds = goodsCriticalValueSettingRepository
            .findNotificationSettingGoodsDataSourceByGoodsDataSourceNos(
                idsByType.getOrDefault(NotificationSourceType.GOODS_THRESHOLD, List.of()).stream().map(Long::intValue).toList(), user)
            .stream()
            .collect(Collectors.toMap(row -> row.goodsDataSourceNo().longValue(), MyNotificationRes::ofGoodsThreshold));

        List<Long> eventKeywordIds = idsByType.getOrDefault(NotificationSourceType.EVENT_KEYWORD, List.of());

        Map<Long, MyNotificationRes> eventKeywords = eventKeywordIds.isEmpty() ? Map.of()
            : eventKeywordNotificationRepository.findAllWithKeywordSetting(eventKeywordIds, user.getUserNo()).stream()
                .collect(Collectors.toMap(EventKeywordNotification::getKeywordNotificationId, MyNotificationRes::ofEventKeyword));

        List<MyNotificationRes> content = new ArrayList<>(keys.getNumberOfElements());
        for (NotificationKeyRow key : keys.getContent()) {
            Map<Long, MyNotificationRes> loaded = NotificationSourceType.valueOf(key.getNotificationType()) == NotificationSourceType.GOODS_THRESHOLD
                ? goodsThresholds : eventKeywords;
            MyNotificationRes item = loaded.get(key.getNotificationId());
            if (item != null) {
                content.add(item);
            }
        }
        return PaginationRes.toRes(new PageImpl<>(content, pageable, keys.getTotalElements()));
    }

    private boolean includesEventKeyword(User user) {
        return new NotificationUser(user.getUserNo(), user.getUserLoginId(), authorizationService.getCompanyCd()).isEventKeywordEnabled();
    }

    /** 기존 상품데이터명 검색(QueryDSL containsIgnoreCase)과 같은 규칙: trim → 소문자 → '!' 이스케이프 → %...% */
    private static String toLikePattern(String search) {
        String escaped = search.trim().toLowerCase()
            .replace(String.valueOf(LIKE_ESCAPE), String.valueOf(LIKE_ESCAPE) + LIKE_ESCAPE)
            .replace("%", LIKE_ESCAPE + "%")
            .replace("_", LIKE_ESCAPE + "_");
        return "%" + escaped + "%";
    }
}
