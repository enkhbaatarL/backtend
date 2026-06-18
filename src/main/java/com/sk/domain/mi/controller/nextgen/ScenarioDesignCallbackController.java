package com.sk.domain.mi.controller.nextgen;

import com.sk.common.annotation.ApiOrder;
import com.sk.common.annotation.PermissionRule;
import com.sk.common.annotation.RequiredPermissionRules;
import com.sk.common.enums.ResultCode;
import com.sk.common.model.BaseResponse;
import com.sk.common.model.BaseResponseFactory;
import com.sk.domain.mi.dto.request.nextgen.design.ScenarioGenerationCallbackReq;
import com.sk.domain.mi.dto.request.nextgen.design.ScenarioSignpostsCallbackReq;
import com.sk.domain.mi.dto.request.nextgen.design.ScenarioValidationCallbackReq;
import com.sk.domain.mi.service.nextgen.design.ScenarioDesignCallbackService;
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

/**
 * 시나리오 설계 Agent 콜백 수신 전용 컨트롤러
 * 콜백 수신 시 stage 반영 + (검토는 Redis 임시 저장) + 설계 세션 SSE 푸시를 트리거한다.
 */
@Profile("!dedicated")
@Tag(name = "03-2. Next Gen MI Scenario Design Callback API", description = "Next Gen MI 시나리오 설계 Agent 콜백 수신 API입니다. (Agent → B.E)")
@Slf4j
@RestController
@RequestMapping("/api/v1/mi/next-gen/scenario-groups")
@RequiredArgsConstructor
public class ScenarioDesignCallbackController {

    private final ScenarioDesignCallbackService callbackService;

    @ApiOrder(1)
    @Operation(
        summary = "[Agent 콜백] 시나리오 생성 완료 콜백 API",
        description = """
            Agent가 시나리오 그룹/시나리오 생성 및 DB 적재를 완료하면 호출하는 콜백입니다. (Agent → B.E) <br>

            <br><br>
            ### 요청 본문
            - `result`: Agent 작업 성공 여부 (`success` / `failure`)
            - `reason`: 실패 사유
            - `scenarioGroup`: 그룹 메타 (`status`=creating, `stage`=scenario_created)
            - `table`: 분기항목(headers) + 시나리오(rows)
            """
    )
    @ApiResponse(responseCode = "200", description = "콜백 수신 성공")
    @PostMapping("/{scenarioGroupId}/generation/callback")
    @RequiredPermissionRules(rules = {
        @PermissionRule(role = Role.SYSTEM),
        @PermissionRule(role = Role.API_SUB_ADMIN),
        @PermissionRule(role = Role.API_PLUS_OPERATOR),
        @PermissionRule(role = Role.USER, conditions = {PermissionCondition.SKGAS_COMPANY_USER_ONLY}),
        @PermissionRule(role = Role.ADMIN, conditions = {PermissionCondition.THIRD_PARTY_ADMIN_ONLY})
    })
    public BaseResponse<Void> generationCallback(
        @Parameter(description = "시나리오 그룹 ID", example = "1") @PathVariable Long scenarioGroupId,
        @Valid @RequestBody ScenarioGenerationCallbackReq request
    ) {
        callbackService.handleGeneration(scenarioGroupId, request);
        return BaseResponseFactory.create(ResultCode.Success.OK);
    }

    @ApiOrder(2)
    @Operation(
        summary = "[Agent 콜백] 시나리오 검토 완료 콜백 API",
        description = """
            Agent가 변경사항 검토(자동생성값 채움 + 정합성 검증)를 완료하면 호출하는 콜백입니다. (Agent → B.E) <br>
            **DB 저장 없이** 전체 검토 정보를 전달하므로, B.E는 이를 Redis 키(휘발, TTL)에 임시 저장하고 SSE로 푸시합니다. (FE는 상태 조회로 재조회) <br>

            <br><br>
            ### 요청 본문
            - `result`: Agent 검토 작업 성공 여부 (`success` / `failure`)
            - `isValid`: 정합성 통과 여부 (`true` → stage `scenario_validated` / `false` → `scenario_created` 유지)
            - `reason`: 실패 사유
            - `scenarioGroup`: 그룹 메타 (`scenarioGroupId`, `status`, `stage`)
            - `table`: 분기항목(headers) + 시나리오(rows)
            """
    )
    @ApiResponse(responseCode = "200", description = "콜백 수신 성공")
    @PostMapping("/{scenarioGroupId}/validation/callback")
    @RequiredPermissionRules(rules = {
        @PermissionRule(role = Role.SYSTEM),
        @PermissionRule(role = Role.API_SUB_ADMIN),
        @PermissionRule(role = Role.API_PLUS_OPERATOR),
        @PermissionRule(role = Role.USER, conditions = {PermissionCondition.SKGAS_COMPANY_USER_ONLY}),
        @PermissionRule(role = Role.ADMIN, conditions = {PermissionCondition.THIRD_PARTY_ADMIN_ONLY})
    })
    public BaseResponse<Void> validationCallback(
        @Parameter(description = "시나리오 그룹 ID", example = "1") @PathVariable Long scenarioGroupId,
        @Valid @RequestBody ScenarioValidationCallbackReq request
    ) {
        callbackService.handleValidation(scenarioGroupId, request);
        return BaseResponseFactory.create(ResultCode.Success.OK);
    }

    @ApiOrder(3)
    @Operation(
        summary = "[Agent 콜백] Signpost 생성 완료 콜백 API",
        description = """
            Agent가 Signpost 생성 및 DB 적재를 완료하면 호출하는 콜백입니다. (Agent → B.E) <br>

            <br><br>
            ### 요청 본문
            - `result`: Agent 작업 성공 여부 (`success` / `failure`)
            - `reason`: 실패 사유
            - `scenarioGroup`: 그룹 메타 (`stage`=signpost_created)
            - `table`: 분기항목(headers) + 시나리오(rows) + 시나리오별 `signposts[]` (`metrics`, `narrative` 포함)
            """
    )
    @ApiResponse(responseCode = "200", description = "콜백 수신 성공")
    @PostMapping("/{scenarioGroupId}/signposts/callback")
    @RequiredPermissionRules(rules = {
        @PermissionRule(role = Role.SYSTEM),
        @PermissionRule(role = Role.API_SUB_ADMIN),
        @PermissionRule(role = Role.API_PLUS_OPERATOR),
        @PermissionRule(role = Role.USER, conditions = {PermissionCondition.SKGAS_COMPANY_USER_ONLY}),
        @PermissionRule(role = Role.ADMIN, conditions = {PermissionCondition.THIRD_PARTY_ADMIN_ONLY})
    })
    public BaseResponse<Void> signpostsCallback(
        @Parameter(description = "시나리오 그룹 ID", example = "1") @PathVariable Long scenarioGroupId,
        @Valid @RequestBody ScenarioSignpostsCallbackReq request
    ) {
        callbackService.handleSignposts(scenarioGroupId, request);
        return BaseResponseFactory.create(ResultCode.Success.OK);
    }

}
