package com.sk.domain.mi.dto.request.nextgen.design;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * 멀티 이벤트 시나리오 설계 시작 요청
 */
public record ScenarioDesignStartV4Req(

    @Schema(description = "설계 대상 이벤트 ID 목록 (1~5개, 선택 순서 보존)", example = "[1463, 1464, 1433]")
    @NotEmpty
    @Size(min = 1, max = 5)
    List<Long> miEventIds,

    @Schema(description = "커서가 속한 operation ID (accepted에서 저장한 값) - lastSequence와 쌍으로 전송", example = "01KZQJK500D0PE7XVQHXJFMHDY", nullable = true)
    String operationId,

    @Schema(description = "마지막으로 처리한 SSE sequence (재연결 대상 operation과 일치할 때만 그 다음부터 replay, 그 외 0부터 전체 replay)", example = "7", nullable = true)
    @PositiveOrZero
    Integer lastSequence

) {}
