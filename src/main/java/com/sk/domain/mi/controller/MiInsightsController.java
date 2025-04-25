package com.sk.domain.mi.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.sk.common.annotation.ApiOrder;
import com.sk.common.annotation.RequiredPermission;
import com.sk.common.model.BaseResponse;
import com.sk.common.model.BaseResponseFactory;
import com.sk.common.model.PaginationRes;
import com.sk.common.utils.RoleUtil;
import com.sk.domain.mi.dto.response.insights.FocusedDataRes;
import com.sk.domain.mi.dto.response.insights.GoodsDataFilterRes;
import com.sk.domain.mi.dto.response.insights.InsightsDataRes;
import com.sk.domain.mi.service.MiInsightsService;
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
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Tag(name = "04. Insights API", description = "Insights API 입니다.")
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

    /**
     * Focused 목록 조회
     *
     * @param customerCompanyNo 고객사 번호
     * @return List<FocusedDataRes>
     */
    @ApiOrder(1)
    @Parameters({
            @Parameter(name = "customerCompanyNo", description = "관리자: 선택 된 고객사 번호, 사용자: 자신이 속한 고객사 번호")
    })
    @Operation(summary = "Focused 목록 조회", description = "Focused 목록을 조회합니다.")
    @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
    @GetMapping("/focused-data")
    @RequiredPermission(roles = {Role.ADMIN, Role.USER})
    public BaseResponse<List<FocusedDataRes>> getFocused(
            @RequestParam(value = "customerCompanyNo") @Min(value = 1) Integer customerCompanyNo) {

        Role role = authorizationService.getCurrentRole();
        int cNo = RoleUtil.isAdmin(role) ? customerCompanyNo : authorizationService.getCompanyNo();

        return BaseResponseFactory.create(miInsightsService.getFocused(cNo));
    }

    /**
     * Insights 고급 필터 목록 조회
     *
     * @return GoodsDataFilterRes
     */
    @ApiOrder(2)
    @Operation(summary = "Insights 검색에서 사용할 고급필터", description = "Insights 검색에서 사용할 고급필터 데이타를 조회합니다.")
    @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
    @GetMapping("/insights/advanced-filter")
    @RequiredPermission(roles = {Role.ADMIN, Role.USER})
    public BaseResponse<GoodsDataFilterRes> getAdvancedFilter() {

        return BaseResponseFactory.create(miInsightsService.getAdvancedFilter());
    }

    /**
     * Insights 목록 조회
     *
     * @param customerCompanyNo 고객사 번호
     * @param page              페이지 번호
     * @param size              페이지당 항목 수
     * @param keyword           검색어
     * @param sort              정렬 기준
     * @param period            기간 필터
     * @param docTypes          문서 유형 필터
     * @param providers         프로바이더 유형 필터
     * @param languages         언어 유형 필터
     * @return PaginationRes<InsightsDataRes>
     */
    @ApiOrder(3)
    @Parameters({
            @Parameter(name = "customerCompanyNo", description = "관리자: 선택 된 고객사 번호, 사용자: 자신이 속한 고객사 번호"),
            @Parameter(name = "page", description = "페이지 번호 (기본값: 1)"),
            @Parameter(name = "size", description = "페이지당 항목 수 (기본값: 10)"),
            @Parameter(name = "keyword", description = "데이타명 검색"),
            @Parameter(name = "sort", description = "정렬 기준", schema = @Schema(implementation = InsightsSortType.class)),
            @Parameter(name = "period", in = ParameterIn.QUERY, description = "기간 필터", schema = @Schema(implementation = InsightsPeriodType.class)),
            @Parameter(name = "filters", description = "고급 필터(A_INSIGHT, A_CMMDT_MRKT_RPT, A_LSEG, A_WOODMAC, A_KOR, A_ENG)", example = ""),
    })
    @Operation(summary = "Insights 목록 조회 - with pagination", description = "Insights 목록을 조회합니다.")
    @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
    @GetMapping("/insights-data")
    @RequiredPermission(roles = {Role.ADMIN, Role.USER})
    public BaseResponse<PaginationRes<InsightsDataRes>> getDataCatalog(
            @RequestParam(value = "customerCompanyNo", required = true) @Min(value = 1) Integer customerCompanyNo,
            @RequestParam(value = "page", required = false, defaultValue = "1") @Min(value = 1) int page,
            @RequestParam(value = "size", required = false, defaultValue = "10") @Min(value = 1) int size,
            @RequestParam(value = "keyword", required = false, defaultValue = "") String keyword,
            @RequestParam(value = "sort", required = false, defaultValue = "NEWEST") InsightsSortType sort,
            @RequestParam(value = "period", required = false, defaultValue = "ALL_DATES") InsightsPeriodType period,
            @RequestParam(value = "filters", required = false, defaultValue = "") String filters) {

        Role role = authorizationService.getCurrentRole();
        int cNo = RoleUtil.isAdmin(role) ? customerCompanyNo : authorizationService.getCompanyNo();

        return BaseResponseFactory
                .create(miInsightsService.getInsights(cNo, page, size, keyword, sort, period, filters));
    }
}
