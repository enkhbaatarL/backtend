package com.sk.domain.mi.controller;

import java.util.List;

import org.springframework.context.annotation.Profile;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.sk.common.annotation.ApiOrder;
import com.sk.common.annotation.RequiredPermission;
import com.sk.common.model.BaseResponse;
import com.sk.common.model.BaseResponseFactory;
import com.sk.common.model.PaginationRes;
import com.sk.common.utils.RoleUtil;
import com.sk.domain.mi.dto.response.marketData.DataCatalogRes;
import com.sk.domain.mi.dto.response.marketData.RecentViewedRes;
import com.sk.domain.mi.dto.response.marketData.charts.MiMarketDataRes;
import com.sk.domain.mi.service.MiMarketDataService;
import com.sk.domain.shared.enums.PermissionCondition;
import com.sk.domain.shared.enums.Role;
import com.sk.domain.shared.service.permissions.AuthorizationService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;

@Profile("!dedicated")
@Tag(name = "03. 시장 데이터 API", description = "시장 데이터 API 입니다.")
@RestController
@RequestMapping("/api/v1/mi/market-data")
@RequiredArgsConstructor
public class MiMarketDataController {
    private final AuthorizationService authorizationService;
    private final MiMarketDataService miMarketDataService;


    @ApiOrder(3)
    @Operation(summary = "Recent Viewed 목록 조회", description = "Recent Viewed 목록을 조회합니다.")
    @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
    @GetMapping("/companies/{companyNo}/recent-viewed")
    @RequiredPermission(roles = {Role.ADMIN, Role.USER, Role.API_PLUS_OPERATOR,
            Role.API_SUB_ADMIN}, condition = PermissionCondition.THIRD_PARTY_ADMIN_ONLY)
    public BaseResponse<List<RecentViewedRes>> getDataCatalog(
            @PathVariable("companyNo") Integer companyNo) {

        int userNo = authorizationService.getUserNo();
        Role role = authorizationService.getCurrentRole();
        int cNo = RoleUtil.isAdmin(role) ? companyNo : authorizationService.getCompanyNo();

        return BaseResponseFactory
                .create(miMarketDataService.getRecentViewed(cNo, userNo));
    }

    @ApiOrder(4)
    @Operation(summary = "custom 마켓 데이터 조회 API", description = "마켓 데이터를 조회합니다.")
    @ApiResponse(responseCode = "200", description = "마켓 데이터 조회 성공")
    @RequiredPermission(roles = {Role.ADMIN, Role.USER, Role.API_PLUS_OPERATOR,
        Role.API_SUB_ADMIN}, condition = PermissionCondition.THIRD_PARTY_ADMIN_ONLY)
    @GetMapping("/custom/{customId}")
    public BaseResponse<?> getMarketDataCustom(@PathVariable("customId") String customId) {
        return BaseResponseFactory.create(miMarketDataService.getMarketDataCustom(customId));
    }
}
