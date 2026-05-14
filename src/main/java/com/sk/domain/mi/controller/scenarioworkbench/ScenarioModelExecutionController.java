package com.sk.domain.mi.controller.scenarioworkbench;

import com.sk.common.annotation.ApiOrder;
import com.sk.common.annotation.RequiredPermission;
import com.sk.common.annotation.RequiredPermissionRules;
import com.sk.common.enums.ResultCode;
import com.sk.common.model.BaseResponse;
import com.sk.common.model.BaseResponseFactory;
import com.sk.domain.mi.dto.request.scenarioworkbench.ModelExecutionCallbackReq;
import com.sk.domain.mi.dto.request.scenarioworkbench.ModelExecutionSubmitReq;
import com.sk.domain.mi.dto.response.scenarioworkbench.ModelExecutionAcceptRes;
import com.sk.domain.mi.dto.response.scenarioworkbench.ModelExecutionParameterRes;
import com.sk.domain.mi.dto.response.scenarioworkbench.ModelExecutionStatusRes;
import com.sk.domain.mi.service.scenarioworkbench.ScenarioModelExecutionService;
import com.sk.domain.mi.service.scenarioworkbench.mock.ScenarioModelExecutionMockService;
import com.sk.common.annotation.PermissionRule;
import com.sk.domain.shared.enums.PermissionCondition;
import com.sk.domain.shared.enums.Role;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "2-1. Scenario Workbench Model Execution API", description = "Scenario Workbench MI 수급모델 실행 API입니다.")
@Slf4j
@RestController
@RequestMapping("/api/v1/mi/scenario-workbench/scenarios/{scenarioId}")
@RequiredArgsConstructor
public class ScenarioModelExecutionController {

    private final ScenarioModelExecutionService scenarioModelExecutionService;
    private final ScenarioModelExecutionMockService scenarioModelExecutionMockService;

    @ApiOrder(3)
    @Operation(
        summary = "MI 수급모델 실행 파라미터 조회 API",
        description = "Scenario 상세 화면의 MI 수급모델 실행 팝업을 초기화하기 위한 parameter metadata와 기본값을 조회합니다."
    )
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @GetMapping("/model-execution-parameters")
    @RequiredPermissionRules(rules = {
        @PermissionRule(role = Role.USER, conditions = {PermissionCondition.SKGAS_COMPANY_USER_ONLY}),
        @PermissionRule(role = Role.ADMIN, conditions = {PermissionCondition.THIRD_PARTY_ADMIN_ONLY})
    })
    public BaseResponse<ModelExecutionParameterRes> getModelExecutionParameters(
        @Parameter(description = "시나리오 ID", example = "1") @PathVariable Long scenarioId
    ) {
        return BaseResponseFactory.create(
            scenarioModelExecutionService.getModelExecutionParameters(scenarioId)
        );
    }

    @ApiOrder(4)
    @Operation(
        summary = "MI 수급모델 실행 접수 API",
        description = "사용자가 조정한 parameter로 MI 수급모델 실행 비동기 리소스 생성을 요청합니다."
    )
    @ApiResponse(responseCode = "202", description = "비동기 실행 접수 성공")
    @PostMapping("/model-executions")
    @RequiredPermissionRules(rules = {
        @PermissionRule(role = Role.USER, conditions = {PermissionCondition.SKGAS_COMPANY_USER_ONLY}),
        @PermissionRule(role = Role.ADMIN, conditions = {PermissionCondition.THIRD_PARTY_ADMIN_ONLY})
    })
    public ResponseEntity<BaseResponse<ModelExecutionAcceptRes>> submitModelExecution(
        @Parameter(description = "시나리오 ID", example = "1") @PathVariable Long scenarioId,
        @Valid @RequestBody ModelExecutionSubmitReq request
    ) {
        ModelExecutionAcceptRes response = scenarioModelExecutionService.submitModelExecution(scenarioId, request);
        return ResponseEntity.status(HttpStatus.ACCEPTED)
            .body(BaseResponseFactory.create(response));
    }

    @ApiOrder(5)
    @Operation(
        summary = "MI 수급모델 실행 상태 조회 API",
        description = "MI 수급모델 실행 비동기 리소스의 lifecycle 상태를 조회합니다."
    )
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @GetMapping("/model-executions/{executionId}")
    @RequiredPermissionRules(rules = {
        @PermissionRule(role = Role.USER, conditions = {PermissionCondition.SKGAS_COMPANY_USER_ONLY}),
        @PermissionRule(role = Role.ADMIN, conditions = {PermissionCondition.THIRD_PARTY_ADMIN_ONLY})
    })
    public BaseResponse<ModelExecutionStatusRes> getModelExecutionStatus(
        @Parameter(description = "시나리오 ID", example = "1") @PathVariable Long scenarioId,
        @Parameter(description = "실행 리소스 ID", example = "exec_a1b2c3d4e5f6") @PathVariable String executionId
    ) {
        return BaseResponseFactory.create(
            scenarioModelExecutionService.getModelExecutionStatus(scenarioId, executionId)
        );
    }

    @ApiOrder(6)
    @Operation(
        summary = "MI 수급모델 실행 콜백 API",
        description = "외부 모델 API가 실행 완료 또는 실패 시 호출하여 execution 상태를 terminal 상태로 갱신합니다."
    )
    @ApiResponse(responseCode = "200", description = "콜백 처리 성공")
    @PostMapping("/model-executions/{executionId}/callback")
    @RequiredPermissionRules(rules = {
        @PermissionRule(role = Role.SYSTEM),
        @PermissionRule(role = Role.API_SUB_ADMIN),
        @PermissionRule(role = Role.API_PLUS_OPERATOR),
        @PermissionRule(role = Role.USER, conditions = {PermissionCondition.SKGAS_COMPANY_USER_ONLY}),
        @PermissionRule(role = Role.ADMIN, conditions = {PermissionCondition.THIRD_PARTY_ADMIN_ONLY})
    })
    public BaseResponse<ModelExecutionStatusRes> callbackModelExecution(
        @Parameter(description = "시나리오 ID", example = "1") @PathVariable Long scenarioId,
        @Parameter(description = "실행 리소스 ID", example = "exec_a1b2c3d4e5f6") @PathVariable String executionId,
        @Valid @RequestBody ModelExecutionCallbackReq request
    ) {
        return BaseResponseFactory.create(
            scenarioModelExecutionService.updateModelExecutionFromCallback(scenarioId, executionId, request)
        );
    }

    @ApiOrder(7)
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
