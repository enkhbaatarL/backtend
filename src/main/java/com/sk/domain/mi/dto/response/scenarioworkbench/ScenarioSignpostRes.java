package com.sk.domain.mi.dto.response.scenarioworkbench;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

public record ScenarioSignpostRes(

    @Schema(description = "핵심이벤트", example = "3/11 호르무즈 완전 봉쇄 + 150+ 선박 대기")
    String keyEvent,

    @Schema(description = "현재 상태 요약", example = "물리적 통항 차단 지속")
    String currentStatusSummary,

    @Schema(description = "시장 영향 해석", example = "LNG 및 원유 공급 병목 현실화 → 가격 상방...")
    String marketImpactInterpretation,

    @Schema(description = "관찰 포인트 (Trigger)", example = "대기 선박 증가 여부 / 실물 운송 재개 여부")
    String observationTrigger,

    @ArraySchema(
        arraySchema = @Schema(description = "출처 목록"),
        schema = @Schema(description = "출처", example = "MarineTraffic")
    )
    List<String> sources

) {
}
