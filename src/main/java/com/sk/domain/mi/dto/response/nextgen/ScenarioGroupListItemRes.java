package com.sk.domain.mi.dto.response.nextgen;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 시나리오 그룹 목록의 한 행.
 */
public record ScenarioGroupListItemRes(

    @Schema(description = "시나리오 그룹 ID", example = "1")
    Long scenarioGroupId,

    @Schema(description = "시나리오 그룹명", example = "카타르 LNG 공급 차질 확산")
    String groupName,

    @Schema(description = "생성일시")
    LocalDateTime createdAt,

    @Schema(description = "시나리오 그룹 요약")
    String summary,

    @Schema(description = "시나리오 수", example = "5")
    int scenarioCount,

    @Schema(description = "동적 표 헤더 정의 (그룹별 차원 칼럼)")
    List<ScenarioDimensionColumnRes> dimensionColumns,

    @Schema(description = "그룹에 속한 시나리오 목록 (표 본문)")
    List<ScenarioRowRes> scenarios

) {}
