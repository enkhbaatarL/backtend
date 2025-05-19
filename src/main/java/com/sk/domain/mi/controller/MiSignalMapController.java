package com.sk.domain.mi.controller;

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
import com.sk.domain.mi.service.MiSignalMapService;
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

@Tag(name = "02. Signal Map API", description = "Signal Map API 입니다.")
@Slf4j
@RestController
@RequestMapping("/api/v1/mi/signal-map")
@RequiredArgsConstructor
public class MiSignalMapController {
    private final AuthorizationService authorizationService;
    private final MiSignalMapService miSignalMapService;

    @ApiOrder(1)
    // @Parameters({
    // @Parameter(name = "customerCompanyNo", description = "관리자: 선택 된 고객사 번호, 사용자: 자신이 속한 고객사 번호")
    // })
    @Operation(summary = "??? 목록 조회", description = "??? 목록을 조회합니다.")
    @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
    @GetMapping("/latest-data")
    @RequiredPermission(roles = {Role.ADMIN, Role.USER})
    public BaseResponse<String> getTest(@PathVariable(value = "companyNo") Integer companyNo) {

        Role role = authorizationService.getCurrentRole();
        int cNo = RoleUtil.isAdmin(role) ? companyNo : authorizationService.getCompanyNo();

        return BaseResponseFactory.create("todo");
    }
}
