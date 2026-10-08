package com.sk.domain.shared.enums.nextgen;

import java.util.Arrays;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * [MCP Snapshot] Event Snapshot 생성·갱신 경로 (tmm_mi_mievent_snapshot_m.update_trigger_cd).
 * 엔티티는 코드 문자열을 그대로 들고, 응답에서 정의된 코드만 label 을 붙인다 (모르는 코드가 와도 예외가 나지 않게).
 */
@Getter
@RequiredArgsConstructor
public enum EventSnapshotUpdateTrigger {

    RECORD_INGESTION("RECORD_INGESTION", "파이프라인 Record 반영"),
    MCP_USER_EDIT("MCP_USER_EDIT", "MCP 사용자 수정");

    @JsonValue
    private final String code;
    private final String label;

    public static Optional<EventSnapshotUpdateTrigger> fromCode(String code) {
        return Arrays.stream(values())
            .filter(trigger -> trigger.code.equals(code))
            .findFirst();
    }

}
