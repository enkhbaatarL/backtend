package com.sk.domain.mi.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.sk.common.annotation.ApiOrder;
import com.sk.common.annotation.RequiredPermission;
import com.sk.common.model.BaseResponse;
import com.sk.common.model.BaseResponseFactory;
import com.sk.common.utils.RoleUtil;
import com.sk.domain.mi.dto.response.dashboard.LatestDataRes;
import com.sk.domain.mi.dto.response.dashboard.LatestInsightsRes;
import com.sk.domain.mi.service.MiDashboardService;
import com.sk.domain.shared.enums.Role;
import com.sk.domain.shared.service.permissions.AuthorizationService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Tag(name = "01. Dashboard API", description = "Dashboard API 입니다.")
@Slf4j
@RestController
@RequestMapping("/api/v1/mi/dashboard")
@RequiredArgsConstructor
public class MiDashboardController {
    private final AuthorizationService authorizationService;
    private final MiDashboardService miDashboardService;

    @ApiOrder(1)
    @Parameters({
            @Parameter(name = "companyNo", description = "관리자: 선택 된 고객사 번호, 사용자: 자신이 속한 고객사 번호")
    })
    @Operation(summary = "Latest Data 목록 조회", description = "Latest Data 목록을 조회합니다.")
    @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
    @GetMapping("/companies/{companyNo}/latest-data")
    @RequiredPermission(roles = {Role.ADMIN, Role.USER})
    public BaseResponse<List<LatestDataRes>> getLatestData(
            @PathVariable("companyNo") Integer companyNo) {

        Role role = authorizationService.getCurrentRole();
        int cNo = RoleUtil.isAdmin(role) ? companyNo : authorizationService.getCompanyNo();

        return BaseResponseFactory.create(miDashboardService.getLatestData(cNo));
    }

    @ApiOrder(2)
    @Parameters({
            @Parameter(name = "companyNo", description = "관리자: 선택 된 고객사 번호, 사용자: 자신이 속한 고객사 번호")
    })
    @Operation(summary = "Latest Insights 목록 조회", description = "Latest Insights 목록을 조회합니다.")
    @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
    @GetMapping("/companies/{companyNo}/latest-insights")
    @RequiredPermission(roles = {Role.ADMIN, Role.USER})
    public BaseResponse<List<LatestInsightsRes>> getLatestInsights(
            @PathVariable("companyNo") Integer companyNo) {

        Role role = authorizationService.getCurrentRole();
        int cNo = RoleUtil.isAdmin(role) ? companyNo : authorizationService.getCompanyNo();

        return BaseResponseFactory.create(miDashboardService.getLatestInsights(cNo));
    }

    @ApiOrder(3)
    @Parameters({
            @Parameter(name = "companyNo", description = "관리자: 선택 된 고객사 번호, 사용자: 자신이 속한 고객사 번호")
    })
    @Operation(summary = "대시보드 차트 데이터 조회", description = "대시보드 차트 데이터를 조회합니다.")
    @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
    @GetMapping("/companies/{companyNo}/chart")
    @RequiredPermission(roles = {Role.ADMIN, Role.USER})
    public BaseResponse<Boolean> getChart(
            @PathVariable("companyNo") Integer companyNo) {
        return BaseResponseFactory.create(miDashboardService.getChart(companyNo));
    }
}
