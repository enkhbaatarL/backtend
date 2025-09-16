package com.sk.domain.mi.controller;

import com.sk.common.annotation.ApiOrder;
import com.sk.common.annotation.RequiredPermission;
import com.sk.common.model.BaseResponse;
import com.sk.common.model.BaseResponseFactory;
import com.sk.common.model.PaginationTableRes;
import com.sk.common.model.TableRes;
import com.sk.common.utils.DateUtil;
import com.sk.domain.mi.dto.request.ChartsPersonalizeReq;
import com.sk.domain.mi.dto.response.charts.ChartBundleRes;
import com.sk.domain.mi.dto.response.charts.ChartMetadataSimple;
import com.sk.domain.mi.dto.response.charts.ChartsPersonalizeRes;
import com.sk.domain.mi.dto.response.detail.GoodsDataRes;
import com.sk.domain.mi.dto.response.detail.GoodsDataSourceRes;
import com.sk.domain.mi.dto.response.detail.UnstructuredDataRes;
import com.sk.domain.mi.service.MiDataDetailService;
import com.sk.domain.shared.enums.PermissionCondition;
import com.sk.domain.shared.enums.Role;
import com.sk.domain.shared.enums.chart.Interval;
import com.sk.domain.shared.enums.chart.Range;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.InputStreamResource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayInputStream;
import java.time.LocalDate;
import java.util.List;

@Tag(name = "05. 상품 데이터 상세 API", description = "상품 데이터 상세 API 입니다.")
@Slf4j
@RestController
@RequestMapping("/api/v1/mi/data-detail")
@RequiredArgsConstructor
public class MiDataDetailDedicatedController {

    private final MiDataDetailService miDataDetailService;

    @ApiOrder(6)
    @Parameters({
        @Parameter(name = "goodsNo", description = "상품 번호"),
        @Parameter(name = "goodsDataNo", description = "상품 데이터 번호"),
        @Parameter(name = "goodsDataSourceNo", description = "상품 데이터소스 번호"),
        @Parameter(name = "interval", description = "차트 주기 (1D, 1W, 2W, 1M, 3M, 6M, 1Y)", schema = @Schema(
            type = "string",
            allowableValues = {
                "1D", "1W", "2W", "1M", "3M", "6M", "1Y"
            }
        )),
        @Parameter(name = "range", description = "차트 범위 (1W, 1M, 3M, 6M, 1Y, 2Y, CUSTOM)", schema = @Schema(
            type = "string",
            allowableValues = {
                "1W", "1M", "3M", "6M", "1Y", "2Y", "CUSTOM"
            }
        )),
        @Parameter(name = "startDate", description = "조회 시작일 (range가 'CUSTOM' 일 경우 필수)"),
        @Parameter(name = "endDate", description = "조회 종료일 (range가 'CUSTOM' 일 경우 필수)"),
        @Parameter(name = "unitCd", description = "변환 단위 코드"),
    })
    @Operation(
        summary = "상품 정형 데이터(데이터소스) 차트 조회 API",
        description = """
            상품 정형 데이터(데이터소스) 차트를 조회합니다. <br>
            상품 `VIEW`권한이 필요합니다.
            """
    )
    @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
    @GetMapping("/goods/{goodsNo}/goods-data/{goodsDataNo}/goods-data-sources/{goodsDataSourceNo}/chart")
    @RequiredPermission(
        roles = {Role.ADMIN, Role.USER, Role.API_PLUS_OPERATOR},
        condition = PermissionCondition.THIRD_PARTY_ADMIN_ONLY,
        goodsNestedParam = "goodsDataSourceNo", requireViewPermission = true
    )
    public BaseResponse<ChartBundleRes> getGoodsDataSourceCharts(
        @PathVariable(value = "goodsNo") Integer goodsNo,
        @PathVariable(value = "goodsDataNo") Integer goodsDataNo,
        @PathVariable(value = "goodsDataSourceNo") Integer goodsDataSourceNo,
        @RequestParam(value = "interval", required = false) Interval interval,
        @RequestParam(value = "range", required = false) Range range,
        @RequestParam(value = "startDate", required = false) @DateTimeFormat(pattern = "yyyyMMdd") LocalDate startDate,
        @RequestParam(value = "endDate", required = false) @DateTimeFormat(pattern = "yyyyMMdd") LocalDate endDate,
        @RequestParam(value = "unitCd", required = false) String unitCd
    ) {

        ChartBundleRes chartBundleRes = miDataDetailService.getGoodsDataSourceCharts(goodsNo, goodsDataNo, goodsDataSourceNo, interval, range, startDate, endDate, unitCd);

        return BaseResponseFactory.create(chartBundleRes);
    }

