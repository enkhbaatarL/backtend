package com.sk.domain.shared.repository.master.tmm.projection;

/**
 * 이벤트-레코드 매핑 근거 (확정 매핑 tmm_mi_mievent_record_map.match_reason).
 */
public interface EventRecordMappingRow {

    Integer getMiRecordId();

    String getMatchReason();

}
