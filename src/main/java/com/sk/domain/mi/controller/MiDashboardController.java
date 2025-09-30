package com.sk.domain.mi.controller;

import com.sk.domain.admin.dto.response.file.FileDownloadRes;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.springframework.context.annotation.Profile;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.sk.common.annotation.ApiOrder;
import com.sk.common.annotation.RequiredPermission;
import com.sk.common.model.BaseResponse;
import com.sk.common.model.BaseResponseFactory;
import com.sk.common.utils.DateUtil;
import com.sk.common.utils.RoleUtil;
import com.sk.domain.mi.dto.response.dashboard.LatestInsightsRes;
import com.sk.domain.mi.dto.response.dashboard.chartFactor.DashboardChartFactorCategoryRes;
import com.sk.domain.mi.dto.response.dashboard.chartFactor.DashboardChartFactorRes;
import com.sk.domain.mi.dto.response.dashboard.summary.DashboardSummaryRes;
import com.sk.domain.mi.dto.response.marketData.charts.MiMarketDataSummaryRes;
import com.sk.domain.mi.service.MiDashboardService;
import com.sk.domain.mi.service.MiMarketDataService;
import com.sk.domain.shared.enums.PermissionCondition;
import com.sk.domain.shared.enums.Role;
import com.sk.domain.shared.service.permissions.AuthorizationService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Profile("!dedicated")
@Tag(name = "01-1. AI Forecast (구 Dashboard) API", description = "AI Forecast (구 Dashboard) API 입니다.")
@Slf4j
@RestController
@RequestMapping("/api/v1/mi/dashboard")
@RequiredArgsConstructor
public class MiDashboardController {
	private final AuthorizationService authorizationService;
	private final MiDashboardService miDashboardService;

	@ApiOrder(1)
	@Parameters({
			@Parameter(name = "companyNo", description = "관리자: 선택 된 고객사 번호, 사용자: 자신이 속한 고객사 번호")
	})
	@Operation(summary = "대시보드 데이터 존재 여부 조회", description = "대시보드 데이터 존재 여부를 조회합니다.")
	@ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
	@GetMapping("/companies/{companyNo}/data/exists")
	@RequiredPermission(roles = {Role.ADMIN, Role.USER, Role.API_PLUS_OPERATOR,
			Role.API_SUB_ADMIN}, condition = PermissionCondition.THIRD_PARTY_ADMIN_ONLY)
	public BaseResponse<Boolean> getDataExists(
			@PathVariable("companyNo") Integer companyNo) {

		Role role = authorizationService.getCurrentRole();
		int cNo = RoleUtil.isAdmin(role) ? companyNo : authorizationService.getCompanyNo();

		return BaseResponseFactory.create(miDashboardService.getDataExists(cNo));
	}

	@ApiOrder(4)
	@Parameters({
			@Parameter(name = "companyNo", description = "관리자: 선택 된 고객사 번호, 사용자: 자신이 속한 고객사 번호")
	})
	@Operation(summary = "Latest Insights 목록 조회", description = "Latest Insights 목록을 조회합니다.")
	@ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
	@GetMapping("/companies/{companyNo}/latest-insights")
	@RequiredPermission(roles = {Role.ADMIN, Role.USER, Role.API_PLUS_OPERATOR,
			Role.API_SUB_ADMIN}, condition = PermissionCondition.THIRD_PARTY_ADMIN_ONLY)
	public BaseResponse<List<LatestInsightsRes>> getLatestInsights(
			@PathVariable("companyNo") Integer companyNo) {

		Role role = authorizationService.getCurrentRole();
		int cNo = RoleUtil.isAdmin(role) ? companyNo : authorizationService.getCompanyNo();

		return BaseResponseFactory.create(miDashboardService.getLatestInsights(cNo));
	}

	@ApiOrder(6)
	@Parameters({
			@Parameter(name = "companyNo", description = "관리자: 선택 된 고객사 번호, 사용자: 자신이 속한 고객사 번호"),
			@Parameter(name = "date", description = "조회할 년월(YYYYMMDD)", example = "20250511")
	})
	@Operation(summary = "대시보드 Summary 데이터 조회", description = "대시보드 Summary 데이터를 조회합니다.")
	@ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
	@GetMapping("/companies/{companyNo}/summary")
	@RequiredPermission(roles = {Role.ADMIN, Role.USER, Role.API_PLUS_OPERATOR,
			Role.API_SUB_ADMIN}, condition = PermissionCondition.THIRD_PARTY_ADMIN_ONLY)
	public BaseResponse<DashboardSummaryRes> getDashboardSummary(
			@PathVariable("companyNo") Integer companyNo,
			@RequestParam(value = "date", required = true) String date) {
		return BaseResponseFactory.create(miDashboardService.getDashboardSummary(companyNo, date));
	}

