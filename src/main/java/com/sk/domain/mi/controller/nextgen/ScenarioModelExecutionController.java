package com.sk.domain.mi.controller.nextgen;

import com.sk.common.annotation.ApiOrder;
import com.sk.common.annotation.PermissionRule;
import com.sk.common.annotation.RequiredPermissionRules;
import com.sk.common.model.BaseResponse;
import com.sk.common.model.BaseResponseFactory;
import com.sk.domain.mi.dto.response.nextgen.LatestModelParamProposalParametersRes;
import com.sk.domain.mi.dto.response.nextgen.ModelExecutionStatusRes;
import com.sk.domain.mi.service.nextgen.ScenarioModelExecutionService;
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
        summary = "최근 실행 확정 파라미터 조회 API",
        description = "최신 A6 실행 Case가 연결한 Agent proposal의 조회 시점 현재 confirmation_parameters를 조회합니다."
    )
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @GetMapping("/model-executions/latest/model-param-proposal/confirmed-parameters")
    @RequiredPermissionRules(rules = {
        @PermissionRule(role = Role.USER, conditions = {PermissionCondition.SKGAS_COMPANY_USER_ONLY}),
        @PermissionRule(role = Role.ADMIN, conditions = {PermissionCondition.THIRD_PARTY_ADMIN_ONLY})
    })
    public BaseResponse<LatestModelParamProposalParametersRes> getLatestConfirmedParameters(
        @Parameter(description = "시나리오 ID", example = "309") @PathVariable Long scenarioId
    ) {
        return BaseResponseFactory.create(
            scenarioModelExecutionService.getLatestConfirmedParameters(scenarioId)
        );
    }

    @ApiOrder(7)
    @Operation(
        summary = "최근 실행 원 제안 파라미터 조회 API",
        description = "최신 A6 실행 Case가 연결한 Agent proposal의 proposal_parameters를 조회합니다."
    )
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @GetMapping("/model-executions/latest/model-param-proposal/proposed-parameters")
    @RequiredPermissionRules(rules = {
        @PermissionRule(role = Role.USER, conditions = {PermissionCondition.SKGAS_COMPANY_USER_ONLY}),
        @PermissionRule(role = Role.ADMIN, conditions = {PermissionCondition.THIRD_PARTY_ADMIN_ONLY})
    })
    public BaseResponse<LatestModelParamProposalParametersRes> getLatestProposedParameters(
        @Parameter(description = "시나리오 ID", example = "309") @PathVariable Long scenarioId
    ) {
        return BaseResponseFactory.create(
            scenarioModelExecutionService.getLatestProposedParameters(scenarioId)
        );
    }

}
