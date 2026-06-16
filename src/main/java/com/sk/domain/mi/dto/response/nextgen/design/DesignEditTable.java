package com.sk.domain.mi.dto.response.nextgen.design;

import com.sk.domain.mi.dto.response.nextgen.ScenarioDimensionColumnRes;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * 설계 편집 표 (검토 / Signpost 생성 요청). 조회 응답({@link DesignTable})과 동일 구조이되,
 * 행은 편집 가능 부분집합({@link DesignEditRow})만 받는다.
 */
public record DesignEditTable(

    @Schema(description = "동적 표 헤더 정의 (편집 후 분기항목, key/label)")
    List<ScenarioDimensionColumnRes> dimensionColumns,

    @Schema(description = "편집 후 시나리오 행 목록 (미채번 행=신규, 빠진 행=삭제)")
    List<DesignEditRow> scenarios

) {}
