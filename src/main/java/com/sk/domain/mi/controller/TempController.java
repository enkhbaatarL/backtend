package com.sk.domain.mi.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.sk.common.annotation.ApiOrder;
import com.sk.common.annotation.RequiredPermission;
import com.sk.common.model.BaseResponse;
import com.sk.common.model.BaseResponseFactory;
import com.sk.common.model.PaginationRes;
import com.sk.common.utils.RoleUtil;
import com.sk.domain.admin.dto.request.provider.GoodsDataUpdateReq;
import com.sk.domain.mi.dto.response.insights.FocusedDataRes;
import com.sk.domain.mi.dto.response.insights.GoodsDataFilterRes;
import com.sk.domain.mi.dto.response.insights.InsightsDataRes;
import com.sk.domain.mi.dto.response.marketData.DataCatalogRes;
import com.sk.domain.mi.dto.response.marketData.RecentViewedRes;
import com.sk.domain.mi.service.MiGoodsDataInqueryUserService;
import com.sk.domain.mi.service.MiInsightsService;
import com.sk.domain.mi.service.TempService;
import com.sk.domain.shared.enums.Role;
import com.sk.domain.shared.enums.insights.InsightsPeriodType;
import com.sk.domain.shared.enums.insights.InsightsSortType;
import com.sk.domain.shared.service.permissions.AuthorizationService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Tag(name = "99. Temp API", description = "Temp API 입니다.")
@Slf4j
@RestController
@RequestMapping("/api/v1/mi/temp")
@RequiredArgsConstructor
public class TempController {
    private final AuthorizationService authorizationService;
    private final TempService tempService;

    @ApiOrder(3)
    @Parameters({
            @Parameter(name = "customerCompanyNo", description = "관리자: 선택 된 고객사 번호, 사용자: 자신이 속한 고객사 번호"),
            @Parameter(name = "page", description = "페이지 번호 (기본값: 1)"),
            @Parameter(name = "size", description = "페이지당 항목 수 (기본값: 10)"),
            @Parameter(name = "keyword", description = "데이타명 검색")
    })
    @Operation(summary = "Data Catalog 목록 조회 - with pagination", description = "Data Catalog 목록을 조회합니다.")
    @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
    @GetMapping("/data-catalog")
    @RequiredPermission(roles = {Role.ADMIN, Role.USER})
    public BaseResponse<PaginationRes<DataCatalogRes>> getDataCatalog(
            @RequestParam(value = "customerCompanyNo", required = true) @Min(value = 1) Integer customerCompanyNo,
            @RequestParam(value = "page", required = false, defaultValue = "1") @Min(value = 1) int page,
            @RequestParam(value = "size", required = false, defaultValue = "10") @Min(value = 1) int size,
            @RequestParam(value = "keyword", required = false, defaultValue = "") String keyword) {

        Role role = authorizationService.getCurrentRole();
        int cNo = RoleUtil.isAdmin(role) ? customerCompanyNo : authorizationService.getCompanyNo();

        return BaseResponseFactory
                .create(tempService.getDataCatalog(cNo, page, size, keyword));
    }

    @ApiOrder(4)
    @Operation(summary = "Recent Viewed 목록 조회", description = "Recent Viewed 목록을 조회합니다.")
    @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
    @GetMapping("/recent-viewed")
    @RequiredPermission(roles = {Role.ADMIN, Role.USER})
    public BaseResponse<List<RecentViewedRes>> getDataCatalog() {

        int userNo = authorizationService.getUserNo();

        return BaseResponseFactory
                .create(tempService.getRecentViewed(userNo));
    }
}
