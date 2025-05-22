package com.sk.domain.mi.controller;

import com.sk.common.annotation.ApiOrder;
import com.sk.common.annotation.RequiredPermission;
import com.sk.common.model.BaseResponse;
import com.sk.common.model.BaseResponseFactory;
import com.sk.domain.mi.service.MiDashboardHistoryService;
import com.sk.domain.shared.enums.Role;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "11. 대시보드 히스토리 API", description = "대시보드 히스토리 API 입니다.")
@Slf4j
@RestController
@RequestMapping("/api/v1/mi/dashboard")
@RequiredArgsConstructor
public class MiDashboardHistoryController {

    private final MiDashboardHistoryService dashboardHistoryService;

    /**
     * 대시보드 History Section
     * 1. 드랍다운 리스트 조회 API + meta?
     * 2. 데이터 리스트 조회 API (예측변화율, 실가변화율, 가장 높은 예측값, 가장 낮은 예측값, 가장 높은 실제값, 가장 낮은 실제값)
     * 3. 월별 데이터인 경우 리포트 다운로드 API
     */
    @ApiOrder(10)
    @Parameters({
        @Parameter(name = "companyNo", description = "고객회사 번호")
    })
    @Operation(summary = "대시보드 히스토리 드랍다운 리스트 API", description = "대시보드 히스토리 드랍다운 리스트를 조회합니다.")
    @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
    @GetMapping("/history/dropdown")
    @RequiredPermission(roles = {Role.ADMIN, Role.USER})
    public BaseResponse<?> getForecastDate(
        @RequestParam(value = "companyNo") Integer companyNo
    ) {
        return BaseResponseFactory.create(dashboardHistoryService.getForecastDate(companyNo));
    }

}
