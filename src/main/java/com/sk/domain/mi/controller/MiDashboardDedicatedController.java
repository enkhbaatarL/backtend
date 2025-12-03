package com.sk.domain.mi.controller;

import com.sk.common.annotation.ApiOrder;
import com.sk.common.annotation.RequiredPermission;
import com.sk.common.model.BaseResponse;
import com.sk.common.model.BaseResponseFactory;
import com.sk.common.utils.DateUtil;
import com.sk.domain.mi.dto.response.dashboard.DashboardChartFactorGraphRes;
import com.sk.domain.mi.dto.response.dashboard.DashboardInfoRes;
import com.sk.domain.mi.dto.response.dashboard.charts.DashboardChartDateRes;
import com.sk.domain.mi.dto.response.dashboard.charts.DashboardChartRes;
import com.sk.domain.mi.dto.response.marketData.charts.MiMarketDataSummaryRes;
import com.sk.domain.mi.service.MiDashboardService;
import com.sk.domain.mi.service.MiMarketDataService;
import com.sk.domain.shared.enums.PermissionCondition;
import com.sk.domain.shared.enums.Role;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "01-1. AI Forecast (구 Dashboard) API", description = "AI Forecast (구 Dashboard) API 입니다.")
@Slf4j
@RestController
@RequestMapping("/api/v1/mi/dashboard")
@RequiredArgsConstructor
public class MiDashboardDedicatedController {
	private final MiDashboardService miDashboardService;
    private final MiMarketDataService miMarketDataService;


    @ApiOrder(2)
	@Parameters({
			@Parameter(name = "companyNo", description = "관리자: 선택 된 고객사 번호, 사용자: 자신이 속한 고객사 번호")
	})
	@Operation(summary = "대시보드 기본 정보 조회", description = "대시보드 기본 정보를 조회합니다.")
	@ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
	@GetMapping("/companies/{companyNo}/info")
	@RequiredPermission(roles = {Role.ADMIN, Role.USER, Role.API_PLUS_OPERATOR,
			Role.API_SUB_ADMIN}, condition = PermissionCondition.THIRD_PARTY_ADMIN_ONLY)
	public BaseResponse<DashboardInfoRes> getDashboardInfo(
			@PathVariable("companyNo") Integer companyNo) {
		return BaseResponseFactory.create(miDashboardService.getDashboardInfo(companyNo));
	}


    @ApiOrder(3)
    @Operation(summary = "DashBoard 화면에서 쓰이는 Market Data 목록 조회", description = "DashBoard 화면에서 쓰이는 Market Data 목록을 조회합니다.")
    @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
    @GetMapping("/companies/{companyNo}/market-datas")
    @RequiredPermission(roles = {Role.ADMIN, Role.USER, Role.API_PLUS_OPERATOR,
        Role.API_SUB_ADMIN}, condition = PermissionCondition.THIRD_PARTY_ADMIN_ONLY)
    public BaseResponse<List<MiMarketDataSummaryRes>> getMarketDataSummary(
        @PathVariable("companyNo") Integer companyNo) {
        return BaseResponseFactory.create(miMarketDataService.getMarketDataSummary(companyNo));
    }

	@ApiOrder(5)
	@Parameters({
			@Parameter(name = "companyNo", description = "관리자: 선택 된 고객사 번호, 사용자: 자신이 속한 고객사 번호"),
	})
	@Operation(summary = "대시보드 차트 조회 가능한 날짜 리스트", description = "대시보드 차트 조회 가능한 날짜 리스트를 조회합니다.")
	@ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
	@GetMapping("/companies/{companyNo}/chart-dates")
	@RequiredPermission(roles = {Role.ADMIN, Role.USER, Role.API_PLUS_OPERATOR,
			Role.API_SUB_ADMIN}, condition = PermissionCondition.THIRD_PARTY_ADMIN_ONLY)
	public BaseResponse<DashboardChartDateRes> getChartDates(
			@PathVariable("companyNo") Integer companyNo) {
		return BaseResponseFactory.create(miDashboardService.getChartDates(companyNo));
	}

	@ApiOrder(8)
	@Parameters({
			@Parameter(name = "companyNo", description = "관리자: 선택 된 고객사 번호, 사용자: 자신이 속한 고객사 번호"),
			@Parameter(name = "date", description = "조회할 년월(YYYYMM)", example = "202505")
	})
	@Operation(summary = "대시보드 차트 데이터 조회(Drill Down #1)", description = "대시보드 차트 데이터를 조회합니다.")
	@ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
	@GetMapping("/companies/{companyNo}/chart")
	@RequiredPermission(roles = {Role.ADMIN, Role.USER, Role.API_PLUS_OPERATOR,
			Role.API_SUB_ADMIN}, condition = PermissionCondition.THIRD_PARTY_ADMIN_ONLY)
	public BaseResponse<DashboardChartRes> getChart(
			@PathVariable("companyNo") Integer companyNo,
			@RequestParam(value = "date", required = false) String date) {
		return BaseResponseFactory.create(miDashboardService.getChart(companyNo, date));
	}

