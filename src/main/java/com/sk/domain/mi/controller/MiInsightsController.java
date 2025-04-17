package com.sk.domain.mi.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.sk.common.annotation.ApiOrder;
import com.sk.common.annotation.RequirePermission;
import com.sk.common.model.BaseResponse;
import com.sk.common.model.BaseResponseFactory;
import com.sk.common.model.PaginationRes;
import com.sk.common.utils.RoleUtil;
import com.sk.domain.mi.dto.response.insights.GoodsDataACRes;
import com.sk.domain.mi.dto.response.insights.GoodsDataRes;
import com.sk.domain.mi.service.MiInsightsService;
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
    
    @ApiOrder(1)
    @Parameters({
            @Parameter(name = "customerCompanyNo", description = "관리자: 선택 된 고객사 번호, 사용자: 자신이 속한 고객사 번호")
    })
    @Operation(summary = "Focused 목록 조회", description = "Focused 목록을 조회합니다.")
    @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
    @GetMapping("/focused")
    @RequirePermission(roles = { Role.ADMIN, Role.USER })
    public BaseResponse<List<GoodsDataRes>> getFocused(
            @RequestParam(value = "customerCompanyNo") @Min(value = 1) Integer customerCompanyNo) {

        int companyNo = authorizationService.getCompanyNo();
        Role role = authorizationService.getCurrentRole();
        int cNo = RoleUtil.isAdmin(role) ? customerCompanyNo : companyNo;

        return BaseResponseFactory.create(miInsightsService.getFocused(cNo));
    }

    @ApiOrder(2)
    @Parameters({
            @Parameter(name = "keyword", description = "데이타명 또는 데이타설명 검색"),
            @Parameter(name = "customerCompanyNo", description = "관리자: 선택 된 고객사 번호, 사용자: 자신이 속한 고객사 번호")
    })
    @Operation(summary = "Data Catalog 검색에서 사용할 자동완성", description = "Data Catalog 검색에서 사용할 자동완성 데이타를 조회합니다.")
    @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
    @GetMapping("/data-catalog/autocomplete")
    @RequirePermission(roles = { Role.ADMIN, Role.USER })
    public BaseResponse<List<GoodsDataACRes>> getDataCatalogAutoComplete(
            @RequestParam(value = "keyword", required = false, defaultValue = "") String keyword,
            @RequestParam(value = "customerCompanyNo") @Min(value = 1) Integer customerCompanyNo) {

        int companyNo = authorizationService.getCompanyNo();
        Role role = authorizationService.getCurrentRole();
        int cNo = RoleUtil.isAdmin(role) ? customerCompanyNo : companyNo;

        return BaseResponseFactory.create(miInsightsService.getDataCatalogAutoComplete(cNo, keyword));
    }

    @ApiOrder(3)
    @Parameters({
            @Parameter(name = "page", description = "페이지 번호 (기본값: 1)"),
            @Parameter(name = "size", description = "페이지당 항목 수 (기본값: 20)"),
            @Parameter(name = "keyword", description = "데이타명 또는 데이타설명 검색"),
            @Parameter(name = "customerCompanyNo", description = "관리자: 선택 된 고객사 번호, 사용자: 자신이 속한 고객사 번호")
    })
    @Operation(summary = "Data Catalog 목록 조회 - with pagination", description = "Data Catalog 목록을 조회합니다.")
    @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
    @GetMapping("/data-catalog")
    @RequirePermission(roles = { Role.ADMIN, Role.USER })
    public BaseResponse<PaginationRes<GoodsDataRes>> getDataCatalog(
            @RequestParam(value = "page", required = false, defaultValue = "1") @Min(value = 1) int page,
            @RequestParam(value = "size", required = false, defaultValue = "20") @Min(value = 1) int size,
            @RequestParam(value = "keyword", required = false, defaultValue = "") String keyword,
            @RequestParam(value = "customerCompanyNo") @Min(value = 1) Integer customerCompanyNo) {

        int companyNo = authorizationService.getCompanyNo();
        Role role = authorizationService.getCurrentRole();
        int cNo = RoleUtil.isAdmin(role) ? customerCompanyNo : companyNo;

        return BaseResponseFactory.create(miInsightsService.getDataCatalog(cNo, page, size, keyword));
    }

}
