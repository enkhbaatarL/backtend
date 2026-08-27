package com.sk.domain.shared.repository.master.tmm.projection;

import java.math.BigDecimal;

/**
 * 이벤트-레코드 매핑 근거 (Event 후보 단계 산출물).
 * 확정 매핑 테이블에는 근거가 없어 후보 매핑(tmm_mi_mievent_candidate_record_map)에서 역추적한다.
 */
public interface EventRecordMappingRow {

    Integer getMiRecordId();

    BigDecimal getMappingConfidence();

    String getMappingReason();

}
