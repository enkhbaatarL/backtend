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
import com.sk.domain.mi.dto.response.signalMap.ScreenCategoryDto;
import com.sk.domain.mi.dto.response.signalMap.SignalMapDataRes;
import com.sk.domain.mi.dto.response.signalMap.SignalMapPeriodRes;
import com.sk.domain.mi.service.MiSignalMapService;
import com.sk.domain.shared.enums.Role;
import com.sk.domain.shared.service.permissions.AuthorizationService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Tag(name = "02. Signal Map API", description = "Signal Map API 입니다.")
@Slf4j
@RestController
@RequestMapping("/api/v1/mi/signal-map")
@RequiredArgsConstructor
public class MiSignalMapController {
    private final AuthorizationService authorizationService;
    private final MiSignalMapService miSignalMapService;

    @ApiOrder(1)
    @Operation(summary = "시그널맵 기간 필터 조회", description = "시그널맵 기간 필터를 조회합니다.")
    @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
    @GetMapping("/companies/{companyNo}/periods")
    @RequiredPermission(roles = {Role.ADMIN, Role.USER})
    public BaseResponse<List<SignalMapPeriodRes>> getSignalMapPeriodType(@PathVariable(value = "companyNo") Integer companyNo) {
        return BaseResponseFactory.create(miSignalMapService.getSignalMapPeriodType(companyNo));
    }

    @ApiOrder(2)
    @Operation(summary = "시그널맵 회사별 필터 조회", description = "시그널맵 회사별 필터를 조회합니다.")
    @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
    @GetMapping("/companies/{companyNo}/filters")
    @RequiredPermission(roles = {Role.ADMIN, Role.USER})
    public BaseResponse<List<String>> getSignalMapFilterType(@PathVariable(value = "companyNo") Integer companyNo) {
        return BaseResponseFactory.create(miSignalMapService.getSignalMapFilterType(companyNo));
    }

    @ApiOrder(3)
    @Parameters({
            @Parameter(name = "period", description = "기간 필터"),
            @Parameter(name = "filter", description = "회사마다 다른 필터가 존재, 없는 경우도 있음"),
    })
    @Operation(summary = "시그널맵 데이타 조회", description = "시그널맵 데이타를 조회합니다.")
    @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
    @GetMapping("/companies/{companyNo}")
    @RequiredPermission(roles = {Role.ADMIN, Role.USER})
    public BaseResponse<SignalMapDataRes> getTest(
            @PathVariable(value = "companyNo") Integer companyNo,
            @RequestParam(value = "period", required = false, defaultValue = "TOT") String period,
            @RequestParam(value = "filter", required = false, defaultValue = "") String filter) {

        Role role = authorizationService.getCurrentRole();
        int cNo = RoleUtil.isAdmin(role) ? companyNo : authorizationService.getCompanyNo();

        String title = miSignalMapService.getTitle(cNo);

        String latestUpdatedDt = miSignalMapService.getLatestUpdatedDt(cNo);

        List<ScreenCategoryDto> datas = miSignalMapService.getSignalMap(cNo, period, filter);

        return BaseResponseFactory.create(new SignalMapDataRes(title, latestUpdatedDt, datas));
    }
}
