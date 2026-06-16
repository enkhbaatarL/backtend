package com.sk.domain.mi.dto.response.nextgen.design;

import com.sk.domain.mi.dto.response.nextgen.ScenarioDimensionRes;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * 설계 편집 표의 한 행 (검토 / Signpost 생성 요청).
 * <p>
 * 이 단계에서 사용자가 편집 가능한 필드만 받는다 — 시나리오명 / 분기기준값(dimensions) / 순서.
 * narrative · 확률 · baseCase · signposts는 확정 단계 또는 Agent 산출이라 여기선 받지 않는다.
 * 응답({@link DesignScenarioRow})과 동일한 필드명을 쓰며, 편집 가능 부분집합이다.
 */
public record DesignEditRow(

    @Schema(description = "시나리오 ID (신규 추가 행이면 null)", example = "90001")
    Long scenarioId,

    @Schema(description = "표시 순서", example = "1")
    Integer displayOrder,

    @Schema(description = "시나리오명", example = "단기 외교 해결")
    String scenarioName,

    @Schema(description = "차원값 목록 (dimensionColumns 순서와 매칭, 신규 분기항목 셀은 value null -> Agent 자동생성)")
    List<ScenarioDimensionRes> dimensions

) {}