	@ApiOrder(9)
	@Parameters({
			@Parameter(name = "companyNo", description = "관리자: 선택 된 고객사 번호, 사용자: 자신이 속한 고객사 번호"),
			@Parameter(name = "date", description = "조회할 년월", example = "202505")
	})
	@Operation(summary = "대시보드 차트 데이터 다운로드(Drill Down #1)", description = "대시보드 차트 데이터를 다운로드합니다.")
	@ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
	@GetMapping("/companies/{companyNo}/chart/file-download")
	@RequiredPermission(roles = {Role.ADMIN, Role.USER, Role.API_PLUS_OPERATOR,
			Role.API_SUB_ADMIN}, condition = PermissionCondition.THIRD_PARTY_ADMIN_ONLY)
	public ResponseEntity<byte[]> getChartFileDownload(
			@PathVariable("companyNo") Integer companyNo,
			@RequestParam(value = "date", required = false) String date) throws IOException {
		byte[] fileData = miDashboardService.getChartFileDownload(companyNo, date);

		String FILE_PREFIX = "dashboard_data_table";
		String encodedFileName = URLEncoder.encode(
				FILE_PREFIX.concat("_").concat(date).concat("_").concat(DateUtil.currentYYYYMMDDHHMMSS()).concat(".xlsx"),
				StandardCharsets.UTF_8.toString());

		HttpHeaders headers = new HttpHeaders();
		headers.setContentLength(fileData.length);
		headers.add("Content-Type", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
		headers.add("Content-Disposition", "attachment; filename*=UTF-8''" + encodedFileName);

		return new ResponseEntity<>(fileData, headers, HttpStatus.OK);
	}


	@ApiOrder(12)
	@Parameters({
			@Parameter(name = "companyNo", description = "관리자: 선택 된 고객사 번호, 사용자: 자신이 속한 고객사 번호"),
			@Parameter(name = "goodsDataSourceNo", description = "예측 데이터 소스 번호", example = "63"),
			@Parameter(name = "predictDate", description = "예측 날짜", example = "20250511"),
			@Parameter(name = "date", description = "조회할 년월(YYYYMM)", example = "202505"),
			@Parameter(name = "explanatoryLevelNm", description = "카테고리", example = "WEATHER"),
			@Parameter(name = "predictionInputMnemonicCd", description = "예측 입력 명칭 코드", example = "EIAGS48"),
			@Parameter(name = "marketIndexAnalysisTargetCd", description = "시장지수분석대상코드", example = "EN_LNG"),
			@Parameter(name = "predictionInputMnemonicDetailCd", description = "예측 입력 명칭 상세코드", example = "EIAGS48.01")
	})
	@Operation(summary = "대시보드 차트 주요 요인 분석 라인 그래프(Drill Down #3)", description = "대시보드 차트 주요 요인 분석 라인 그래프를 조회합니다.")
	@ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
	@GetMapping("/companies/{companyNo}/chart/factor/graph")
	@RequiredPermission(roles = {Role.ADMIN, Role.USER, Role.API_PLUS_OPERATOR,
			Role.API_SUB_ADMIN}, condition = PermissionCondition.THIRD_PARTY_ADMIN_ONLY)
	public BaseResponse<DashboardChartFactorGraphRes> getChartFactorGraph(
			@PathVariable("companyNo") Integer companyNo,
			@RequestParam(value = "goodsDataSourceNo", required = true) int goodsDataSourceNo,
			@RequestParam(value = "predictDate", required = true) String predictDate,
			@RequestParam(value = "date", required = true) String date,
			@RequestParam(value = "explanatoryLevelNm", required = true) String explanatoryLevelNm,
			@RequestParam(value = "predictionInputMnemonicCd", required = true) String predictionInputMnemonicCd,
			@RequestParam(value = "marketIndexAnalysisTargetCd", required = true) String marketIndexAnalysisTargetCd,
			@RequestParam(value = "predictionInputMnemonicDetailCd", required = true) String predictionInputMnemonicDetailCd) {
		return BaseResponseFactory.create(miDashboardService.getChartFactorGraph(companyNo, goodsDataSourceNo, predictDate, date,
				explanatoryLevelNm, predictionInputMnemonicCd, marketIndexAnalysisTargetCd, predictionInputMnemonicDetailCd));
	}
}
