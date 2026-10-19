package com.sk.domain.shared.repository.master.tmm.projection;

import java.time.LocalDateTime;

/**
 * 키워드 알림 메일 카드 1행(구분 · 카테고리 · 발생일)용 이벤트 정보
 */
public interface KeywordAlertMailEventRow {

    Boolean getIsNew();

    String getCategoryLabel();

    /** 이벤트 상세 '갱신일시'와 같은 기준: 최신 연관 레코드 최초 생성일시 (없으면 이벤트 생성일시). DB 값(UTC) 그대로이므로 표시할 때 KST로 변환 */
    LocalDateTime getOccurredAt();
}
