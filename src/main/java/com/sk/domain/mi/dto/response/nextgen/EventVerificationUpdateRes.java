package com.sk.domain.mi.dto.response.nextgen;

import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

public record EventVerificationUpdateRes(
    @NotNull @Positive Long miEventId,
    @Schema(description = "이번 업데이트에 사용한 원본 검증 ID") @NotNull UUID verificationId,
    @NotNull String checkedAt,
    @Schema(description = "저장 결과. 공개 검증 status와 별개이며 updated만 저장 성공. unchanged/skipped는 DB 변경 없음")
    @NotNull @Pattern(regexp = "updated|unchanged|skipped") String updateStatus,
    @NotNull String message,
    @Schema(description = "변경 전 DB Event의 공개 검증 결과")
    @NotNull @Valid EventVerificationRes.Item verification,
    @Schema(description = "저장 전 수정안 검증 결과. skipped이면 미저장 수정안이므로 현재 Event 검증 결과를 덮어쓰지 않음")
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
