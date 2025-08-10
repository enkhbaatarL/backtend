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
import java.io.ByteArrayInputStream;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.InputStreamResource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Profile("!dedicated")
@Tag(name = "05. 상품 데이터 상세 API", description = "상품 데이터 상세 API 입니다.")
@Slf4j
@RestController
@RequestMapping("/api/v1/mi/data-detail")
@RequiredArgsConstructor
public class MiDataDetailController {

    private final MiDataDetailService miDataDetailService;

    @ApiOrder(1)
    @Parameters({
        @Parameter(name = "goodsNo", description = "상품 번호"),
        @Parameter(name = "goodsDataNo", description = "상품 데이터 번호")
    })
    @Operation(
        summary = "상품 데이터 조회 API",
        description = """
            상품 데이터를 조회합니다. <br><br>
            `isViewable`: 상품에 대한 보기 권한 여부를 의미합니다. <br>
            `isDownloadable`: 상품에 대한 다운로드 권한 여부를 의미합니다. <br><br>
            해당 API 호출 시, 상품데이터 조회 내역이 내부적으로 적재되어 관리됩니다.
            """
    )
    @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
    @GetMapping("/goods/{goodsNo}/goods-data/{goodsDataNo}")
    @RequiredPermission(
        roles = {Role.ADMIN, Role.USER, Role.API_PLUS_OPERATOR},
        condition = PermissionCondition.THIRD_PARTY_ADMIN_ONLY,
        goodsNestedParam = "goodsDataNo", requireViewPermission = true
    )
    public BaseResponse<GoodsDataRes> getGoodsData(
        @PathVariable(value = "goodsNo") Integer goodsNo,
        @PathVariable(value = "goodsDataNo") Integer goodsDataNo
    ) {
        return BaseResponseFactory.create(miDataDetailService.getGoodsData(goodsNo, goodsDataNo));
    }

    @ApiOrder(2)
    @Parameters({
        @Parameter(name = "goodsNo", description = "상품 번호"),
        @Parameter(name = "goodsDataNo", description = "상품 데이터 번호")
    })
    @Operation(
        summary = "상품 비정형 데이터 목록 조회 API",
        description = """
            상품 데이터 비정형데이터 목록을 조회합니다. <br><br>
            `isPdfViewable`: 비정형데이터 파일의 PDF 뷰어 가능 여부를 의미합니다.
            """
    )
    @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
    @GetMapping("/goods/{goodsNo}/goods-data/{goodsDataNo}/unstructured-data")
    @RequiredPermission(
        roles = {Role.ADMIN, Role.USER, Role.API_PLUS_OPERATOR},
        condition = PermissionCondition.THIRD_PARTY_ADMIN_ONLY
    )
    public BaseResponse<List<UnstructuredDataRes>> getGoodsDataUnstructuredData(
        @PathVariable(value = "goodsNo") Integer goodsNo,
        @PathVariable(value = "goodsDataNo") Integer goodsDataNo
    ) {
        return BaseResponseFactory.create(miDataDetailService.getGoodsDataUnstructuredData(goodsNo, goodsDataNo));
    }

    @ApiOrder(3)
    @Parameters({
        @Parameter(name = "goodsNo", description = "상품 번호"),
        @Parameter(name = "goodsDataNo", description = "상품 데이터 번호")
    })
    @Operation(summary = "상품 정형 데이터(데이터소스) 목록 조회 API", description = "상품 정형 데이터(데이터소스) 목록을 조회합니다.")
    @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
    @GetMapping("/goods/{goodsNo}/goods-data/{goodsDataNo}/goods-data-sources")
    @RequiredPermission(
        roles = {Role.ADMIN, Role.USER, Role.API_PLUS_OPERATOR},
        condition = PermissionCondition.THIRD_PARTY_ADMIN_ONLY
    )
    public BaseResponse<List<GoodsDataSourceRes>> getGoodsDataSources(
        @PathVariable(value = "goodsNo") Integer goodsNo,
        @PathVariable(value = "goodsDataNo") Integer goodsDataNo
    ) {
        return BaseResponseFactory.create(miDataDetailService.getGoodsDataSources(goodsNo, goodsDataNo));
    }

