package com.sk.domain.mi.controller.fundamentalmodel;

import com.sk.common.annotation.ApiOrder;
import com.sk.common.model.BaseResponse;
import com.sk.common.model.BaseResponseFactory;
import com.sk.domain.mi.dto.request.fundamentalmodel.FundamentalModelRunCallbackReq;
import com.sk.domain.mi.dto.response.fundamentalmodel.FundamentalModelRunCallbackRes;
import com.sk.domain.mi.service.fundamentalmodel.FundamentalModelCaseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
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
@Validated
public class FundamentalModelRunDedicatedController {

    private final FundamentalModelCaseService fundamentalModelCaseService;

    @ApiOrder(1)
    @Operation(
        summary = "MI Fundamental Model Scenario Case 실행 완료 Callback API",
        description = "모델이 실행 완료 또는 실패 시 호출하여 run/case 상태를 terminal 상태로 갱신합니다."
    )
    @ApiResponse(responseCode = "200", description = "콜백 처리 성공")
    @PostMapping("/{runId}/callback")
    public BaseResponse<FundamentalModelRunCallbackRes> callback(
        @Parameter(description = "yyyymmdd_hhmmss_XXXX 형식 실행 추적 ID", example = "20260521_143500_A1B2")
        @PathVariable
        @Size(min = 20, max = 20, message = "runId는 20자여야 합니다")
        @Pattern(regexp = "^\\d{8}_\\d{6}_[0-9A-F]{4}$", message = "runId는 yyyymmdd_hhmmss_XXXX 형식이어야 합니다")
        String runId,
        @Valid @RequestBody FundamentalModelRunCallbackReq request
    ) {
        return BaseResponseFactory.create(fundamentalModelCaseService.handleCallback(runId, request));
    }
}
