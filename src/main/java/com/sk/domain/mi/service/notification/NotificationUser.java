package com.sk.domain.mi.service.notification;

import com.sk.domain.shared.enums.PermissionCondition;

/**
 * 알림 조회 사용자 (알림벨·My Notifications 공통 — 유형 활성 판단·소유권 조건에 쓰는 값만 보관)
 *
 * @param userNo      사용자번호
 * @param loginUserId 사용자 로그인 ID (상품 알림 수신자 기준)
 * @param companyCd   회사코드 (원천 활성 판단)
 */
public record NotificationUser(

    Integer userNo,

    String loginUserId,

    String companyCd

) {

    /** 이벤트 키워드 알림 포함 여부 (SKGAS 전용) */
    public boolean isEventKeywordEnabled() {
        return PermissionCondition.AllowedCompany.SKGAS.getCode().equals(companyCd);
    }
}
