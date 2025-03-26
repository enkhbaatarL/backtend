package com.sk.domain.admin.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.sk.common.annotation.ApiOrder;
import com.sk.common.model.BaseResponse;
import com.sk.common.model.BaseResponseFactory;
import com.sk.domain.admin.dto.response.privatePolicy.PrivatePolicyRes;
import com.sk.domain.admin.dto.response.privatePolicy.PrivatePolicyVersionRes;
import com.sk.domain.admin.service.AdminPrivatePolicyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Tag(name = "08. 정책약관 관리 API", description = "정책약관 관리 API 입니다.")
@Slf4j
@RestController
@RequestMapping("/api/v1/admin/privatepolicy-manangement")
@RequiredArgsConstructor
public class AdminPrivatePolicyController {
    private final AdminPrivatePolicyService adminPrivatePolicyService;

    @ApiOrder(1)
    @Operation(summary = "정책약관 목록 조회", description = "정책약관 목록 조회합니다.")
    @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
    @GetMapping("/")
    public BaseResponse<List<PrivatePolicyVersionRes>> getPrivatePolicies() {

        return BaseResponseFactory.create(adminPrivatePolicyService.getPrivatePolicies());
    }

    @ApiOrder(2)
    @Parameters({
            @Parameter(name = "poltacNo", description = "정책약관번호")
    })
    @Operation(summary = "정책약관 조회", description = "정책약관을 조회합니다.")
    @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
    @GetMapping("/PrivatePolicies/{poltacNo}")
    public BaseResponse<PrivatePolicyRes> getPrivatePolicy(
            @PathVariable(value = "poltacNo") @Min(value = 1) Integer poltacNo) {

        return BaseResponseFactory.create(adminPrivatePolicyService.getPrivatePolicy(poltacNo));
    }

    @ApiOrder(3)
    @Operation(summary = "최신 정책약관 조회", description = "최신 정책약관을 조회합니다.")
    @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
    @GetMapping("/PrivatePolicies/latest")
    public BaseResponse<PrivatePolicyRes> getLatestPrivatePolicy() {

        return BaseResponseFactory.create(adminPrivatePolicyService.getLatestPrivatePolicy());
    }
}
