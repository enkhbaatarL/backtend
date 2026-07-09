package com.sk.domain.mi.controller.nextgen;

import com.sk.common.annotation.ApiOrder;
import com.sk.common.annotation.PermissionRule;
import com.sk.common.annotation.RequiredPermissionRules;
import com.sk.common.enums.ResultCode;
import com.sk.common.model.BaseResponse;
import com.sk.common.model.BaseResponseFactory;
import com.sk.domain.mi.dto.request.nextgen.ModelParamProposalCallbackReq;
import com.sk.domain.mi.service.nextgen.ModelParamProposalCallbackService;
import com.sk.domain.shared.enums.PermissionCondition;
import com.sk.domain.shared.enums.Role;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Profile("!dedicated")
@Tag(name = "05-3. Next Gen MI Model Parameter Proposal Callback API", description = "Next Gen MI 수급모델 파라미터 제안 Agent 콜백 API입니다. (Agent → B.E)")
@Slf4j
@RestController
@RequestMapping("/api/v1/mi/next-gen/scenarios/{scenarioId}/model-param-proposals")
@RequiredArgsConstructor
public class ModelParamProposalCallbackController {

    private final ModelParamProposalCallbackService callbackService;

    @ApiOrder(5)
    @Operation(
        summary = "[Agent 콜백] MI 수급모델 파라미터 제안 완료 API",
        description = "Agent proposal 완료/실패 신호를 수신한 뒤 Agent proposal table을 재조회해 Backend orchestration 상태를 갱신합니다."
    )
    @ApiResponse(responseCode = "200", description = "콜백 수신 성공")
    @PostMapping("/callback")
    @RequiredPermissionRules(rules = {
        @PermissionRule(role = Role.SYSTEM),
        @PermissionRule(role = Role.API_SUB_ADMIN),
        @PermissionRule(role = Role.API_PLUS_OPERATOR),
        @PermissionRule(role = Role.USER, conditions = {PermissionCondition.SKGAS_COMPANY_USER_ONLY}),
        @PermissionRule(role = Role.ADMIN, conditions = {PermissionCondition.THIRD_PARTY_ADMIN_ONLY})
    })
    public BaseResponse<Void> callback(
        @Parameter(description = "시나리오 ID", example = "309") @PathVariable Long scenarioId,
        @Valid @RequestBody ModelParamProposalCallbackReq request
    ) {
        callbackService.handleCallback(scenarioId, request);
        return BaseResponseFactory.create(ResultCode.Success.OK);
    }
}
