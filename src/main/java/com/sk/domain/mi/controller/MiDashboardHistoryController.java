package com.sk.domain.mi.controller;

import com.sk.common.annotation.ApiOrder;
import com.sk.common.annotation.RequiredPermission;
import com.sk.common.model.BaseResponse;
import com.sk.common.model.BaseResponseFactory;
import com.sk.domain.mi.dto.response.dashboard.history.HistoryMetadataRes;
import com.sk.domain.mi.service.MiDashboardHistoryService;
import com.sk.domain.shared.enums.PermissionCondition;
import com.sk.domain.shared.enums.Role;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.*;

@Profile("!dedicated")
@Tag(name = "01-2. Dashboard(History) API", description = "대시보드 히스토리 API 입니다.")
@Slf4j
@RestController
@RequestMapping("/api/v1/mi/dashboard")
@RequiredArgsConstructor
public class MiDashboardHistoryController {

    private final MiDashboardHistoryService dashboardHistoryService;

    @ApiOrder(10)
    @Operation(summary = "대시보드 히스토리 메타데이터 API", description = "대시보드 히스토리 메타데이터를 조회합니다.")
    @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
    @GetMapping("/histories/metadata")
    @RequiredPermission(
        roles = {Role.ADMIN, Role.USER, Role.API_PLUS_OPERATOR},
        condition = PermissionCondition.THIRD_PARTY_ADMIN_ONLY
    )
    public BaseResponse<HistoryMetadataRes> getForecastHistoryMetadata() {
        return BaseResponseFactory.create(dashboardHistoryService.getForecastHistoryMetadata());
    }

}
