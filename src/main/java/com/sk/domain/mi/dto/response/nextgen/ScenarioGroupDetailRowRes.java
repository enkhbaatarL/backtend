package com.sk.domain.mi.dto.response.nextgen;

import com.sk.domain.shared.enums.nextgen.ScenarioStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * 그룹 상세 '가정 및 실행 가능성' 표의 한 행. 목록 행 + 판정 상태(status).
 */
public record ScenarioGroupDetailRowRes(

    @Schema(description = "시나리오 ID", example = "11")
    Long scenarioId,

    @Schema(description = "시나리오명", example = "단기 봉쇄와 제한적 공급 대응")
    String scenarioName,

    @Schema(description = "BC(Base Case) 여부", example = "false")
    boolean baseCase,

    @Schema(description = "차원값 목록 (dimensionColumns 순서와 매칭)")
    List<ScenarioDimensionRes> dimensions,

    @Schema(description = "시나리오 발생 확률 (0~100)", example = "30")
    Integer probability,

    @Schema(description = "판정 상태 (pending/realized/discarded)", example = "pending")
    ScenarioStatus status,

    @Schema(description = "판정 상태 라벨", example = "대기")
    String statusLabel

) {}
