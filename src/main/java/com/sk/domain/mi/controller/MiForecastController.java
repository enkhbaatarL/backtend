package com.sk.domain.mi.controller;

import com.sk.common.annotation.ApiOrder;
import com.sk.common.annotation.RequiredPermission;
import com.sk.common.model.BaseResponse;
import com.sk.common.model.BaseResponseFactory;
import com.sk.common.utils.RoleUtil;
import com.sk.domain.mi.dto.response.forecast.chartFactor.ForecastChartFactorCategoryRes;
import com.sk.domain.mi.dto.response.forecast.chartFactor.ForecastChartFactorRes;
import com.sk.domain.mi.service.MiForecastService;
import com.sk.domain.shared.enums.PermissionCondition;
import com.sk.domain.shared.enums.Role;
import com.sk.domain.shared.service.permissions.AuthorizationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Profile("!dedicated")
@Tag(name = "18. AI Forecast API", description = "AI Forecast API 입니다.")
@RestController
@RequestMapping("/api/v1/mi/forecast")
@RequiredArgsConstructor
public class MiForecastController {
	private final AuthorizationService authorizationService;
	private final MiForecastService miForecastService;

	@ApiOrder(1)
	@Parameter(name = "companyNo", description = "관리자: 선택 된 고객사 번호, 사용자: 자신이 속한 고객사 번호")
	@Operation(summary = "Forecast 데이터 존재 여부 조회", description = "Forecast 데이터 존재 여부를 조회합니다.")
	@ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
	@GetMapping("/companies/{companyNo}/data/exists")
	@RequiredPermission(roles = {Role.ADMIN, Role.USER, Role.API_PLUS_OPERATOR,
			Role.API_SUB_ADMIN}, condition = PermissionCondition.THIRD_PARTY_ADMIN_ONLY)
	public BaseResponse<Boolean> getDataExists(
        @PathVariable Integer companyNo) {

		Role role = authorizationService.getCurrentRole();
		int cNo = RoleUtil.isAdmin(role) ? companyNo : authorizationService.getCompanyNo();

		return BaseResponseFactory.create(miForecastService.getDataExists(cNo));
	}

	@ApiOrder(9)
	@Parameters({
			@Parameter(name = "companyNo", description = "관리자: 선택 된 고객사 번호, 사용자: 자신이 속한 고객사 번호"),
			@Parameter(name = "goodsDataSourceNo", description = "데이터 소스 번호", example = "63"),
			@Parameter(name = "predictDate", description = "예측 날짜", example = "20250511")
	})
	@Operation(summary = "Forecast 차트 주요 요인 분석 카테고리 조회",
			description = "Forecast 차트 주요 요인 분석 카테고리를 조회합니다.")
	@ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
	@GetMapping("/companies/{companyNo}/chart/factor/category")
	@RequiredPermission(roles = {Role.ADMIN, Role.USER, Role.API_PLUS_OPERATOR,
			Role.API_SUB_ADMIN}, condition = PermissionCondition.THIRD_PARTY_ADMIN_ONLY)
	public BaseResponse<ForecastChartFactorCategoryRes> getChartFactorCategory(
			@PathVariable("companyNo") Integer companyNo,
			@RequestParam(value = "goodsDataSourceNo", required = true) int goodsDataSourceNo,
			@RequestParam(value = "predictDate", required = true) String predictDate) {
		return BaseResponseFactory.create(
				miForecastService.getChartFactorCategory(companyNo, goodsDataSourceNo, predictDate));
	}

	@ApiOrder(10)
	@Parameters({
			@Parameter(name = "companyNo", description = "관리자: 선택 된 고객사 번호, 사용자: 자신이 속한 고객사 번호"),
			@Parameter(name = "predictDate", description = "예측 날짜", example = "20250511"),
			@Parameter(name = "goodsDataSourceNo", description = "데이터 소스 번호", example = "63"),
			@Parameter(name = "explanatoryLevelNm", description = "카테고리", example = "WEATHER")
	})
	@Operation(summary = "Forecast 차트 주요 요인 분석 조회",
			description = "Forecast 차트 주요 요인 분석 데이터를 조회합니다.")
	@ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
	@GetMapping("/companies/{companyNo}/chart/factors")
	@RequiredPermission(roles = {Role.ADMIN, Role.USER, Role.API_PLUS_OPERATOR,
			Role.API_SUB_ADMIN}, condition = PermissionCondition.THIRD_PARTY_ADMIN_ONLY)
	public BaseResponse<ForecastChartFactorRes> getChartFactors(
			@PathVariable("companyNo") Integer companyNo,
			@RequestParam(value = "predictDate", required = true) String predictDate,
			@RequestParam(value = "goodsDataSourceNo", required = true) int goodsDataSourceNo,
			@RequestParam(value = "explanatoryLevelNm", required = true) String explanatoryLevelNm) {
		return BaseResponseFactory.create(
				miForecastService.getChartFactors(companyNo, goodsDataSourceNo, predictDate, explanatoryLevelNm));
	}
}
