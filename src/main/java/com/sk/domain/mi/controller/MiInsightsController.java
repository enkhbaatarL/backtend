package com.sk.domain.mi.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.sk.common.annotation.ApiOrder;
import com.sk.common.model.BaseResponse;
import com.sk.common.model.BaseResponseFactory;
import com.sk.common.model.PaginationRes;
import com.sk.common.utils.RoleUtil;
import com.sk.domain.mi.dto.response.insights.GoodsDataFilterRes;
import com.sk.domain.mi.dto.response.insights.GoodsDataRes;
import com.sk.domain.mi.dto.response.insights.IGoodsDataRes;
import com.sk.domain.mi.service.MiInsightsService;
import com.sk.domain.shared.enums.Role;
import com.sk.domain.shared.enums.insights.InsightsSearchType;
import com.sk.domain.shared.enums.insights.InsightsSortType;
import com.sk.domain.shared.service.permissions.AuthorizationService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Tag(name = "03. Insights API", description = "Insights API 입니다.")
@Slf4j
@RestController
@RequestMapping("/api/v1/mi/insights")
@RequiredArgsConstructor
public class MiInsightsController {
    private final AuthorizationService authorizationService;
    private final MiInsightsService miInsightsService;

    // ADMIN-SKCC
    // eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJTS0NDMDAwQFNLQ0MuY29tIiwiaWF0IjoxNzQ0Mjc0ODcxLCJ1c2VyTm8iOjEwMDAwNDQsImNvbXBhbnlObyI6MjAxMiwiY29tcGFueUNkIjoiU0tDQyIsInJvbGUiOiJBRE1JTiIsImV4cCI6MTc4MDI3NDg3MX0.hVm8KPlcTPwjxGtdRr8VH1uIrGFIfIePb_FyVkiquHc

    // OPERATOR-SKHY
    // eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJTS0hZMDAwQFNLSFkuY29tIiwiaWF0IjoxNzQ0Mjc0OTA0LCJ1c2VyTm8iOjEwMDAwNDUsImNvbXBhbnlObyI6MjAxNSwiY29tcGFueUNkIjoiU0tIWSIsInJvbGUiOiJPUEVSQVRPUiIsImV4cCI6MTc4MDI3NDkwNH0.T0JZ5XJva9dokRGhDhipWTvlRi92kSO9cUEzpFfXgSk

    // OPERATOR-SKGAS
    // eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJTS0dBUzAwMEBTS0dBUy5jb20iLCJpYXQiOjE3NDQyNzQ5MjcsInVzZXJObyI6MTAwMDA1MSwiY29tcGFueU5vIjoyMDE0LCJjb21wYW55Q2QiOiJTS0dBUyIsInJvbGUiOiJPUEVSQVRPUiIsImV4cCI6MTc4MDI3NDkyN30.p3BWi9foUIVa2LoG4GF6yBLvyoV-1ZTb7Q2S_JS2U1I

    // USER-SKHY
    // eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJTS0hZMDAxQFNLSFkuY29tIiwiaWF0IjoxNzQ0ODUyODE4LCJ1c2VyTm8iOjEwMDAwNTMsImNvbXBhbnlObyI6MjAxNSwiY29tcGFueUNkIjoiU0tIWSIsInJvbGUiOiJVU0VSIiwiaXNPdGhlclNlcnZpY2VBY2Nlc3NQb3NzaWJsZSI6ZmFsc2UsImV4cCI6MTc4MDg1MjgxOH0.QVEABqTE7DIwcjRP81FvxKCmThW3ImwveJKFecaiXcA

    // USER-SKGAS
    // eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJTS0dBUzAwMUBTS0dBUy5jb20iLCJpYXQiOjE3NDQ4NTMwMTUsInVzZXJObyI6MTAwMDA4NSwiY29tcGFueU5vIjoyMDE0LCJjb21wYW55Q2QiOiJTS0dBUyIsInJvbGUiOiJVU0VSIiwiaXNPdGhlclNlcnZpY2VBY2Nlc3NQb3NzaWJsZSI6ZmFsc2UsImV4cCI6MTc4MDg1MzAxNX0.WcDDwXHwvXtkSITUeLE18AzFgWkwSCEEvGA_tqmx5X8

    @ApiOrder(1)
    @Parameters({
            @Parameter(name = "customerCompanyNo", description = "관리자: 선택 된 고객사 번호, 사용자: 자신이 속한 고객사 번호")
    })
    @Operation(summary = "Focused 목록 조회", description = "Focused 목록을 조회합니다.")
    @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
    @GetMapping("/focused")
    // @RequirePermission(roles = { Role.ADMIN, Role.USER })
    public BaseResponse<List<GoodsDataRes>> getFocused(
            @RequestParam(value = "customerCompanyNo") @Min(value = 1) Integer customerCompanyNo) {

        Role role = authorizationService.getCurrentRole();
        int cNo = RoleUtil.isAdmin(role) ? customerCompanyNo : authorizationService.getCompanyNo();

        return BaseResponseFactory.create(miInsightsService.getFocused(cNo));
    }

    // @ApiOrder(2)
    // @Parameters({
    // @Parameter(name = "customerCompanyNo", description = "관리자: 선택 된 고객사 번호, 사용자: 자신이 속한 고객사 번호"),
    // @Parameter(name = "keyword", description = "데이타명 또는 데이타설명 검색")
    // })
    // @Operation(summary = "Insights 검색에서 사용할 자동완성", description = "Insights 검색에서 사용할 자동완성 데이타를 조회합니다.")
    // @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
    // @GetMapping("/insights/autocomplete")
    // @RequirePermission(roles = { Role.ADMIN, Role.USER })
    // public BaseResponse<List<GoodsDataACRes>> getDataCatalogAutoComplete(
    // @RequestParam(value = "customerCompanyNo", required = true) @Min(value = 1) Integer customerCompanyNo,
    // @RequestParam(value = "keyword", required = false, defaultValue = "") String keyword) {

