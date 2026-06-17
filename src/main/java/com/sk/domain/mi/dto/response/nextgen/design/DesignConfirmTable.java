package com.sk.domain.mi.dto.response.nextgen.design;

import com.sk.domain.mi.dto.response.nextgen.ScenarioDimensionColumnRes;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * 확정 요청 표 (단계2 편집 일괄 적용). 조회 응답({@link DesignTable})과 동일 구조이되,
 * 행은 전 필드 편집 가능한 확정 전용({@link DesignConfirmRow})으로 받는다.
 */
public record DesignConfirmTable(

    @Schema(description = "동적 표 헤더 정의 (key/label)")
    List<ScenarioDimensionColumnRes> dimensionColumns,

    @Schema(description = "편집 후 시나리오 행 목록")
    List<DesignConfirmRow> scenarios

) {}
