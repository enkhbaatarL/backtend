package com.sk.domain.mi.dto.response.nextgen;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 그룹 일괄 실행의 시나리오 한 건 결과.
 */
public record GroupModelExecutionItemRes(

    @Schema(description = "시나리오 ID", example = "309")
    Long scenarioId,

    @Schema(description = "실행 접수 성공 여부", example = "true")
    boolean success,

    @Schema(description = "실행 접수 결과 (단건 실행 응답과 동일 구조). 실패 시 null", nullable = true)
    ModelExecutionAcceptRes execution,

    @Schema(description = "실패 오류 코드. 성공 시 null", example = "RSC_ALREADY_EXISTS", nullable = true)
    String errorCode,

    @Schema(description = "실패 오류 메시지. 성공 시 null", nullable = true)
    String errorMessage
) {

    public static GroupModelExecutionItemRes success(Long scenarioId, ModelExecutionAcceptRes execution) {
        return new GroupModelExecutionItemRes(scenarioId, true, execution, null, null);
    }

    public static GroupModelExecutionItemRes failure(Long scenarioId, String errorCode, String errorMessage) {
        return new GroupModelExecutionItemRes(scenarioId, false, null, errorCode, errorMessage);
    }
}