    // Role role = authorizationService.getCurrentRole();
    // int cNo = RoleUtil.isAdmin(role) ? customerCompanyNo : authorizationService.getCompanyNo();

    // return BaseResponseFactory.create(miInsightsService.getDataCatalogAutoComplete(cNo, keyword));
    // }

    @ApiOrder(3)
    @Operation(summary = "Insights 검색에서 사용할 고급필터", description = "Insights 검색에서 사용할 고급필터 데이타를 조회합니다.")
    @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
    @GetMapping("/insights/advancedfilter")
    // @RequirePermission(roles = { Role.ADMIN, Role.USER })
    public BaseResponse<GoodsDataFilterRes> getAdvancedFilter() {

        return BaseResponseFactory.create(miInsightsService.getAdvancedFilter());
    }

    @ApiOrder(3)
    @Parameters({
            @Parameter(name = "customerCompanyNo", description = "관리자: 선택 된 고객사 번호, 사용자: 자신이 속한 고객사 번호"),
            @Parameter(name = "page", description = "페이지 번호 (기본값: 1)"),
            @Parameter(name = "size", description = "페이지당 항목 수 (기본값: 10)"),
            @Parameter(name = "keyword", description = "데이타명 또는 데이타설명 검색"),
            @Parameter(name = "sort", description = "정렬 기준 (NEWEST, OLDEST)", schema = @Schema(implementation = InsightsSortType.class)),
            @Parameter(name = "period", in = ParameterIn.QUERY, description = "기간 필터 (ALL_DAY, ONE_DAY, ONE_WEEK, ONE_MONTH, THREE_MONTH, SIX_MONTH, TWELVE_MONTH)", schema = @Schema(implementation = InsightsSearchType.class)),
            @Parameter(name = "docTypes", description = ""),
            @Parameter(name = "providers", description = ""),
            @Parameter(name = "languages", description = ""),
    })
    @Operation(summary = "Insights 목록 조회 - with pagination", description = "Insights 목록을 조회합니다.")
    @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
    @GetMapping("/insights")
    // @RequirePermission(roles = { Role.ADMIN, Role.USER })
    public BaseResponse<PaginationRes<IGoodsDataRes>> getDataCatalog(
            @RequestParam(value = "customerCompanyNo", required = true) @Min(value = 1) Integer customerCompanyNo,
            @RequestParam(value = "page", required = false, defaultValue = "1") @Min(value = 1) int page,
            @RequestParam(value = "size", required = false, defaultValue = "10") @Min(value = 1) int size,
            @RequestParam(value = "keyword", required = false, defaultValue = "") String keyword,
            @RequestParam(value = "sort", required = false, defaultValue = "NEWEST") InsightsSortType sort,
            @RequestParam(value = "period", required = false, defaultValue = "ALL_DAY") InsightsSearchType period,
            @RequestParam(value = "docTypes", required = false, defaultValue = "") InsightsSearchType docTypes,
            @RequestParam(value = "providers", required = false, defaultValue = "") InsightsSearchType providers,
            @RequestParam(value = "languages", required = false, defaultValue = "") InsightsSearchType languages) {

        System.err.println(" ======== >>> " + keyword);
        System.err.println(" ======== >>> " + sort);
        System.err.println(" ======== >>> " + period);

        Role role = authorizationService.getCurrentRole();
        int cNo = RoleUtil.isAdmin(role) ? customerCompanyNo : authorizationService.getCompanyNo();

        return BaseResponseFactory.create(miInsightsService.getInsights(cNo, page, size, keyword));
    }

    @ApiOrder(4)
    @Parameters({
            @Parameter(name = "customerCompanyNo", description = "관리자: 선택 된 고객사 번호, 사용자: 자신이 속한 고객사 번호"),
            @Parameter(name = "goodsDataNo", description = "상품데이타번호")
    })
    @Operation(summary = "상품 데이타 상세 조회", description = "상품 데이타 상세를 조회합니다.")
    @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
    @GetMapping("/insights/detail")
    // @RequirePermission(roles = { Role.ADMIN, Role.USER })
    public BaseResponse<String> getGoodsData(
            @RequestParam(value = "customerCompanyNo", required = true) @Min(value = 1) Integer customerCompanyNo,
            @RequestParam(value = "goodsDataNo", required = true) @Min(value = 1) Integer goodsDataNo) {

        Role role = authorizationService.getCurrentRole();
        int cNo = RoleUtil.isAdmin(role) ? customerCompanyNo : authorizationService.getCompanyNo();

        return BaseResponseFactory.create("todo");
    }

    @ApiOrder(5)
    @Parameters({

    })
    @Operation(summary = "카테고리 목록 조회", description = "카테고리 목록을 조회합니다.")
    @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
    @GetMapping("/category")
    // @RequirePermission(roles = { Role.ADMIN, Role.USER })
    public BaseResponse<String> getCategory() {

        // Role role = authorizationService.getCurrentRole();
        // int cNo = RoleUtil.isAdmin(role) ? customerCompanyNo : authorizationService.getCompanyNo();

        return BaseResponseFactory.create("todo");
    }
}
