package com.sk.domain.mi.controller.nextgen;

import com.sk.common.annotation.ApiOrder;
import com.sk.common.annotation.PermissionRule;
import com.sk.common.annotation.RequiredPermissionRules;
import com.sk.common.enums.ResultCode;
import com.sk.common.model.BaseResponse;
import com.sk.common.model.BaseResponseFactory;
import com.sk.domain.mi.dto.request.nextgen.priceinterp.PriceInterpretationGenerationCallbackReq;
import com.sk.domain.mi.service.nextgen.priceinterp.PriceInterpretationCallbackService;
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
 * 가격전망 해석 Agent 콜백 수신 전용 컨트롤러 (Agent → B.E).
 * 콜백 수신 시 해석 원본을 DB에 적재하고 SSE 푸시를 트리거한다.
 */
@Profile("!dedicated")
@Tag(name = "04-2. Next Gen MI Price Interpretation Callback API", description = "Next Gen MI 가격전망 해석 Agent 콜백 수신 API입니다. (Agent → B.E)")
@Slf4j
@RestController
@RequestMapping("/api/v1/mi/next-gen/scenario-groups")
@RequiredArgsConstructor
public class PriceInterpretationCallbackController {

    private final PriceInterpretationCallbackService callbackService;

    @ApiOrder(1)
    @Operation(
        summary = "[Agent 콜백] 가격전망 해석 생성 완료 콜백 API",
        description = """
            Agent가 시나리오별 가격전망 해석 산출을 완료하면 호출하는 콜백입니다. (Agent → B.E) <br>
            성공 시 시나리오별 원본 해석을 DB에 적재하고, 실패 시 대상 시나리오를 failed로 업데이트합니다. <br>
            이후 F/E에게 SSE 신호를 푸시하며 F/E는 조회 API를 재조회하여 화면을 갱신합니다. <br>

            <br><br>
            ### 요청 본문
            - `result`: Agent 작업 성공 여부 (`success` / `failure`)
            - `reason`: 실패 사유
            - `interpretations[]`: 시나리오별 해석 (`scenarioId`, `content`)
            """
    )
    @ApiResponse(responseCode = "200", description = "콜백 수신 성공")
    @PostMapping("/{scenarioGroupId}/price-interpretations/callback")
    @RequiredPermissionRules(rules = {
        @PermissionRule(role = Role.SYSTEM),
        @PermissionRule(role = Role.API_SUB_ADMIN),
        @PermissionRule(role = Role.API_PLUS_OPERATOR),
        @PermissionRule(role = Role.USER, conditions = {PermissionCondition.SKGAS_COMPANY_USER_ONLY}),
        @PermissionRule(role = Role.ADMIN, conditions = {PermissionCondition.THIRD_PARTY_ADMIN_ONLY})
    })
    public BaseResponse<Void> generationCallback(
        @Parameter(description = "시나리오 그룹 ID", example = "1") @PathVariable Long scenarioGroupId,
        @Valid @RequestBody PriceInterpretationGenerationCallbackReq request
    ) {
        callbackService.handleGeneration(scenarioGroupId, request);
        return BaseResponseFactory.create(ResultCode.Success.OK);
    }

}
