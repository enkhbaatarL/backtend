package com.sk.domain.mi.dto.response.nextgen.snapshot;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * [MCP Snapshot] 버전·시점·최신 조회 결과. 이력이 없으면 오류가 아니라 found=false.
 */
public record EventSnapshotLookupRes(

    @Schema(description = "조회 기준 (`versionNo` / `asOf` / `latest`)", allowableValues = {"versionNo", "asOf", "latest"}, example = "asOf")
    String criteria,

    @Schema(description = "조건에 맞는 Snapshot 이 있는지. false 면 이력 없음", example = "true")
    boolean found,

    @Schema(description = "찾은 Snapshot 상세. 없으면 null", nullable = true)
    EventSnapshotDetailRes snapshot

) {

    public static final String CRITERIA_VERSION_NO = "versionNo";
    public static final String CRITERIA_AS_OF = "asOf";
    public static final String CRITERIA_LATEST = "latest";

    public static EventSnapshotLookupRes of(String criteria, EventSnapshotDetailRes snapshot) {
        return new EventSnapshotLookupRes(criteria, snapshot != null, snapshot);
    }

}
