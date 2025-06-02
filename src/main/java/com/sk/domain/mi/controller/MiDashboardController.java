package com.sk.domain.mi.controller;

import java.util.List;

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
import com.sk.common.utils.RoleUtil;
import com.sk.domain.mi.dto.response.dashboard.DashboardInfoRes;
import com.sk.domain.mi.dto.response.dashboard.LatestInsightsRes;
import com.sk.domain.mi.dto.response.dashboard.charts.DashboardChartFactorCategoryRes;
import com.sk.domain.mi.dto.response.dashboard.charts.DashboardChartFactorRes;
import com.sk.domain.mi.dto.response.dashboard.charts.DashboardChartRes;
import com.sk.domain.mi.dto.response.marketData.charts.MiMarketDataSummaryRes;
import com.sk.domain.mi.service.MiDashboardService;
import com.sk.domain.mi.service.MiMarketDataService;
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

@Tag(name = "01. Dashboard API", description = "Dashboard API 입니다.")
@Slf4j
@RestController
@RequestMapping("/api/v1/mi/dashboard")
@RequiredArgsConstructor
public class MiDashboardController {
	private final AuthorizationService authorizationService;
	private final MiDashboardService miDashboardService;
	private final MiMarketDataService miMarketDataService;

	@ApiOrder(1)
	@Parameters({
			@Parameter(name = "companyNo", description = "관리자: 선택 된 고객사 번호, 사용자: 자신이 속한 고객사 번호")
	})
	@Operation(summary = "대시보드 데이터 존재 여부 조회", description = "대시보드 데이터 존재 여부를 조회합니다.")
	@ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
	@GetMapping("/companies/{companyNo}/data/exists")
	@RequiredPermission(roles = {Role.ADMIN, Role.USER})
	public BaseResponse<Boolean> getDataExists(
			@PathVariable("companyNo") Integer companyNo) {

		Role role = authorizationService.getCurrentRole();
		int cNo = RoleUtil.isAdmin(role) ? companyNo : authorizationService.getCompanyNo();

		return BaseResponseFactory.create(miDashboardService.getDataExists(cNo));
	}

	@ApiOrder(2)
	@Parameters({
			@Parameter(name = "companyNo", description = "관리자: 선택 된 고객사 번호, 사용자: 자신이 속한 고객사 번호")
	})
	@Operation(summary = "대시보드 기본 정보 조회", description = "대시보드 기본 정보를 조회합니다.")
	@ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
	@GetMapping("/companies/{companyNo}/info")
	@RequiredPermission(roles = {Role.ADMIN, Role.USER})
	public BaseResponse<DashboardInfoRes> getInfo(
			@PathVariable("companyNo") Integer companyNo) {
		return BaseResponseFactory.create(miDashboardService.getInfo(companyNo));
	}

	@ApiOrder(3)
	@Operation(summary = "DashBoard 화면에서 쓰이는 Market Data 목록 조회", description = "DashBoard 화면에서 쓰이는 Market Data 목록을 조회합니다.")
	@ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
	@GetMapping("/companies/{companyNo}/market-datas")
	@RequiredPermission(roles = {Role.ADMIN, Role.USER})
	public BaseResponse<List<MiMarketDataSummaryRes>> getMarketDataSummary(
			@PathVariable("companyNo") Integer companyNo,
			Authentication authentication) {
		return BaseResponseFactory.create(miMarketDataService.getMarketDataSummary(companyNo, authentication));
	}

	@ApiOrder(4)
	@Parameters({
			@Parameter(name = "companyNo", description = "관리자: 선택 된 고객사 번호, 사용자: 자신이 속한 고객사 번호")
	})
	@Operation(summary = "Latest Insights 목록 조회", description = "Latest Insights 목록을 조회합니다.")
	@ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
	@GetMapping("/companies/{companyNo}/latest-insights")
	@RequiredPermission(roles = {Role.ADMIN, Role.USER})
	public BaseResponse<List<LatestInsightsRes>> getLatestInsights(
			@PathVariable("companyNo") Integer companyNo) {

		Role role = authorizationService.getCurrentRole();
		int cNo = RoleUtil.isAdmin(role) ? companyNo : authorizationService.getCompanyNo();

		return BaseResponseFactory.create(miDashboardService.getLatestInsights(cNo));
	}

	@ApiOrder(5)
	@Parameters({
			@Parameter(name = "companyNo", description = "관리자: 선택 된 고객사 번호, 사용자: 자신이 속한 고객사 번호")
	})
	@Operation(summary = "대시보드 기본 정보 조회", description = "대시보드 기본 정보를 조회합니다.")
	@ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
	@GetMapping("/companies/{companyNo}/dashboard-info")
	@RequiredPermission(roles = {Role.ADMIN, Role.USER})
	public BaseResponse<DashboardInfoRes> getDashboardInfo(
			@PathVariable("companyNo") Integer companyNo) {
		return BaseResponseFactory.create(miDashboardService.getDashboardInfo(companyNo));
	}

