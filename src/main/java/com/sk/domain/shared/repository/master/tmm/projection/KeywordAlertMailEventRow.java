package com.sk.domain.shared.repository.master.tmm.projection;

import java.time.LocalDateTime;

/**
 * 키워드 알림 메일 카드 1행(구분 · 카테고리 · 발생일)용 이벤트 정보
 */
public interface KeywordAlertMailEventRow {

    Boolean getIsNew();

    String getCategoryLabel();

    /** 이벤트 목록 '일시'와 같은 기준: 최신 연관 레코드 최초 생성일시 (없으면 이벤트 생성일시) */
    LocalDateTime getOccurredAt();
}
