package com.sk.domain.mi.dto.response.nextgen;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

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
        @Schema(description = "공개 검증 등급. pass에도 주의·미확인이 포함될 수 있으며 저장 여부와 별개")
        @NotNull @Pattern(regexp = "pass|warning|fail") String status,
        @Schema(description = "verified: 일치 확인, no_contradiction: 명확한 불일치 없음. checks의 주의 사항도 표시")
        @NotNull @Pattern(regexp = "verified|no_contradiction|review_required|no_evidence|search_error|verification_error|contradicted|outdated") String reasonCode,
        @NotNull @Size(max = 300) String review,
        @NotNull List<@NotNull String> links,
        @NotNull @Size(max = 6) List<@NotNull @Valid Check> checks,
        @Schema(description = "Agent가 저장한 원본 검증 ID. 검증 API에서 Event별로 제공하며 수정안 검증에는 null일 수 있음")
        UUID verificationId,
        @Schema(description = "원본 검증 재사용 만료 시각 원문. 응답 수신 시점부터 TTL을 다시 계산하지 않음")
        String expiresAt
    ) {}

    public record Check(
        @NotNull @Pattern(regexp = "entity|location|time|quantity|freshness|attribution") String aspect,
        @NotNull @Pattern(regexp = "matched|caution|contradicted|outdated|unverified|not_applicable") String verdict,
        @NotNull String eventClaim,
        @NotNull String finding,
        @NotNull List<@NotNull String> sourceUrls,
        @Schema(description = "선택적 주의 사유. 저장 조건 판단은 Agent가 수행")
        @Pattern(regexp = "as_of_limitation|source_conflict|insufficient_evidence|other") String cautionReason,
        @Schema(description = "확인 기준 날짜와 이후 미확인 범위를 명시한 수정안 본문 발췌. 없으면 null")
        String qualificationQuote
    ) {}
}
