package com.sk.domain.mi.dto.response.nextgen;

import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.List;

public record EventVerificationUpdateRes(
    @NotNull @Positive Long miEventId,
    @NotNull String checkedAt,
    @Schema(description = "updated만 저장 성공. unchanged/skipped는 DB 변경 없음")
    @NotNull @Pattern(regexp = "updated|unchanged|skipped") String updateStatus,
    @NotNull String message,
    @NotNull @Valid EventVerificationRes.Item verification,
    @Valid EventVerificationRes.Item postVerification,
    @Schema(description = "변경 직전 상태를 저장한 Snapshot ID")
    @Positive Long miEventSnapshotId,
    @Positive Integer snapshotVersionNo,
    @Positive Long previousMiEventSnapshotId,
    @Schema(description = "DB 수정 시각 원문. 시간대·정밀도 변환 없음") String updatedAt,
    @NotNull @Size(max = 6) List<@NotNull String> changedFields,
    @NotNull @Size(max = 6) List<@NotNull @Valid Change> changes
) {
    public record Change(
        @NotNull String field,
        JsonNode before,
        JsonNode after,
        @NotNull String reason,
        @NotNull List<@NotNull String> sourceUrls
    ) {}
}
