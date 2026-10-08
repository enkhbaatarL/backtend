package com.sk.domain.shared.repository.master.tmm.projection;

import java.time.LocalDateTime;

/**
 * 키워드 알림 스캔 대상 이벤트 (신규 생성 이벤트)
 */
public interface KeywordAlertEventRow {

    Long getMiEventId();

    String getOneLineSummary();

    LocalDateTime getCreateDt();
}
