package com.sk.domain.mi.controller;

import com.sk.common.annotation.ApiOrder;
import com.sk.common.annotation.RequiredPermission;
import com.sk.common.enums.ResultCode.Success;
import com.sk.common.model.BaseResponse;
import com.sk.common.model.BaseResponseFactory;
import com.sk.common.utils.RoleUtil;
import com.sk.domain.mi.dto.response.insights.GoodsDataFilterRes;
import com.sk.domain.mi.dto.response.marketData.RecentViewedRes;
import com.sk.domain.mi.service.MiMarketDataService;
import com.sk.domain.shared.enums.PermissionCondition;
import com.sk.domain.shared.enums.Role;
import com.sk.domain.shared.service.permissions.AuthorizationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Profile("!dedicated")
@Tag(name = "03. 시장 데이터 API", description = "시장 데이터 API 입니다.")
@RestController
@RequestMapping("/api/v1/mi/market-data")
@RequiredArgsConstructor
public class MiMarketDataController {

    private final AuthorizationService authorizationService;
    private final MiMarketDataService miMarketDataService;

    @ApiOrder(2)
    @Operation(summary = "Market Data 검색에서 사용할 고급필터", description = "Market Data 검색에서 사용할 providers 고급필터 데이타를 조회합니다.")
    @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
    @GetMapping("/companies/{companyNo}/advanced-filter")
    @RequiredPermission(
        roles = {Role.ADMIN, Role.USER, Role.API_PLUS_OPERATOR, Role.API_SUB_ADMIN},
        condition = PermissionCondition.THIRD_PARTY_ADMIN_ONLY)
    public BaseResponse<GoodsDataFilterRes> getAdvancedFilter(
        @PathVariable("companyNo") Integer companyNo) {

        Role role = authorizationService.getCurrentRole();
        int cNo = RoleUtil.isAdmin(role) ? companyNo : authorizationService.getCompanyNo();

        return BaseResponseFactory.create(miMarketDataService.getAdvancedFilter(cNo));
    }

    @ApiOrder(3)
    @Operation(summary = "Recent Viewed 목록 조회", description = "Recent Viewed 목록을 조회합니다.")
    @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
    @GetMapping("/companies/{companyNo}/recent-viewed")
    @RequiredPermission(
        roles = {Role.ADMIN, Role.USER, Role.API_PLUS_OPERATOR, Role.API_SUB_ADMIN},
        condition = PermissionCondition.THIRD_PARTY_ADMIN_ONLY)
    public BaseResponse<List<RecentViewedRes>> getDataCatalog(
        @PathVariable("companyNo") Integer companyNo) {

        int userNo = authorizationService.getUserNo();
        Role role = authorizationService.getCurrentRole();
        int cNo = RoleUtil.isAdmin(role) ? companyNo : authorizationService.getCompanyNo();

        return BaseResponseFactory.create(miMarketDataService.getRecentViewed(cNo, userNo));
    }

    @ApiOrder(4)
    @Operation(summary = "Recent Viewed 목록 삭제", description = "Recent Viewed 목록을 삭제합니다.")
    @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
    @DeleteMapping("/companies/{companyNo}/recent-viewed")
    @RequiredPermission(
        roles = {Role.ADMIN, Role.USER, Role.API_PLUS_OPERATOR, Role.API_SUB_ADMIN},
        condition = PermissionCondition.THIRD_PARTY_ADMIN_ONLY)
    public BaseResponse<Void> clearRecentViewed(
        @PathVariable("companyNo") Integer companyNo) {

        int userNo = authorizationService.getUserNo();
        Role role = authorizationService.getCurrentRole();
        int cNo = RoleUtil.isAdmin(role) ? companyNo : authorizationService.getCompanyNo();

        miMarketDataService.clearRecentViewed(cNo, userNo);
        return BaseResponseFactory.create(Success.OK);
    }
}
