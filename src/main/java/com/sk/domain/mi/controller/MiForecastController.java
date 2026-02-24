package com.sk.domain.mi.controller;

import com.sk.common.annotation.ApiOrder;
import com.sk.common.annotation.RequiredPermission;
import com.sk.common.model.BaseResponse;
import com.sk.common.model.BaseResponseFactory;
import com.sk.common.utils.DateUtil;
import com.sk.common.utils.RoleUtil;
import com.sk.domain.admin.dto.response.file.FileDownloadRes;
import com.sk.domain.mi.dto.response.forecast.chartFactor.ForecastChartFactorRes;
import com.sk.domain.mi.service.MiForecastService;
import com.sk.domain.shared.enums.PermissionCondition;
import com.sk.domain.shared.enums.Role;
import com.sk.domain.shared.service.permissions.AuthorizationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
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

@Profile("!dedicated")
@Tag(name = "18. AI Forecast API", description = "AI Forecast API 입니다.")
@RestController
@RequestMapping("/api/v1/mi/forecast")
@RequiredArgsConstructor
public class MiForecastController {

    private final AuthorizationService authorizationService;
    private final MiForecastService miForecastService;

    @ApiOrder(1)
    @Parameter(name = "companyNo", description = "관리자: 선택 된 고객사 번호, 사용자: 자신이 속한 고객사 번호", example = "2020")
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

    @ApiOrder(10)
    @Parameter(name = "companyNo", description = "관리자: 선택 된 고객사 번호, 사용자: 자신이 속한 고객사 번호", example = "2020")
    @Parameter(name = "predictDate", description = "예측 날짜", example = "20250825")
    @Parameter(name = "startDate", description = "조회 시작일자(YYYYMMDD, 선택)", example = "20250901")
    @Parameter(name = "goodsDataSourceNo", description = "예측 데이터 소스 번호", example = "4158")
    @Parameter(name = "explanatoryLevelNm", description = "카테고리", example = "COMMODITIES")
    @Operation(summary = "Forecast 차트 주요 요인 분석 조회",
        description = "Forecast 차트 주요 요인 분석 데이터를 조회합니다.")
    @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
    @GetMapping("/companies/{companyNo}/chart/factors")
    @RequiredPermission(roles = {Role.ADMIN, Role.USER, Role.API_PLUS_OPERATOR,
        Role.API_SUB_ADMIN}, condition = PermissionCondition.THIRD_PARTY_ADMIN_ONLY)
    public BaseResponse<ForecastChartFactorRes> getChartFactors(
        @PathVariable Integer companyNo,
        @RequestParam(value = "predictDate", required = true) String predictDate,
        @RequestParam(value = "startDate", required = false) String startDate,
        @RequestParam(value = "goodsDataSourceNo", required = true) int goodsDataSourceNo,
        @RequestParam(value = "explanatoryLevelNm", required = true) String explanatoryLevelNm) {
        return BaseResponseFactory.create(
            miForecastService.getChartFactors(companyNo, goodsDataSourceNo, predictDate, startDate,
                explanatoryLevelNm));
    }

    @ApiOrder(12)
    @Parameter(name = "companyNo", description = "관리자: 선택 된 고객사 번호, 사용자: 자신이 속한 고객사 번호", example = "2020")
    @Parameter(name = "predictDate", description = "예측 날짜(YYYYMMDD)", example = "20250511")
    @Operation(summary = "Forecast report 파일 다운로드", description = "Forecast report 파일을 다운로드합니다.")
    @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
    @GetMapping("/companies/{companyNo}/report/file-download")
    @RequiredPermission(roles = {Role.ADMIN, Role.USER, Role.API_PLUS_OPERATOR,
        Role.API_SUB_ADMIN}, condition = PermissionCondition.THIRD_PARTY_ADMIN_ONLY)
    public ResponseEntity<byte[]> getForecastReportFileDownload(
        @PathVariable Integer companyNo,
        @RequestParam(value = "predictDate") String predictDate) {

        String filePrefix = "forecast_report";
        String encodedFileName = URLEncoder.encode(
            filePrefix.concat("_").concat(predictDate).concat("_")
                .concat(DateUtil.currentYYYYMMDDHHMMSS()).concat(".zip"),
            StandardCharsets.UTF_8);
        FileDownloadRes fileDownloadRes = miForecastService.getForecastReportFileDownload(
            companyNo, predictDate, encodedFileName);

        ContentDisposition contentDisposition = ContentDisposition.attachment()
            .filename(fileDownloadRes.fileNm(), StandardCharsets.UTF_8)
            .build();

        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        httpHeaders.setContentLength(fileDownloadRes.fileSize());
        httpHeaders.add(HttpHeaders.CONTENT_DISPOSITION, contentDisposition.toString());

        return new ResponseEntity<>(fileDownloadRes.fileData(), httpHeaders, HttpStatus.OK);
    }
}
