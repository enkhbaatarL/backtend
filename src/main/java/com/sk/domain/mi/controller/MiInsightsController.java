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
import com.sk.common.model.PaginationRes;
import com.sk.common.utils.RoleUtil;
import com.sk.domain.mi.dto.response.insights.FocusedDataDto;
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

    @ApiOrder(1)
    @Operation(summary = "Focused 목록 조회", description = "Focused 목록을 조회합니다.")
    @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
    @GetMapping("/companies/{companyNo}/focused-data")
    @RequiredPermission(roles = {Role.ADMIN, Role.USER})
    public BaseResponse<List<FocusedDataDto>> getFocused(@PathVariable(value = "companyNo") Integer companyNo) {

        Role role = authorizationService.getCurrentRole();
        int cNo = RoleUtil.isAdmin(role) ? companyNo : authorizationService.getCompanyNo();

        return BaseResponseFactory.create(miInsightsService.getFocused(cNo));
    }

    @ApiOrder(2)
    @Operation(summary = "Insights 검색에서 사용할 고급필터", description = "Insights 검색에서 사용할 고급필터 데이타를 조회합니다.")
    @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
    @GetMapping("/companies/{companyNo}/advanced-filter")
    @RequiredPermission(roles = {Role.ADMIN, Role.USER})
    public BaseResponse<GoodsDataFilterRes> getAdvancedFilter(@PathVariable(value = "companyNo") Integer companyNo) {

        Role role = authorizationService.getCurrentRole();
        int cNo = RoleUtil.isAdmin(role) ? companyNo : authorizationService.getCompanyNo();

        return BaseResponseFactory.create(miInsightsService.getAdvancedFilter(cNo));
    }

    @ApiOrder(3)
    @Parameters({
            @Parameter(name = "page", description = "페이지번호 (기본값: 1)"),
            @Parameter(name = "size", description = "페이지당항목수 (기본값: 10)"),
            @Parameter(name = "keyword", description = "검색키워드"),
            @Parameter(name = "sort", description = "정렬기준", schema = @Schema(implementation = InsightsSortType.class)),
            @Parameter(name = "period", in = ParameterIn.QUERY, description = "기간필터", schema = @Schema(implementation = InsightsPeriodType.class)),
            @Parameter(name = "filters", description = "고급필터(A_INSIGHT, A_CMMDT_MRKT_RPT, A_LSEG, A_WOODMAC, A_KOR, A_ENG 등)"),
    })
    @Operation(summary = "Insights 목록 조회 - with pagination", description = "Insights 목록을 조회합니다.")
    @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
    @GetMapping("/companies/{companyNo}/insights-data")
    @RequiredPermission(roles = {Role.ADMIN, Role.USER})
    public BaseResponse<PaginationRes<InsightsDataRes>> getDataCatalog(
            @PathVariable(value = "companyNo") Integer companyNo,
            @RequestParam(value = "page", required = false, defaultValue = "1") @Min(value = 1) int page,
            @RequestParam(value = "size", required = false, defaultValue = "10") @Min(value = 1) int size,
            @RequestParam(value = "keyword", required = false, defaultValue = "") String keyword,
            @RequestParam(value = "sort", required = false, defaultValue = "NEWEST") InsightsSortType sort,
            @RequestParam(value = "period", required = false, defaultValue = "ALL_DATES") InsightsPeriodType period,
            @RequestParam(value = "filters", required = false, defaultValue = "") String filters) {

        Role role = authorizationService.getCurrentRole();
        int cNo = RoleUtil.isAdmin(role) ? companyNo : authorizationService.getCompanyNo();

        return BaseResponseFactory
                .create(miInsightsService.getInsights(cNo, page, size, keyword, sort, period, filters));
    }

    @ApiOrder(4)
    @Operation(summary = "", description = "")
    @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
    @GetMapping("/insights/create-dummy")
    @RequiredPermission(roles = {Role.ADMIN, Role.USER})
    public BaseResponse<String> createDummy() {

        miInsightsService.createDummyData();

        return BaseResponseFactory.create("success");
    }
}
