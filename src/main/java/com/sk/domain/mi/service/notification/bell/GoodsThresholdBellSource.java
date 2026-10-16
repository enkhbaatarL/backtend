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
import com.sk.domain.shared.repository.master.tlm.EmailAccessLogRepository;

import lombok.RequiredArgsConstructor;

/**
 * 알림벨 원천 — 상품 임계치 알림 (tlm_emlsendl, MI 공통이라 모든 사용자에게 활성)
 * - 조건은 기존 알림 발송 내역 목록과 동일: 본인 수신(로그인 ID) + 미삭제 (기간·정렬은 BellNotificationRepository)
 * - 항목은 기존 응답(MiNotificationRes)을 그대로 담는다
 */
@Component
@RequiredArgsConstructor
public class GoodsThresholdBellSource implements BellNotificationSource {

    private final EmailAccessLogRepository emailAccessLogRepository;

    @Override
    public NotificationSourceType type() {
        return NotificationSourceType.GOODS_THRESHOLD;
    }

    @Override
    public boolean supports(NotificationUser user) {
        return true;
    }

    @Override
    public Map<Long, BellNotificationRes> load(NotificationUser user, List<Long> notificationIds) {
        List<Integer> emailSendSns = notificationIds.stream().filter(GoodsThresholdBellSource::isEmailSendSnRange).map(Long::intValue).toList();
        return emailAccessLogRepository.findNotificationsByEmailSendSns(emailSendSns, user.loginUserId()).stream()
            .map(BellNotificationRes::ofGoodsThreshold)
            .collect(Collectors.toMap(BellNotificationRes::notificationId, Function.identity()));
    }

    /**
     * 기존 읽음 처리와 같은 eml_inqr_yn을 갱신한 뒤, 목록과 같은 프로젝션으로 다시 읽어 반환한다
     * (MiNotificationRes.of는 상품 설정이 없는 행에서 NPE가 나므로 쓰지 않음)
     */
    @Override
    public Optional<BellNotificationRes> markRead(NotificationUser user, Long notificationId) {
        if (!isEmailSendSnRange(notificationId)) {
            return Optional.empty();
        }
        return emailAccessLogRepository
            .findByEmailSendSnAndEmailReceiveUserLoginIdAndIsDeletedFalse(notificationId.intValue(), user.loginUserId())
            .flatMap(emailAccessLog -> {
                emailAccessLog.updateInquiry();
                return Optional.ofNullable(load(user, List.of(notificationId)).get(notificationId));
            });
    }

    /** eml_send_sn은 int — 범위를 넘는 ID를 intValue로 바꾸면 다른 알림과 겹치므로 없는 알림으로 취급한다 */
    private static boolean isEmailSendSnRange(Long notificationId) {
        return notificationId != null && notificationId > 0 && notificationId <= Integer.MAX_VALUE;
    }
}
