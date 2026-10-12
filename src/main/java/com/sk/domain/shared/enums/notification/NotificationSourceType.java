package com.sk.domain.shared.enums.notification;

/**
 * 알림 유형 (알림벨·My Notifications 공통).
 * 유형마다 원천 테이블이 다르며, 활성 유형의 원천을 UNION ALL로 합쳐 조회한다.
 */
public enum NotificationSourceType {

    /** 상품 임계치
     * (알림벨: tlm_emlsendl 발송 내역, My Notifications: tmd_gdscrtvlsetl 설정) — MI 공통
     */
    GOODS_THRESHOLD,

    /**
     * 이벤트 키워드 알림 (tmd_mi_evnt_keywdnotil) — SKGAS
     */
    EVENT_KEYWORD

}
