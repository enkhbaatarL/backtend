package com.sk.domain.mi.controller;

import com.sk.common.annotation.ApiOrder;
import com.sk.common.annotation.RequiredPermission;
import com.sk.common.model.BaseResponse;
import com.sk.common.model.BaseResponseFactory;
import com.sk.domain.mi.dto.response.forecast.ForecastChartRes;
import com.sk.domain.mi.dto.response.forecast.ForecastInfoRes;
import com.sk.domain.mi.dto.response.forecast.summary.ForecastReportDetailRes;
import com.sk.domain.mi.dto.response.forecast.summary.ForecastReportRes;
import com.sk.domain.mi.service.MiForecastService;
import com.sk.domain.shared.enums.PermissionCondition;
import com.sk.domain.shared.enums.Role;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "18. AI Forecast API", description = "AI Forecast API 입니다.")
@RestController
@RequestMapping("/api/v1/mi/forecast")
@RequiredArgsConstructor
public class MiForecastDedicatedController {
	private final MiForecastService miForecastService;

	@ApiOrder(2)
	@Parameter(name = "companyNo", description = "관리자: 선택 된 고객사 번호, 사용자: 자신이 속한 고객사 번호")
	@Operation(summary = "Forecast 기본 정보 조회", description = "Forecast 기본 정보를 조회합니다.")
	@ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
	@GetMapping("/companies/{companyNo}/info")
	@RequiredPermission(roles = {Role.ADMIN, Role.USER, Role.API_PLUS_OPERATOR,
			Role.API_SUB_ADMIN}, condition = PermissionCondition.THIRD_PARTY_ADMIN_ONLY)
	public BaseResponse<ForecastInfoRes> getForecastInfo(
        @PathVariable Integer companyNo) {
		return BaseResponseFactory.create(miForecastService.getForecastInfo(companyNo));
	}

	@ApiOrder(4)
	@Parameter(name = "companyNo", description = "관리자: 선택 된 고객사 번호, 사용자: 자신이 속한 고객사 번호")
	@Parameter(name = "xAxisUnit", description = "x축 단위(1D, 1W, 1M, 1Q, 1Y)")
	@Parameter(name = "period", description = "전체기간(1W, 1M, 3M, 6M, YTD, 1Y, ALL)")
	@Operation(summary = "Forecast 차트 기간 조회", description = "Forecast 차트 기간을 조회합니다.")
	@ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
	@GetMapping("/companies/{companyNo}/chart")
	@RequiredPermission(roles = {Role.ADMIN, Role.USER, Role.API_PLUS_OPERATOR,
			Role.API_SUB_ADMIN}, condition = PermissionCondition.THIRD_PARTY_ADMIN_ONLY)
	public BaseResponse<ForecastChartRes> getChart(
			@PathVariable Integer companyNo,
			@RequestParam String xAxisUnit,
			@RequestParam String period) {
		return BaseResponseFactory.create(miForecastService.getChart(companyNo, xAxisUnit, period));
	}

	@ApiOrder(5)
	@Parameter(name = "companyNo", description = "관리자: 선택 된 고객사 번호, 사용자: 자신이 속한 고객사 번호")
	@Parameter(name = "goodsDataSourceNo", description = "데이터 소스 번호(선택)")
	@Operation(summary = "Forecast report 목록 조회", description = "Forecast report 목록을 조회합니다.")
	@ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
	@GetMapping("/companies/{companyNo}/reports")
	@RequiredPermission(roles = {Role.ADMIN, Role.USER, Role.API_PLUS_OPERATOR,
			Role.API_SUB_ADMIN}, condition = PermissionCondition.THIRD_PARTY_ADMIN_ONLY)
	public BaseResponse<List<ForecastReportRes>> getForecastSummaries(
			@PathVariable Integer companyNo,
			@RequestParam(required = false) Integer goodsDataSourceNo) {
		return BaseResponseFactory.create(miForecastService.getForecastReports(companyNo, goodsDataSourceNo));
	}

	@ApiOrder(6)
	@Parameter(name = "companyNo", description = "관리자: 선택 된 고객사 번호, 사용자: 자신이 속한 고객사 번호")
	@Parameter(name = "marketIndexAnalysisTargetCd", description = "시장지수분석대상코드")
	@Parameter(name = "predictionFulfillDt", description = "예측수행일자")
	@Parameter(name = "mnemonicCd", description = "MNEMONIC코드")
	@Parameter(name = "predictionCriteriaDt", description = "예측기준일자")
	@Operation(summary = "Forecast report 상세 조회", description = "Forecast report 상세 정보를 조회합니다.")
	@ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
	@GetMapping("/companies/{companyNo}/reports/detail")
	@RequiredPermission(roles = {Role.ADMIN, Role.USER, Role.API_PLUS_OPERATOR,
			Role.API_SUB_ADMIN}, condition = PermissionCondition.THIRD_PARTY_ADMIN_ONLY)
	public BaseResponse<ForecastReportDetailRes> getForecastReportDetail(
			@PathVariable Integer companyNo,
			@RequestParam String marketIndexAnalysisTargetCd,
			@RequestParam String predictionFulfillDt,
			@RequestParam String mnemonicCd,
			@RequestParam String predictionCriteriaDt) {
		return BaseResponseFactory.create(miForecastService.getForecastReportDetail(companyNo, marketIndexAnalysisTargetCd,
				predictionFulfillDt, mnemonicCd, predictionCriteriaDt));
	}
}
