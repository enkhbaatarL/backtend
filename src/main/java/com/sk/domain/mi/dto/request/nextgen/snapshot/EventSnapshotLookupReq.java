package com.sk.domain.mi.dto.request.nextgen.snapshot;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import java.time.LocalDateTime;
import org.springframework.format.annotation.DateTimeFormat;

/**
 * [MCP Snapshot] versionNo 와 asOf 는 동시에 줄 수 없다 (서비스에서 REQ-003). 둘 다 없으면 최신 저장 버전.
 */
public record EventSnapshotLookupReq(

    @Min(value = 1, message = "버전 번호는 1 이상이어야 합니다")
    @Schema(description = "Event 내부 버전 번호 (snapshot_version_no). 버전 번호가 없는 과거 Snapshot 은 이 값으로 찾을 수 없습니다", example = "2")
    Long versionNo,

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    @Schema(description = "기준 시각 (KST, `yyyy-MM-dd'T'HH:mm:ss`). 이 시각까지 저장된 마지막 Snapshot 을 찾습니다", example = "2026-09-15T14:00:00")
    LocalDateTime asOf

) {
}
