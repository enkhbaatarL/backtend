package com.sk.domain.mi.controller;

import com.sk.common.annotation.ApiOrder;
import com.sk.common.model.BaseResponse;
import com.sk.common.model.BaseResponseFactory;
import com.sk.domain.mi.dto.response.tactics.PolicyTacticsRes;
import com.sk.domain.mi.dto.response.tactics.PolicyTacticsSimpleRes;
import com.sk.domain.mi.service.MiPolicyTacticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Profile("!dedicated")
@Tag(name = "08. 개인정보 처리방침 API", description = "개인정보 처리방침 API 입니다.")
@RestController
@RequestMapping("/api/v1/mi/policy-tactics")
@RequiredArgsConstructor
public class MiPolicyTacticsController {

    private final MiPolicyTacticsService policyTacticsService;

    @ApiOrder(1)
    @Operation(summary = "개인정보 처리방침 목록 조회 API", description = "개인정보 처리방침 목록을 조회합니다. <br> 최신 생성일 기준 내림차순 정렬되어 응답됩니다.")
    @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
    @GetMapping("/privacy-policies")
    public BaseResponse<List<PolicyTacticsSimpleRes>> getPrivacyPolicies() {
        return BaseResponseFactory.create(policyTacticsService.getPrivacyPolicies());
    }

    @ApiOrder(2)
    @Parameters({
        @Parameter(name = "poltacNo", description = "정책약관 번호")
    })
    @Operation(summary = "개인정보 처리방침 상세 조회 API", description = "개인정보 처리방침을 상세 조회합니다.")
    @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
    @GetMapping("/privacy-policies/{poltacNo}")
    public BaseResponse<PolicyTacticsRes> getPrivacyPoliciesDetail(
        @PathVariable(value = "poltacNo") Integer poltacNo
    ) {
        return BaseResponseFactory.create(policyTacticsService.getPrivacyPoliciesDetail(poltacNo));
    }

    @ApiOrder(3)
    @Operation(summary = "최신 개인정보 처리방침 상세 조회 API", description = "가장 최신의 개인정보 처리방침 상세내용을 조회합니다.")
    @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
    @GetMapping("/privacy-policies/latest")
    public BaseResponse<PolicyTacticsRes> getLatestPrivacyPoliciesDetail() {
        return BaseResponseFactory.create(policyTacticsService.getLatestPrivacyPoliciesDetail());
    }

}
