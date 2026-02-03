package com.sk.domain.mi.controller;

import com.sk.common.annotation.ApiOrder;
import com.sk.common.annotation.RequiredPermission;
import com.sk.common.model.BaseResponse;
import com.sk.common.model.BaseResponseFactory;
import com.sk.common.model.PaginationRes;
import com.sk.common.utils.DateUtil;
import com.sk.domain.mi.dto.response.forecast.ForecastChartFactorGraphRes;
import com.sk.domain.mi.dto.response.forecast.ForecastChartRes;
import com.sk.domain.mi.dto.response.forecast.ForecastInfoRes;
import com.sk.domain.mi.dto.response.forecast.summary.ForecastRecordRes;
import com.sk.domain.mi.dto.response.forecast.summary.ForecastReportDetailRes;
import com.sk.domain.mi.dto.response.forecast.summary.ForecastReportRes;
import com.sk.domain.mi.dto.response.forecast.table.ForecastTableRes;
import com.sk.domain.mi.service.MiForecastService;
import com.sk.domain.shared.enums.PermissionCondition;
import com.sk.domain.shared.enums.Role;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Min;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
    @Parameter(name = "companyNo", description = "관리자: 선택 된 고객사 번호, 사용자: 자신이 속한 고객사 번호", example = "2020")
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
    @Parameter(name = "companyNo", description = "관리자: 선택 된 고객사 번호, 사용자: 자신이 속한 고객사 번호", example = "2020")
    @Parameter(name = "xAxisUnit", description = "x축 단위(1D, 1W, 1M, 1Q, 1Y)", example = "1W")
    @Parameter(name = "period", description = "전체기간(1W, 1M, 3M, 6M, YTD, 1Y, ALL)", example = "3M")
    @Operation(summary = "Forecast 차트 데이터 조회", description = "Forecast 차트 데이터를 조회합니다.")
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
    @Parameter(name = "companyNo", description = "관리자: 선택 된 고객사 번호, 사용자: 자신이 속한 고객사 번호", example = "2020")
    @Parameter(name = "goodsDataSourceNo", description = "데이터 소스 번호(선택)", example = "934")
    @Parameter(name = "startDate", description = "조회 시작일자(YYYYMMDD)", example = "20250301")
    @Parameter(name = "endDate", description = "조회 종료일자(YYYYMMDD)", example = "20260201")
    @Parameter(name = "order", description = "정렬 순서(NEWEST/OLDEST)")
    @Parameter(name = "page", description = "페이지번호 (기본값: 1)")
    @Parameter(name = "size", description = "페이지당항목수 (기본값: 20)")
    @Operation(summary = "Forecast record 목록 조회", description = "Forecast record 목록을 조회합니다.")
    @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
    @GetMapping("/companies/{companyNo}/records")
    @RequiredPermission(roles = {Role.ADMIN, Role.USER, Role.API_PLUS_OPERATOR,
        Role.API_SUB_ADMIN}, condition = PermissionCondition.THIRD_PARTY_ADMIN_ONLY)
    public BaseResponse<PaginationRes<ForecastRecordRes>> getForecastRecords(
        @PathVariable Integer companyNo,
        @RequestParam(required = false) Integer goodsDataSourceNo,
        @RequestParam String startDate,
        @RequestParam String endDate,
        @RequestParam(defaultValue = "NEWEST") String order,
        @RequestParam(value = "page", required = false, defaultValue = "1") @Min(value = 1) int page,
        @RequestParam(value = "size", required = false, defaultValue = "20") @Min(value = 20) int size) {
        return BaseResponseFactory.create(
            miForecastService.getForecastRecords(companyNo, goodsDataSourceNo, startDate, endDate,
                order, page, size));
    }

    @ApiOrder(6)
    @Parameter(name = "companyNo", description = "관리자: 선택 된 고객사 번호, 사용자: 자신이 속한 고객사 번호", example = "2020")
    @Parameter(name = "marketIndexAnalysisTargetCd", description = "시장지수분석대상코드", example = "EN_CROIL")
    @Parameter(name = "predictionFulfillDt", description = "예측수행일자", example = "20250825")
    @Parameter(name = "mnemonicCd", description = "MNEMONIC코드", example = "D-PCAAT00")
    @Parameter(name = "predictionCriteriaDt", description = "예측기준일자", example = "20250822")
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
        return BaseResponseFactory.create(
            miForecastService.getForecastReportDetail(companyNo, marketIndexAnalysisTargetCd,
                predictionFulfillDt, mnemonicCd, predictionCriteriaDt));
    }

    @ApiOrder(7)
    @Parameter(name = "companyNo", description = "관리자: 선택 된 고객사 번호, 사용자: 자신이 속한 고객사 번호", example = "2020")
    @Parameter(name = "goodsDataSourceNo", description = "데이터 소스 번호(선택)", example = "4158")
    @Parameter(name = "startDate", description = "조회 시작일자(YYYYMMDD)")
    @Parameter(name = "endDate", description = "조회 종료일자(YYYYMMDD)")
    @Parameter(name = "order", description = "정렬 순서(NEWEST/OLDEST)")
    @Parameter(name = "page", description = "페이지번호 (기본값: 1)")
    @Parameter(name = "size", description = "페이지당항목수 (기본값: 20)")
    @Operation(summary = "Forecast 테이블 조회", description = "Forecast 테이블 데이터를 조회합니다.")
    @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
    @GetMapping("/companies/{companyNo}/table")
    @RequiredPermission(roles = {Role.ADMIN, Role.USER, Role.API_PLUS_OPERATOR,
        Role.API_SUB_ADMIN}, condition = PermissionCondition.THIRD_PARTY_ADMIN_ONLY)
    public BaseResponse<ForecastTableRes> getForecastTable(
        @PathVariable Integer companyNo,
        @RequestParam(required = false) Integer goodsDataSourceNo,
        @RequestParam String startDate,
        @RequestParam String endDate,
        @RequestParam(defaultValue = "NEWEST") String order,
        @RequestParam(value = "page", required = false, defaultValue = "1") @Min(value = 1) int page,
        @RequestParam(value = "size", required = false, defaultValue = "20") @Min(value = 20) int size) {
        return BaseResponseFactory.create(
            miForecastService.getForecastTable(companyNo, goodsDataSourceNo, startDate, endDate,
                order, page, size));
    }

    @ApiOrder(8)
    @Parameter(name = "companyNo", description = "관리자: 선택 된 고객사 번호, 사용자: 자신이 속한 고객사 번호", example = "2020")
    @Parameter(name = "goodsDataSourceNo", description = "데이터 소스 번호(선택)", example = "4158")
    @Parameter(name = "startDate", description = "조회 시작일자(YYYYMMDD)")
    @Parameter(name = "endDate", description = "조회 종료일자(YYYYMMDD)")
    @Parameter(name = "order", description = "정렬 순서(NEWEST/OLDEST)")
    @Operation(summary = "Forecast 테이블 다운로드", description = "Forecast 테이블 데이터를 엑셀로 다운로드합니다.")
    @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
    @GetMapping("/companies/{companyNo}/table/file-download")
    @RequiredPermission(roles = {Role.ADMIN, Role.USER, Role.API_PLUS_OPERATOR,
        Role.API_SUB_ADMIN}, condition = PermissionCondition.THIRD_PARTY_ADMIN_ONLY)
    public ResponseEntity<byte[]> getForecastTableFileDownload(
        @PathVariable Integer companyNo,
        @RequestParam(required = false) Integer goodsDataSourceNo,
        @RequestParam String startDate,
        @RequestParam String endDate,
        @RequestParam(defaultValue = "NEWEST") String order) throws IOException {
        byte[] fileData = miForecastService.getForecastTableFileDownload(
            companyNo, goodsDataSourceNo, startDate, endDate, order);

        String filePrefix = "forecast_table";
        String encodedFileName = URLEncoder.encode(
            filePrefix.concat("_").concat(startDate).concat("_").concat(endDate)
                .concat("_").concat(DateUtil.currentYYYYMMDDHHMMSS()).concat(".xlsx"),
            StandardCharsets.UTF_8);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentLength(fileData.length);
        headers.add("Content-Type",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        headers.add("Content-Disposition", "attachment; filename*=UTF-8''" + encodedFileName);

        return new ResponseEntity<>(fileData, headers, HttpStatus.OK);
    }

    @ApiOrder(9)
    @Parameter(name = "companyNo", description = "관리자: 선택 된 고객사 번호, 사용자: 자신이 속한 고객사 번호", example = "2020")
    @Parameter(name = "goodsDataSourceNo", description = "예측 데이터 소스 번호", example = "4158")
    @Parameter(name = "predictDate", description = "예측 날짜", example = "20250825")
    @Parameter(name = "date", description = "조회할 년월(YYYYMM)", example = "202509")
    @Parameter(name = "explanatoryLevelNm", description = "카테고리", example = "COMMODITIES")
    @Parameter(name = "predictionInputMnemonicCd", description = "예측 입력 명칭 코드", example = "CLc1")
    @Parameter(name = "marketIndexAnalysisTargetCd", description = "시장지수분석대상코드", example = "EN_CROIL")
    @Parameter(name = "predictionInputMnemonicDetailCd", description = "예측 입력 명칭 상세코드", example = "CLc1")
    @Operation(summary = "Forecast 차트 주요 요인 분석 라인 그래프",
        description = "Forecast 차트 주요 요인 분석 라인 그래프를 조회합니다.")
    @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
    @GetMapping("/companies/{companyNo}/chart/factor/graph")
    @RequiredPermission(roles = {Role.ADMIN, Role.USER, Role.API_PLUS_OPERATOR,
        Role.API_SUB_ADMIN}, condition = PermissionCondition.THIRD_PARTY_ADMIN_ONLY)
    public BaseResponse<ForecastChartFactorGraphRes> getChartFactorGraph(
        @PathVariable Integer companyNo,
        @RequestParam(value = "goodsDataSourceNo") int goodsDataSourceNo,
        @RequestParam(value = "predictDate") String predictDate,
        @RequestParam(value = "date", required = false) String date,
        @RequestParam(value = "explanatoryLevelNm") String explanatoryLevelNm,
        @RequestParam(value = "predictionInputMnemonicCd") String predictionInputMnemonicCd,
        @RequestParam(value = "marketIndexAnalysisTargetCd") String marketIndexAnalysisTargetCd,
        @RequestParam(value = "predictionInputMnemonicDetailCd") String predictionInputMnemonicDetailCd) {
        return BaseResponseFactory.create(
            miForecastService.getChartFactorGraph(companyNo, goodsDataSourceNo,
                predictDate, date, explanatoryLevelNm, predictionInputMnemonicCd,
                marketIndexAnalysisTargetCd,
                predictionInputMnemonicDetailCd));
    }

    @ApiOrder(11)
    @Parameter(name = "companyNo", description = "관리자: 선택 된 고객사 번호, 사용자: 자신이 속한 고객사 번호", example = "2020")
    @Parameter(name = "goodsDataSourceNo", description = "데이터 소스 번호(선택)", example = "934")
    @Operation(summary = "Forecast report 목록 조회", description = "Forecast report 목록을 조회합니다.")
    @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
    @GetMapping("/companies/{companyNo}/reports")
    @RequiredPermission(roles = {Role.ADMIN, Role.USER, Role.API_PLUS_OPERATOR,
        Role.API_SUB_ADMIN}, condition = PermissionCondition.THIRD_PARTY_ADMIN_ONLY)
    public BaseResponse<List<ForecastReportRes>> getForecastReports(
        @PathVariable Integer companyNo,
        @RequestParam(required = false) Integer goodsDataSourceNo) {
        return BaseResponseFactory.create(
            miForecastService.getForecastReports(companyNo, goodsDataSourceNo));
    }

}
