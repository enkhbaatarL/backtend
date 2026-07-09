package com.sk.domain.mi.controller.nextgen;

import com.sk.common.annotation.ApiOrder;
import com.sk.common.annotation.PermissionRule;
import com.sk.common.annotation.RequiredPermissionRules;
import com.sk.common.model.BaseResponse;
import com.sk.common.model.BaseResponseFactory;
import com.sk.domain.mi.dto.response.nextgen.ModelExecutionOptionsRes;
import com.sk.domain.mi.dto.response.nextgen.ModelParamProposalAcceptRes;
import com.sk.domain.mi.dto.response.nextgen.ModelParamProposalRes;
import com.sk.domain.mi.service.nextgen.ModelParamProposalService;
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
@Tag(name = "05. Next Gen MI Model Parameter Proposal API", description = "Next Gen MI 수급모델 파라미터 제안 API입니다.")
@Slf4j
@RestController
@RequestMapping("/api/v1/mi/next-gen/scenarios/{scenarioId}")
@RequiredArgsConstructor
public class ModelParamProposalController {

    private final ModelParamProposalService modelParamProposalService;

    @ApiOrder(1)
    @Operation(
        summary = "수급모델 실행 입력 방식 조회 API",
        description = "Scenario 기반 Event에 따라 수급모델 실행 입력 방식을 조회합니다."
    )
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @GetMapping("/model-execution-options")
    @RequiredPermissionRules(rules = {
        @PermissionRule(role = Role.USER, conditions = {PermissionCondition.SKGAS_COMPANY_USER_ONLY}),
        @PermissionRule(role = Role.ADMIN, conditions = {PermissionCondition.THIRD_PARTY_ADMIN_ONLY})
    })
    public BaseResponse<ModelExecutionOptionsRes> getModelExecutionOptions(
        @Parameter(description = "시나리오 ID", example = "309") @PathVariable Long scenarioId
    ) {
        return BaseResponseFactory.create(modelParamProposalService.getModelExecutionOptions(scenarioId));
    }

    @ApiOrder(2)
    @Operation(
        summary = "수급모델 파라미터 제안 생성 접수 API",
        description = "Agent에 파라미터 제안 생성을 요청하고 Backend execution-preparation orchestration row를 연결합니다."
    )
    @ApiResponse(responseCode = "200", description = "접수 성공")
    @PostMapping("/model-param-proposals")
    @RequiredPermissionRules(rules = {
        @PermissionRule(role = Role.USER, conditions = {PermissionCondition.SKGAS_COMPANY_USER_ONLY}),
        @PermissionRule(role = Role.ADMIN, conditions = {PermissionCondition.THIRD_PARTY_ADMIN_ONLY})
    })
    public BaseResponse<ModelParamProposalAcceptRes> createModelParamProposal(
        @Parameter(description = "시나리오 ID", example = "309") @PathVariable Long scenarioId
    ) {
        return BaseResponseFactory.create(modelParamProposalService.createProposal(scenarioId));
    }

    @ApiOrder(3)
    @Operation(
        summary = "수급모델 파라미터 제안 상태/결과 조회 API",
        description = "Backend execution-preparation row와 Agent proposal table을 조합해 상태와 제안 결과를 조회합니다."
    )
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @GetMapping("/model-param-proposals/{modelParamProposalId}")
    @RequiredPermissionRules(rules = {
        @PermissionRule(role = Role.USER, conditions = {PermissionCondition.SKGAS_COMPANY_USER_ONLY}),
        @PermissionRule(role = Role.ADMIN, conditions = {PermissionCondition.THIRD_PARTY_ADMIN_ONLY})
    })
    public BaseResponse<ModelParamProposalRes> getModelParamProposal(
        @Parameter(description = "시나리오 ID", example = "309") @PathVariable Long scenarioId,
        @Parameter(description = "Agent model parameter proposal ID", example = "10001") @PathVariable Long modelParamProposalId
    ) {
        return BaseResponseFactory.create(modelParamProposalService.getProposal(scenarioId, modelParamProposalId));
    }
}
