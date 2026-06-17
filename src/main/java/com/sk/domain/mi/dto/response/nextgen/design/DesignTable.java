package com.sk.domain.mi.dto.response.nextgen.design;

import com.sk.domain.mi.dto.response.nextgen.ScenarioDimensionColumnRes;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * 설계 세션 시나리오 표. 조회 응답과 편집 요청(검토/단계2/확정)이 동일 구조로 공유한다.
 * 동적 헤더({@code dimensionColumns}) + 시나리오 행({@code scenarios}).
 */
public record DesignTable(

    @Schema(description = "동적 표 헤더 정의 (그룹별 차원 칼럼)")
    List<ScenarioDimensionColumnRes> dimensionColumns,

    @Schema(description = "시나리오 표 본문 (행 목록)")
    List<DesignScenarioRow> scenarios

) {

    public static DesignTable of(List<ScenarioDimensionColumnRes> dimensionColumns, List<DesignScenarioRow> scenarios) {
        return new DesignTable(dimensionColumns, scenarios);
    }

}