	@ApiOrder(7)
	@Parameters({
			@Parameter(name = "companyNo", description = "관리자: 선택 된 고객사 번호, 사용자: 자신이 속한 고객사 번호"),
			@Parameter(name = "date", description = "조회할 년월(YYYYMMDD), ", example = "20250511"),
			@Parameter(name = "predictDate", description = "예측 날짜(YYYYMM)", example = "202506")
	})
	@Operation(summary = "대시보드 Summary 데이터 조회", description = "대시보드 Summary 데이터를 조회합니다.")
	@ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
	@GetMapping("/companies/{companyNo}/summary/file-download")
	@RequiredPermission(roles = {Role.ADMIN, Role.USER, Role.API_PLUS_OPERATOR,
			Role.API_SUB_ADMIN}, condition = PermissionCondition.THIRD_PARTY_ADMIN_ONLY)
	public ResponseEntity<byte[]> getDashboardSummaryFileDownload(
			@PathVariable("companyNo") Integer companyNo,
			@RequestParam(value = "date", required = true) String date,
			@RequestParam(value = "predictDate", required = false) String predictDate) throws UnsupportedEncodingException {

		String FILE_PREFIX = "dashboard_summary";
		String encodedFileName = URLEncoder.encode(
				FILE_PREFIX.concat("_").concat(date).concat("_").concat(DateUtil.currentYYYYMMDDHHMMSS()).concat(".zip"), StandardCharsets.UTF_8);
        FileDownloadRes fileDownloadRes = miDashboardService.getDashboardSummaryFileDownload(companyNo, date, predictDate, encodedFileName);

        ContentDisposition contentDisposition = ContentDisposition.attachment()
                .filename(fileDownloadRes.fileNm(), StandardCharsets.UTF_8)
                    .build();

        log.debug("Dashboard Summary Download File Info. FileName={}, FileSize={}", fileDownloadRes.fileNm(), fileDownloadRes.fileSize());
		HttpHeaders httpHeaders = new HttpHeaders();
		httpHeaders.setContentType(MediaType.APPLICATION_OCTET_STREAM);
		httpHeaders.setContentLength(fileDownloadRes.fileSize());
		httpHeaders.add(HttpHeaders.CONTENT_DISPOSITION, contentDisposition.toString());

		return new ResponseEntity<>(fileDownloadRes.fileData(), httpHeaders, HttpStatus.OK);
	}

	@ApiOrder(10)
	@Parameters({
			@Parameter(name = "companyNo", description = "관리자: 선택 된 고객사 번호, 사용자: 자신이 속한 고객사 번호"),
			@Parameter(name = "predictDate", description = "예측 날짜", example = "20250511"),
			@Parameter(name = "date", description = "조회할 년월(YYYYMM)", example = "202505"),
			@Parameter(name = "goodsDataSourceNo", description = "데이터 소스 번호", example = "63")
	})
	@Operation(summary = "대시보드 차트 주요 요인 분석 카테고리 조회(Drill Down #2)", description = "대시보드 차트 주요 요인 분석 카테고리를 조회합니다.")
	@ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
	@GetMapping("/companies/{companyNo}/chart/factor/category")
	@RequiredPermission(roles = {Role.ADMIN, Role.USER, Role.API_PLUS_OPERATOR,
			Role.API_SUB_ADMIN}, condition = PermissionCondition.THIRD_PARTY_ADMIN_ONLY)
	public BaseResponse<DashboardChartFactorCategoryRes> getChartFactorCategory(
			@PathVariable("companyNo") Integer companyNo,
			@RequestParam(value = "predictDate", required = true) String predictDate,
			@RequestParam(value = "date", required = true) String date,
			@RequestParam(value = "goodsDataSourceNo", required = true) int goodsDataSourceNo) {
		return BaseResponseFactory.create(miDashboardService.getChartFactorCategory(companyNo, goodsDataSourceNo, predictDate, date));
	}

	@ApiOrder(11)
	@Parameters({
			@Parameter(name = "companyNo", description = "관리자: 선택 된 고객사 번호, 사용자: 자신이 속한 고객사 번호"),
			@Parameter(name = "predictDate", description = "예측 날짜", example = "20250511"),
			@Parameter(name = "date", description = "조회할 년월(YYYYMM)", example = "202505"),
			@Parameter(name = "goodsDataSourceNo", description = "데이터 소스 번호", example = "63"),
			@Parameter(name = "explanatoryLevelNm", description = "카테고리", example = "WEATHER")
	})
	@Operation(summary = "대시보드 차트 주요 요인 분석 조회(Drill Down #2)", description = "대시보드 차트 주요 요인 분석 데이터를 조회합니다.")
	@ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
	@GetMapping("/companies/{companyNo}/chart/factors")
	@RequiredPermission(roles = {Role.ADMIN, Role.USER, Role.API_PLUS_OPERATOR,
			Role.API_SUB_ADMIN}, condition = PermissionCondition.THIRD_PARTY_ADMIN_ONLY)
	public BaseResponse<DashboardChartFactorRes> getChartFactors(
			@PathVariable("companyNo") Integer companyNo,
			@RequestParam(value = "predictDate", required = true) String predictDate,
			@RequestParam(value = "date", required = true) String date,
			@RequestParam(value = "goodsDataSourceNo", required = true) int goodsDataSourceNo,
			@RequestParam(value = "explanatoryLevelNm", required = true) String explanatoryLevelNm) {
		return BaseResponseFactory.create(miDashboardService.getChartFactors(companyNo, goodsDataSourceNo, predictDate, date, explanatoryLevelNm));
	}
}
