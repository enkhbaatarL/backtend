package com.sk.domain.mi.dto.response.nextgen;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * 그룹 일괄 실행 결과. 부분 실패가 있어도 HTTP 200이며 시나리오별 결과로 판별한다.
 */
public record GroupModelExecutionRes(

    @Schema(description = "시나리오 그룹 ID", example = "61")
    Long scenarioGroupId,

    @Schema(description = "요청 시나리오 수", example = "4")
    int totalCount,

    @Schema(description = "실행 접수 성공 수", example = "3")
    int successCount,

    @Schema(description = "실행 접수 실패 수", example = "1")
    int failureCount,

    @Schema(description = "시나리오별 실행 접수 결과 (요청 순서)")
    List<GroupModelExecutionItemRes> results
) {

    public static GroupModelExecutionRes of(Long scenarioGroupId, List<GroupModelExecutionItemRes> results) {
        int successCount = (int) results.stream().filter(GroupModelExecutionItemRes::success).count();
        return new GroupModelExecutionRes(
            scenarioGroupId,
            results.size(),
            successCount,
            results.size() - successCount,
            results
        );
    }
}
