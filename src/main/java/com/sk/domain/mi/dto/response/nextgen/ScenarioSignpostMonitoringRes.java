package com.sk.domain.mi.dto.response.nextgen;

import com.sk.domain.mi.service.nextgen.support.ScenarioProbabilityAllocator;
import com.sk.domain.shared.repository.master.tmm.projection.ScenarioSignpostEventRow;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Signpost 모니터링 현황 (시나리오 상세). 관측 이벤트에 따른 시나리오 확률 업데이트 이력.
 * 확률은 그룹 스케일(v4는 0~1 비율)을 표시 퍼센트로 변환해 내린다 — 시나리오 확률과 동일 규칙.
 */
public record ScenarioSignpostMonitoringRes(

    @Schema(description = "업데이트 일시")
    LocalDateTime updatedAt,

    @Schema(description = "Signpost 이벤트 요약", example = "트럼프 대통령 '전쟁 거의 끝' 발언 및 평화협상 재개 기대")
    String eventSummary,

    @Schema(description = "갱신된 시나리오 확률 (0~100)", example = "54")
    Integer probability,

    @Schema(description = "직전 대비 확률 변화량 (DB 원본 스케일 그대로 - v4 관측 데이터 발생 시 단위 확정 예정)", example = "2.54")
    BigDecimal probabilityDelta,

    @Schema(description = "상세 내역 (Signpost 해석 근거)")
    String rationale

) {

    public static ScenarioSignpostMonitoringRes of(ScenarioSignpostEventRow row, boolean ratioScale) {
        return new ScenarioSignpostMonitoringRes(
            row.occurredAt(),
            row.summary(),
            ScenarioProbabilityAllocator.toDisplayPercent(row.probabilityScore(), ratioScale),
            row.probabilityDelta(),
            row.matchingReason()
        );
    }

}
