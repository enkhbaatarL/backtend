package com.sk.domain.shared.repository.master.tmm.projection;

import java.time.LocalDateTime;

/**
 * [MCP Snapshot] Snapshot 근거 Record 행 (native). 사본 컬럼(miRecordId·recordType·refNm·recordFirstCreated) + 원본 Record 의 현재 값.
 * 원본이 삭제됐거나 조인되지 않으면 원본 컬럼은 null 이고 sourceAvailable 은 0 이다.
 */
public interface EventSnapshotRecordRow {
    Long getMiRecordId();
    String getRecordType();
    String getRefNm();
    LocalDateTime getRecordFirstCreated();
    String getTitle();
    String getSummary();
    Integer getGoodsNo();
    Integer getGoodsDataNo();
    Long getSourceObjId();
    Integer getSourceAvailable();
}
