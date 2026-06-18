package com.sk.domain.mi.dto.response.nextgen.design;

import com.sk.domain.mi.dto.response.nextgen.ScenarioDimensionRes;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * 설계 편집 표의 한 행 (검토 / Signpost 생성 요청)
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
