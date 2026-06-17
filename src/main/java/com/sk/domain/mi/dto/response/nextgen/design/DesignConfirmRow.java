package com.sk.domain.mi.dto.response.nextgen.design;

import com.sk.domain.mi.dto.response.nextgen.ScenarioDimensionRes;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * 확정 요청 표의 한 행 (단계2 편집 일괄 적용).
 * <p>
 * 확정 단계는 전 필드 편집 가능 — 시나리오명/분기기준값/순서 + narrative/확률/baseCase/Signpost.
 * 응답({@link DesignScenarioRow})과 동일 필드명을 쓰되, Signpost는 편집 전용({@link DesignConfirmSignpost})으로 받는다.
 */
public record DesignConfirmRow(

    @Schema(description = "시나리오 ID (신규 추가 행이면 null)", example = "90001")
    Long scenarioId,

    @Schema(description = "표시 순서", example = "1")
    Integer displayOrder,

    @Schema(description = "시나리오명", example = "단기 외교 해결")
    String scenarioName,

    @Schema(description = "BC(Base Case) 여부", example = "false")
    boolean baseCase,

    @Schema(description = "차원값 목록 (dimensionColumns 순서와 매칭)")
    List<ScenarioDimensionRes> dimensions,

    @Schema(description = "시나리오 발생 확률 (0~100)", example = "75")
    Integer probability,

    @Schema(description = "시나리오 내러티브")
    String narrative,

    @Schema(description = "시나리오별 편집 Signpost")
    List<DesignConfirmSignpost> signposts

) {}
