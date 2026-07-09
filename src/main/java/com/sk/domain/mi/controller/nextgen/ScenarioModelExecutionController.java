package com.sk.domain.mi.controller.nextgen;

import com.sk.common.annotation.ApiOrder;
import com.sk.common.annotation.PermissionRule;
import com.sk.common.annotation.RequiredPermissionRules;
import com.sk.common.enums.ResultCode;
import com.sk.common.model.BaseResponse;
import com.sk.common.model.BaseResponseFactory;
import com.sk.domain.mi.dto.response.nextgen.ModelExecutionStatusRes;
import com.sk.domain.mi.service.nextgen.ScenarioModelExecutionService;
import com.sk.domain.mi.service.nextgen.mock.ScenarioModelExecutionMockService;
import com.sk.domain.shared.enums.PermissionCondition;
import com.sk.domain.shared.enums.Role;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Profile("!dedicated")
@Tag(name = "05-1. Next Gen MI Model Execution API", description = "Next Gen MI MI 수급모델 실행 API입니다.")
@Slf4j
@RestController
@RequestMapping("/api/v1/mi/next-gen/scenarios/{scenarioId}")
@RequiredArgsConstructor
public class ScenarioModelExecutionController {

    private final ScenarioModelExecutionService scenarioModelExecutionService;
    private final ScenarioModelExecutionMockService scenarioModelExecutionMockService;

    @ApiOrder(5)
    @Operation(
        summary = "MI 수급모델 실행 상태 조회 API",
        description = "MI 수급모델 실행 비동기 리소스의 lifecycle 상태를 조회합니다."
    )
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @GetMapping("/model-executions/{runId}")
    @RequiredPermissionRules(rules = {
        @PermissionRule(role = Role.USER, conditions = {PermissionCondition.SKGAS_COMPANY_USER_ONLY}),
        @PermissionRule(role = Role.ADMIN, conditions = {PermissionCondition.THIRD_PARTY_ADMIN_ONLY})
    })
    public BaseResponse<ModelExecutionStatusRes> getModelExecutionStatus(
        @Parameter(description = "시나리오 ID", example = "1") @PathVariable Long scenarioId,
        @Parameter(description = "yyyymmdd_hhmmss_XXXX 형식 실행 추적 ID", example = "20260619_143500_A1B2") @PathVariable String runId
    ) {
        return BaseResponseFactory.create(
            scenarioModelExecutionService.getModelExecutionStatus(scenarioId, runId)
        );
    }

    @ApiOrder(6)
    @Operation(
        summary = "[mockapi 전용] 수급모델 실행 데이터 초기화 API",
        description = """
            Mock 수급모델 실행 데이터를 초기 상태로 리셋합니다. <br>
            테스트/데모 반복 시 사용합니다.
            """
    )
    @ApiResponse(responseCode = "200", description = "초기화 성공")
    @PostMapping("/model-executions/reset")
    @RequiredPermissionRules(rules = {
        @PermissionRule(role = Role.USER, conditions = {PermissionCondition.SKGAS_COMPANY_USER_ONLY}),
        @PermissionRule(role = Role.ADMIN, conditions = {PermissionCondition.THIRD_PARTY_ADMIN_ONLY})
    })
    public BaseResponse<Void> resetModelExecutions(
        @Parameter(description = "시나리오 ID", example = "1") @PathVariable Long scenarioId
    ) {
        scenarioModelExecutionMockService.reset();
        return BaseResponseFactory.create(ResultCode.Success.OK);
    }
}
