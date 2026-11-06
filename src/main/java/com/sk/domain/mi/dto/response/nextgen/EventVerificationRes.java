package com.sk.domain.mi.dto.response.nextgen;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.List;

public record EventVerificationRes(
    @Schema(description = "Agent 검증 기준 시각, UTC ISO 8601. 완료 시각이 아님")
    @NotNull String checkedAt,
    @NotNull @Size(min = 1, max = 5) List<@NotNull @Valid Item> items
) {
    public record Item(
        @NotNull @Positive Long miEventId,
        @NotNull String title,
        @Schema(description = "검증한 DB 수정 시각. null 또는 timezone 없는 문자열일 수 있으며 원문을 보존")
        String sourceUpdatedAt,
        @NotNull @Pattern(regexp = "pass|warning|fail") String status,
        @NotNull @Pattern(regexp = "verified|review_required|no_evidence|search_error|verification_error|contradicted|outdated") String reasonCode,
        @NotNull @Size(max = 300) String review,
        @NotNull List<@NotNull String> links,
        @NotNull @Size(max = 6) List<@NotNull @Valid Check> checks
    ) {}

    public record Check(
        @NotNull @Pattern(regexp = "entity|location|time|quantity|freshness|attribution") String aspect,
        @NotNull @Pattern(regexp = "matched|caution|contradicted|outdated|unverified|not_applicable") String verdict,
        @NotNull String eventClaim,
        @NotNull String finding,
        @NotNull List<@NotNull String> sourceUrls
    ) {}
}
