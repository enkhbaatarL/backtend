package com.sk.domain.mi.controller.nextgen;

import com.sk.common.annotation.ApiOrder;
import com.sk.common.annotation.PermissionRule;
import com.sk.common.annotation.RequiredPermissionRules;
import com.sk.common.model.BaseResponse;
import com.sk.common.model.BaseResponseFactory;
import com.sk.domain.mi.dto.request.nextgen.GroupModelExecutionReq;
import com.sk.domain.mi.dto.response.nextgen.GroupModelExecutionRes;
import com.sk.domain.mi.service.nextgen.ModelParamProposalGroupService;
import com.sk.domain.shared.enums.PermissionCondition;
import com.sk.domain.shared.enums.Role;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 그룹 수급모델 일괄 실행 컨트롤러.
 */
@Tag(name = "05-1. Next Gen MI Model Execution API", description = "Next Gen MI MI 수급모델 실행 API입니다.")
@Slf4j
@RestController
@RequestMapping("/api/v1/mi/next-gen/scenario-groups/{scenarioGroupId}")
@RequiredArgsConstructor
public class ScenarioGroupModelExecutionDedicatedController {

    private final ModelParamProposalGroupService modelParamProposalGroupService;

    @ApiOrder(6)
    @Operation(
        summary = "그룹 수급모델 일괄 실행 접수 API",
        description = """
            선택된 시나리오들을 각자의 확정 파라미터로 순차 실행 접수합니다.
            """
    )
    @ApiResponse(responseCode = "200", description = "일괄 접수 처리 완료. 시나리오별 결과는 results로 판별")
    @PostMapping(value = "/model-executions", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @RequiredPermissionRules(rules = {
        @PermissionRule(role = Role.USER, conditions = {PermissionCondition.SKGAS_COMPANY_USER_ONLY}),
        @PermissionRule(role = Role.ADMIN, conditions = {PermissionCondition.THIRD_PARTY_ADMIN_ONLY})
    })
    public BaseResponse<GroupModelExecutionRes> executeGroupModel(
        @Parameter(description = "시나리오 그룹 ID", example = "61") @PathVariable Long scenarioGroupId,
        @Valid @RequestBody GroupModelExecutionReq request
    ) {
        return BaseResponseFactory.create(modelParamProposalGroupService.executeGroup(scenarioGroupId, request));
    }
}