    @ApiOrder(4)
    @Parameters({
        @Parameter(name = "goodsNo", description = "상품 번호"),
        @Parameter(name = "goodsDataNo", description = "상품 데이터 번호"),
        @Parameter(name = "goodsDataSourceNo", description = "상품 데이터소스 번호"),
    })
    @Operation(
        summary = "상품 정형 데이터(데이터소스) 차트 개인화 설정 API",
        description = """
            상품 정형 데이터(데이터소스) 차트 개인화 설정을 합니다. <br><br>

            개인화 차트 조건 타입과 허용 값 정의 <br>
             - RANGE: 차트 범위(1W, 1M, 3M, 6M, 1Y, 2Y) / CUSTOM은 개인화 대상이 아님 <br>
             - INTERVAL: 차트 주기(1D, 1W, 2W, 1M, 3M, 6M, 1Y) <br>
             - INDICATOR: 보조 지표(BB, MA_5, MA_20, MA_60, MA_120), visible 처리 / 복수 값 허용 <br>
             - CHART: 차트 유형(CANDLE, LINE) <br>
             - UNIT_CD: 변환 단위 코드 <br>
             - COMP_DATA_SOURCE: 상품 데이터소스 번호, visible 처리 / 복수 값 허용 <br><br>

            차트 개인화 설정은 항상 <b>전체 설정 값</b>을 리스트로 전달해주어야 합니다. (요청 시, 개인화 리소스 전체 교체)
            """
    )
    @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
    @PutMapping("/goods/{goodsNo}/goods-data/{goodsDataNo}/goods-data-sources/{goodsDataSourceNo}/charts-personalize")
    @RequiredPermission(
        roles = {Role.ADMIN, Role.USER},
        condition = PermissionCondition.THIRD_PARTY_ADMIN_ONLY
    )
    public BaseResponse<List<ChartsPersonalizeRes>> putGoodsDataSourcesChartsPersonalize(
        @PathVariable(value = "goodsNo") Integer goodsNo,
        @PathVariable(value = "goodsDataNo") Integer goodsDataNo,
        @PathVariable(value = "goodsDataSourceNo") Integer goodsDataSourceNo,
        @RequestBody @Valid List<ChartsPersonalizeReq> chartsPersonalizeReqs
    ) {
        return BaseResponseFactory.create(miDataDetailService.putGoodsDataSourcesChartsPersonalize(goodsNo, goodsDataNo, goodsDataSourceNo, chartsPersonalizeReqs));
    }

    @ApiOrder(5)
    @Parameters({
        @Parameter(name = "goodsNo", description = "상품 번호"),
        @Parameter(name = "goodsDataNo", description = "상품 데이터 번호"),
        @Parameter(name = "goodsDataSourceNo", description = "상품 데이터소스 번호"),
    })
    @Operation(
        summary = "상품 정형 데이터(데이터소스) 차트 메타데이터 조회 API",
        description = """
            상품 정형 데이터(데이터소스) 차트의 메타데이터를 조회합니다. <br>
            상품 `VIEW`권한이 필요합니다.
            """
    )
    @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
    @GetMapping("/goods/{goodsNo}/goods-data/{goodsDataNo}/goods-data-sources/{goodsDataSourceNo}/metadata")
    @RequiredPermission(
        roles = {Role.ADMIN, Role.USER, Role.API_PLUS_OPERATOR},
        condition = PermissionCondition.THIRD_PARTY_ADMIN_ONLY,
        goodsNestedParam = "goodsDataSourceNo", requireViewPermission = true
    )
    public BaseResponse<ChartMetadataSimple> getGoodsDataSourceMetaData(
        @PathVariable(value = "goodsNo") Integer goodsNo,
        @PathVariable(value = "goodsDataNo") Integer goodsDataNo,
        @PathVariable(value = "goodsDataSourceNo") Integer goodsDataSourceNo
    ) {
        return BaseResponseFactory.create(miDataDetailService.getGoodsDataSourceMetadata(goodsNo, goodsDataNo, goodsDataSourceNo));
    }

}
