package com.sk.domain.mi.dto.response.nextgen;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

/**
 * 개별 시나리오가 참조하는 부모 시나리오 그룹 요약.
 */
public record ScenarioGroupRefRes(

    @Schema(description = "시나리오 그룹 ID", example = "1")
    Long scenarioGroupId,

    @Schema(description = "시나리오 그룹명", example = "카타르 LNG 공급 차질 확산")
    String groupName,

    @Schema(description = "그룹 생성일시")
    LocalDateTime createdAt

) {}
