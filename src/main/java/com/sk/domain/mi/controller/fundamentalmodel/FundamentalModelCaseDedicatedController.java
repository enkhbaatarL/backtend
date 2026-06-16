package com.sk.domain.mi.controller.fundamentalmodel;

import com.sk.common.annotation.ApiOrder;
import com.sk.common.annotation.PermissionRule;
import com.sk.common.annotation.RequiredPermissionRules;
import com.sk.common.model.BaseResponse;
import com.sk.common.model.BaseResponseFactory;
import com.sk.domain.mi.dto.response.fundamentalmodel.FundamentalModelRunAcceptRes;
import com.sk.domain.mi.service.fundamentalmodel.FundamentalModelCaseService;
import com.sk.domain.shared.enums.PermissionCondition;
import com.sk.domain.shared.enums.Role;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "6. MI Fundamental Model Case API", description = "MI Fundamental Model Scenario Case mock API입니다.")
@Slf4j
@RestController
@RequestMapping("/api/v1/mi/fundamental-model/cases")
@RequiredArgsConstructor
public class FundamentalModelCaseDedicatedController {

    private final FundamentalModelCaseService fundamentalModelCaseService;

    @ApiOrder(6)
    @Operation(
        summary = "MI Fundamental Model Scenario Case 실행 요청 API",
        description = "PENDING/COMPLETED/FAILED 상태의 case를 RUNNING 상태로 전이하고 runId를 발급합니다."
    )
    @ApiResponse(responseCode = "202", description = "비동기 실행 접수 성공")
    @PostMapping("/{caseId}/runs")
    @RequiredPermissionRules(rules = {
        @PermissionRule(role = Role.USER, conditions = {PermissionCondition.SKGAS_COMPANY_USER_ONLY}),
        @PermissionRule(role = Role.ADMIN, conditions = {PermissionCondition.THIRD_PARTY_ADMIN_ONLY})
    })
    public ResponseEntity<BaseResponse<FundamentalModelRunAcceptRes>> runCase(
        @Parameter(description = "Scenario Case ID", example = "101") @PathVariable Long caseId
    ) {
        FundamentalModelRunAcceptRes response = fundamentalModelCaseService.runCase(caseId);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(BaseResponseFactory.create(response));
    }

}