    @ApiOrder(7)
    @Parameters({
        @Parameter(name = "goodsNo", description = "상품 번호"),
        @Parameter(name = "goodsDataNo", description = "상품 데이터 번호"),
        @Parameter(name = "goodsDataSourceNo", description = "상품 데이터소스 번호"),
        @Parameter(name = "page", description = "페이지 번호 (default: 1)"),
        @Parameter(name = "size", description = "페이지당 항목 수 (default: 10)"),
        @Parameter(name = "startDate", description = "조회 시작일 (format: YYYYMMDD)"),
        @Parameter(name = "endDate", description = "조회 종료일 (format: YYYYMMDD)")
    })
    @Operation(
        summary = "상품 정형 데이터(데이터소스) raw 데이터 조회 API (with pagination)",
        description = """
            페이지네이션을 통해 상품 정형 데이터(데이터소스) raw 데이터를 조회합니다. <br>
            상품 `VIEW`권한이 필요합니다.
            """
    )
    @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
    @GetMapping("/goods/{goodsNo}/goods-data/{goodsDataNo}/goods-data-sources/{goodsDataSourceNo}/raw-data")
    @RequiredPermission(
        roles = {Role.ADMIN, Role.USER, Role.API_PLUS_OPERATOR},
        condition = PermissionCondition.THIRD_PARTY_ADMIN_ONLY,
        goodsNestedParam = "goodsDataSourceNo", requireViewPermission = true
    )
    public BaseResponse<PaginationTableRes<TableRes.Row, TableRes.Column>> getGoodsDataSourcesRawData(
        @PathVariable(value = "goodsNo") Integer goodsNo,
        @PathVariable(value = "goodsDataNo") Integer goodsDataNo,
        @PathVariable(value = "goodsDataSourceNo") Integer goodsDataSourceNo,
        @RequestParam(value = "page", required = false, defaultValue = "1") @Min(value = 1) int page,
        @RequestParam(value = "size", required = false, defaultValue = "10") @Min(value = 1) int size,
        @RequestParam(value = "startDate") @DateTimeFormat(pattern = "yyyyMMdd") LocalDate startDate,
        @RequestParam(value = "endDate") @DateTimeFormat(pattern = "yyyyMMdd") LocalDate endDate
    ) {
        return BaseResponseFactory.create(miDataDetailService.getGoodsDataSourcesRawData(goodsNo, goodsDataNo, goodsDataSourceNo, page, size, startDate, endDate));
    }

    @ApiOrder(8)
    @Parameters({
        @Parameter(name = "goodsNo", description = "상품 번호"),
        @Parameter(name = "goodsDataNo", description = "상품 데이터 번호"),
        @Parameter(name = "goodsDataSourceNo", description = "상품 데이터소스 번호"),
        @Parameter(name = "startDate", description = "조회 시작일 (format: YYYYMMDD)"),
        @Parameter(name = "endDate", description = "조회 종료일 (format: YYYYMMDD)")
    })
    @Operation(
        summary = "상품 정형 데이터(데이터소스) raw 데이터 다운로드 API",
        description = """
            상품 정형 데이터(데이터소스) raw 데이터를 다운로드 합니다. <br>
            상품 `DOWNLOAD`권한이 필요합니다.
            """
    )
    @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
    @GetMapping("/goods/{goodsNo}/goods-data/{goodsDataNo}/goods-data-sources/{goodsDataSourceNo}/raw-data/download")
    @RequiredPermission(
        roles = {Role.ADMIN, Role.USER, Role.API_PLUS_OPERATOR},
        condition = PermissionCondition.THIRD_PARTY_ADMIN_ONLY,
        goodsNestedParam = "goodsDataSourceNo", requireDownloadPermission = true
    )
    public ResponseEntity<InputStreamResource> downloadGoodsDataSourcesRawData(
        @PathVariable(value = "goodsNo") Integer goodsNo,
        @PathVariable(value = "goodsDataNo") Integer goodsDataNo,
        @PathVariable(value = "goodsDataSourceNo") Integer goodsDataSourceNo,
        @RequestParam(value = "startDate") @DateTimeFormat(pattern = "yyyyMMdd") LocalDate startDate,
        @RequestParam(value = "endDate") @DateTimeFormat(pattern = "yyyyMMdd") LocalDate endDate
    ) {
        HttpHeaders headers = new HttpHeaders();

        String title = "raw_data_%s.xlsx".formatted(DateUtil.currentYmdHms());
        headers.setContentDisposition(ContentDisposition.attachment().filename(title).build());

        ByteArrayInputStream in = miDataDetailService.downloadGoodsDataSourcesRawData(goodsNo, goodsDataNo, goodsDataSourceNo, startDate, endDate);

        return ResponseEntity.ok()
            .headers(headers)
            .contentType(MediaType.APPLICATION_OCTET_STREAM)
            .body(new InputStreamResource(in));
    }

}