	@ApiOrder(6)
	@Parameters({
		@Parameter(name = "page", description = "페이지 번호", example = "1"),
		@Parameter(name = "size", description = "페이지 크기", example = "10"),
		@Parameter(name = "companyNo", description = "관리자: 선택 된 고객사 번호, 사용자: 자신이 속한 고객사 번호"),
	})
	@Operation(summary = "대시보드 차트 조회 가능한 날짜 리스트", description = "대시보드 차트 조회 가능한 날짜 리스트를 조회합니다.")
	@ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
	@GetMapping("/companies/{companyNo}/chart-dates")
	@RequiredPermission(roles = {Role.ADMIN, Role.USER})
	public BaseResponse<List<String>> getChartDates(
			@RequestParam(value = "page", required = false, defaultValue = "1") @Min(value = 1) int page,
            @RequestParam(value = "size", required = false, defaultValue = "10") @Min(value = 10) int size,
			@PathVariable("companyNo") Integer companyNo) {
		return BaseResponseFactory.create(miDashboardService.getChartDates(page, size, companyNo));
	}

	@ApiOrder(7)
	@Parameters({
			@Parameter(name = "companyNo", description = "관리자: 선택 된 고객사 번호, 사용자: 자신이 속한 고객사 번호"),
			@Parameter(name = "date", description = "조회할 년월", example = "202505")
	})
	@Operation(summary = "대시보드 차트 데이터 조회", description = "대시보드 차트 데이터를 조회합니다.")
	@ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
	@GetMapping("/companies/{companyNo}/chart")
	@RequiredPermission(roles = {Role.ADMIN, Role.USER})
	public BaseResponse<DashboardChartRes> getChart(
			@PathVariable("companyNo") Integer companyNo,
			@RequestParam(value = "date", required = false) String date,
			Authentication authentication) {
		return BaseResponseFactory.create(miDashboardService.getChart(companyNo, date, authentication));
	}

	@ApiOrder(8)
	@Parameters({
			@Parameter(name = "predictDate", description = "예측 날짜", example = "20250511"),
			@Parameter(name = "goodsDataSourceNo", description = "데이터 소스 번호", example = "1")
	})
	@Operation(summary = "대시보드 차트 주요 요인 분석 카테고리 조회", description = "대시보드 차트 주요 요인 분석 카테고리를 조회합니다.")
	@ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
	@GetMapping("/companies/{companyNo}/chart/factor/category")
	@RequiredPermission(roles = {Role.ADMIN, Role.USER})
	public BaseResponse<DashboardChartFactorCategoryRes> getChartFactorCategory(
			@PathVariable("companyNo") Integer companyNo,
			@RequestParam(value = "predictDate", required = true) String predictDate,
			@RequestParam(value = "goodsDataSourceNo", required = true) int goodsDataSourceNo) {
		return BaseResponseFactory.create(miDashboardService.getChartFactorCategory(companyNo, goodsDataSourceNo, predictDate));
	}

	@ApiOrder(9)
	@Parameters({
			@Parameter(name = "predictDate", description = "예측 날짜", example = "20250511"),
			@Parameter(name = "goodsDataSourceNo", description = "데이터 소스 번호", example = "1"),
			@Parameter(name = "category", description = "카테고리", example = "WEATHER")
	})
	@Operation(summary = "대시보드 차트 주요 요인 분석 조회", description = "대시보드 차트 주요 요인 분석 데이터를 조회합니다.")
	@ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
	@GetMapping("/companies/{companyNo}/chart/factors")
	@RequiredPermission(roles = {Role.ADMIN, Role.USER})
	public BaseResponse<DashboardChartFactorRes> getChartFactors(
			@PathVariable("companyNo") Integer companyNo,
			@RequestParam(value = "predictDate", required = true) String predictDate,
			@RequestParam(value = "goodsDataSourceNo", required = true) int goodsDataSourceNo,
			@RequestParam(value = "category", required = true) String category) {
		return BaseResponseFactory.create(miDashboardService.getChartFactors(companyNo, goodsDataSourceNo, predictDate, category));
	}

	// @ApiOrder(10)
	// @Parameters({
	// 		@Parameter(name = "companyNo", description = "관리자: 선택 된 고객사 번호, 사용자: 자신이 속한 고객사 번호")
	// })
	// @Operation(summary = "대시보드 차트 주요 요인 분석 조회", description = "대시보드 차트 주요 요인 분석 데이터를 조회합니다.")
	// @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
	// @GetMapping("/companies/{companyNo}/chart/factor/graph")
	// @RequiredPermission(roles = {Role.ADMIN, Role.USER})
	// public BaseResponse<DashboardChartFactorGraphRes> getChartFactorGraph(
	// 		@PathVariable("companyNo") Integer companyNo,
	// 		@RequestParam(value = "predictDate", required = true) String predictDate,
	// 		@RequestParam(value = "goodsDataSourceNo", required = true) int goodsDataSourceNo,
	// 		@RequestParam(value = "category", required = true) String category) {
	// 	return BaseResponseFactory.create(miDashboardService.getChartFactorGraph(companyNo, goodsDataSourceNo, predictDate, category));
	// }
}
