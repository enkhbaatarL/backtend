package com.sk.domain.mi.dto.response.nextgen;

import com.sk.domain.shared.repository.master.tmm.projection.ScenarioGroupBaseRow;
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

    @Schema(description = "생성일시 (최신순 정렬·기간 필터 기준)")
    LocalDateTime createdAt,

    @Schema(description = "수정일시")
    LocalDateTime updatedAt,

    @Schema(description = "시나리오 그룹 요약")
    String summary,

    @Schema(description = "시나리오 수", example = "5")
    int scenarioCount,

    @Schema(description = "동적 표 헤더 정의 (그룹별 차원 칼럼)")
    List<ScenarioDimensionColumnRes> dimensionColumns,

    @Schema(description = "그룹에 속한 시나리오 목록 (표 본문)")
    List<ScenarioRowRes> scenarios

) {

    /** dimensionColumns/scenarios 는 template 병합이 필요해 외부에서 조립한 값을 받는다. */
    public static ScenarioGroupListItemRes of(
        ScenarioGroupBaseRow base,
        List<ScenarioDimensionColumnRes> dimensionColumns,
        List<ScenarioRowRes> scenarios
    ) {
        return new ScenarioGroupListItemRes(
            base.scenarioGroupId(),
            base.title(),
            base.createDt(),
            base.modifyDt(),
            base.summary(),
            base.scenarioCount() == null ? 0 : base.scenarioCount(),
            dimensionColumns,
            scenarios
        );
    }

}
