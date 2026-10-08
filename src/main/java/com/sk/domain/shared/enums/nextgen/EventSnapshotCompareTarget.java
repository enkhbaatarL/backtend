package com.sk.domain.shared.enums.nextgen;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * [MCP Snapshot] Event Snapshot 비교 대상.
 */
@Getter
@RequiredArgsConstructor
public enum EventSnapshotCompareTarget {

    PREVIOUS("previous", "이전 버전 (previous_mi_event_snapshot_id)"),
    CURRENT("current", "현재 Event (tmm_mi_mievent_m)");

    @JsonValue
    private final String code;
    private final String label;

}
