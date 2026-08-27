package com.sk.domain.mi.dto.response.nextgen.design;

import com.sk.domain.shared.enums.nextgen.AdditionalDimensionGenerationStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * 추가 분기항목 후보 조회 응답
 */
public record AdditionalDimensionsRes(

    @Schema(description = "시나리오 그룹 ID", example = "194")
    Long scenarioGroupId,

    @Schema(description = "후보 생성 상태 (not_requested/queued/running/ready/failed/interrupted/discarded, 미지 값은 null)", example = "ready")
    AdditionalDimensionGenerationStatus generationStatus,

    @Schema(description = "후보 생성 상태 표시명", example = "준비완료")
    String generationStatusLabel,

    @Schema(description = "아직 채택되지 않은 추가 분기항목 후보 목록 (ready이면서 빈 배열이면 추가 가능한 후보 없음 — 정상)")
    List<AdditionalDimensionCandidate> candidates,

    @Schema(description = "반환된 후보 개수", example = "1")
    Integer availableCount,

    @Schema(description = "상태 또는 후보가 마지막으로 갱신된 시각 (Agent 원문, ISO 8601)", example = "2026-08-19T10:00:20Z")
    String updatedAt,

    @Schema(description = "후보 생성 실패·중단 시의 오류 코드", example = "ADDITIONAL_DIMENSION_GENERATION_FAILED")
    String errorCode

) {

    /** 추가 분기항목 후보 한 건 (presentation의 분기항목 정의와 동형). */
    public record AdditionalDimensionCandidate(

        @Schema(description = "분기항목 키 (채택 시 표 헤더 key로 사용)", example = "d7")
        String key,

        @Schema(description = "분기항목 이름", example = "PSV 프리미엄 간헐 유지")
        String label,

        @Schema(description = "분기기준 설명 (사용자가 판단할 불확실성)", example = "분석 기준일 이후 이탈리아 PSV-TTF 프리미엄이 유지되는가?")
        String description,

        @Schema(description = "분기항목 선정 근거", example = "PSV 프리미엄 유지 여부에 따라 유럽 내 LNG 유입 유인과 JKM 압력이 달라짐")
        String rationale,

        @Schema(description = "수급모델 파라미터 지원 상태 (full/price_only 등)", example = "full")
        String modelSupportStatus,

        @Schema(description = "선택 가능한 분기값 목록")
        List<CandidateValue> values

    ) {}

    /** 후보 분기항목의 분기값 한 건. */
    public record CandidateValue(

        @Schema(description = "분기값 ID", example = "d7_v1")
        String valueId,

        @Schema(description = "분기값 표시명", example = "선적 확대가 단기간 내 종료")
        String label,

        @Schema(description = "분기값 선택 근거", example = "공급 완화 효과가 제한되는 가정")
        String rationale,

        @Schema(description = "JKM 압력 기여 (bullish/bearish/balanced)", example = "bullish")
        String pressureContribution

    ) {}

    public static AdditionalDimensionsRes of(Long scenarioGroupId, AdditionalDimensionGenerationStatus generationStatus,
                                             List<AdditionalDimensionCandidate> candidates, Integer availableCount,
                                             String updatedAt, String errorCode) {
        return new AdditionalDimensionsRes(
            scenarioGroupId,
            generationStatus,
            generationStatus == null ? null : generationStatus.getLabel(),
            candidates,
            availableCount,
            updatedAt,
            errorCode
        );
    }

}
