package com.sk.domain.mi.dto.response.nextgen;

import com.sk.domain.shared.enums.nextgen.ScenarioStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * 시나리오 판정 상태 일괄 변경 결과 (그룹 전체 모두 대기/실현/폐기).
 */
public record ScenarioBulkStatusRes(

    @Schema(description = "일괄 적용된 판정 상태 (pending/realized/discarded)", example = "realized")
    ScenarioStatus status,

    @Schema(description = "판정 상태 라벨", example = "실현")
    String statusLabel,

    @Schema(description = "변경된 시나리오 수", example = "6")
    int updatedCount,

    @Schema(description = "변경된 시나리오 ID 목록")
    List<Long> scenarioIds

) {}
