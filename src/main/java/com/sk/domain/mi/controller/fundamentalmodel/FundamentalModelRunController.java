package com.sk.domain.mi.controller.fundamentalmodel;

import com.sk.common.annotation.ApiOrder;
import com.sk.common.annotation.RequiredPermission;
import com.sk.common.model.BaseResponse;
import com.sk.common.model.BaseResponseFactory;
import com.sk.domain.mi.dto.request.fundamentalmodel.FundamentalModelRunCallbackReq;
import com.sk.domain.mi.dto.response.fundamentalmodel.FundamentalModelRunCallbackRes;
import com.sk.domain.mi.service.fundamentalmodel.FundamentalModelCaseService;
import com.sk.domain.shared.enums.Role;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "6-1. MI Fundamental Model Run API", description = "MI Fundamental Model 실행 Callback API입니다.")
@Slf4j
@RestController
@RequestMapping("/api/v1/mi/fundamental-model/runs")
@RequiredArgsConstructor
public class FundamentalModelRunController {

    private final FundamentalModelCaseService fundamentalModelCaseService;

    @ApiOrder(1)
    @Operation(
        summary = "MI Fundamental Model Scenario Case 실행 완료 Callback API",
        description = "외부 모델 API가 실행 완료 또는 실패 시 호출하여 run/case 상태를 terminal 상태로 갱신합니다."
    )
    @ApiResponse(responseCode = "200", description = "콜백 처리 성공")
    @PostMapping("/{runId}/callback")
    @RequiredPermission(roles = {Role.ADMIN, Role.API_PLUS_OPERATOR, Role.API_SUB_ADMIN})
    public BaseResponse<FundamentalModelRunCallbackRes> callback(
        @Parameter(description = "실행 추적 ID", example = "3f7e44c4-2bb5-4b09-b577-920f83ad2a6d") @PathVariable String runId,
        @Valid @RequestBody FundamentalModelRunCallbackReq request
    ) {
        return BaseResponseFactory.create(fundamentalModelCaseService.handleCallback(runId, request));
    }
}
