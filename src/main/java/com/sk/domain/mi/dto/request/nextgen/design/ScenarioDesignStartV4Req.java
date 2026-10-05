package com.sk.domain.mi.dto.request.nextgen.design;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * 멀티 이벤트 시나리오 설계 시작 요청
 */
public record ScenarioDesignStartV4Req(

    @Schema(description = "설계 대상 이벤트 ID 목록 (1~5개, 선택 순서 보존)", example = "[1463, 1464, 1433]")
    @NotEmpty
    @Size(min = 1, max = 5)
    List<Long> miEventIds

) {}
