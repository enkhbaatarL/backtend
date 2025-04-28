package com.sk.domain.mi.controller;

import com.sk.common.annotation.ApiOrder;
import com.sk.common.model.BaseResponse;
import com.sk.common.model.BaseResponseFactory;
import com.sk.common.model.PaginationRes;
import com.sk.common.model.TableRes;
import com.sk.domain.mi.dto.request.ChartsPersonalizeReq;
import com.sk.domain.mi.dto.response.charts.ChartBundleRes;
import com.sk.domain.mi.dto.response.charts.ChartsPersonalizeRes;
import com.sk.domain.mi.dto.response.detail.GoodsDataRes;
import com.sk.domain.mi.dto.response.detail.GoodsDataSourceRes;
import com.sk.domain.mi.dto.response.detail.UnstructuredDataRes;
import com.sk.domain.mi.service.MiDataDetailService;
import com.sk.domain.shared.enums.chart.Interval;
import com.sk.domain.shared.enums.chart.Range;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "05. 상품 데이터 상세 API", description = "상품 데이터 상세 API 입니다.")
@Slf4j
@RestController
@RequestMapping("/api/v1/mi/data-detail")
@RequiredArgsConstructor
public class MiDataDetailController {

    private final MiDataDetailService miDataDetailService;

    /**
     * todo 1. role에 따른 권한 같이 내릴 것
     * todo 2. 카테고리코드 상세 체크
     */
    @ApiOrder(1)
    @Parameters({
        @Parameter(name = "goodsNo", description = "상품 번호"),
        @Parameter(name = "goodsDataNo", description = "상품 데이터 번호")
    })
    @Operation(summary = "상품 데이터 조회 API", description = "상품 데이터를 조회합니다.")
    @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
    @GetMapping("/goods/{goodsNo}/goods-data/{goodsDataNo}")
    public BaseResponse<GoodsDataRes> getGoodsData(
        @PathVariable(value = "goodsNo") Integer goodsNo,
        @PathVariable(value = "goodsDataNo") Integer goodsDataNo
    ) {
        return BaseResponseFactory.create(miDataDetailService.getGoodsData(goodsNo, goodsDataNo));
    }

    /**
     * todo 1.어디서 어떻게 데이터 가져오는지 확인 할 것.
     */
    @ApiOrder(2)
    @Parameters({
        @Parameter(name = "goodsNo", description = "상품 번호"),
        @Parameter(name = "goodsDataNo", description = "상품 데이터 번호")
    })
    @Operation(summary = "상품 비정형 데이터 목록 조회 API", description = "상품 데이터 비정형데이터 목록을 조회합니다.")
    @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
    @GetMapping("/goods/{goodsNo}/goods-data/{goodsDataNo}/unstructured-data")
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
    public BaseResponse<GoodsDataSourceRes> getGoodsDataSources(
        @PathVariable(value = "goodsNo") Integer goodsNo,
        @PathVariable(value = "goodsDataNo") Integer goodsDataNo
    ) {
        return null;
    }

    @ApiOrder(4)
    @Parameters({
        @Parameter(name = "goodsNo", description = "상품 번호"),
        @Parameter(name = "goodsDataNo", description = "상품 데이터 번호"),
        @Parameter(name = "dataSourceNo", description = "상품 데이터소스 번호"),
        @Parameter(name = "chartsPersonalizeReqs", description = "차트 개인화 정보")
    })
    @Operation(summary = "데이터소스 차트 개인화 설정 API", description = "상품 데이터소스 차트를 개인 설정합니다.")
    @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
    @PutMapping("/goods/{goodsNo}/goods-data/{goodsDataNo}/goods-data-sources/{goodsDataSourceNo}/charts-personalize")
    public BaseResponse<List<ChartsPersonalizeRes>> putGoodsDataSourcesChartsPersonalize(
        @PathVariable(value = "goodsNo") Integer goodsNo,
        @PathVariable(value = "goodsDataNo") Integer goodsDataNo,
        @PathVariable(value = "goodsDataSourceNo") Integer goodsDataSourceNo,
        @RequestBody @Valid List<ChartsPersonalizeReq> chartsPersonalizeReqs
    ) {
        return BaseResponseFactory.create(miDataDetailService.putGoodsDataSourcesChartsPersonalize(goodsDataNo, goodsDataSourceNo, chartsPersonalizeReqs));
    }

    /**
     * todo 1. 쿼리 개선
     * todo 2. 인터벌 집계 로직 변경
     * todo 3. 파라미터 validation 체크 추가
     */
    @ApiOrder(5)
    @Parameters({
        @Parameter(name = "goodsNo", description = "상품 번호"),
        @Parameter(name = "goodsDataNo", description = "상품 데이터 번호"),
        @Parameter(name = "goodsDataSourceNo", description = "상품 데이터소스 번호"),
        @Parameter(name = "interval", description = "차트 인터벌 (1D, 1W, 2W, 1M, 3M, 6M, 1Y)", schema = @Schema(
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
    @Operation(summary = "상품 데이터소스 차트 조회 API", description = "상품 데이터소스 차트를 조회합니다. (상품 `VIEW`권한이 필요합니다.)")
    @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
    @GetMapping("/goods/{goodsNo}/goods-data/{goodsDataNo}/goods-data-sources/{goodsDataSourceNo}/chart")
    public BaseResponse<ChartBundleRes> getGoodsDataSourceCharts(
        @PathVariable(value = "goodsNo") Integer goodsNo,
        @PathVariable(value = "goodsDataNo") Integer goodsDataNo,
        @PathVariable(value = "goodsDataSourceNo") Integer goodsDataSourceNo,
        @RequestParam(value = "interval", required = false) Interval interval, // todo validation
        @RequestParam(value = "range", required = false) Range range, // todo validation
        @RequestParam(value = "startDate", required = false) String startDate, // todo validation
        @RequestParam(value = "endDate", required = false) String endDate, // todo validation
        @RequestParam(value = "unitCd", required = false) String unitCd,
        Authentication authentication
    ) {
        return BaseResponseFactory.create(miDataDetailService.getGoodsDataSourceCharts(goodsDataNo, goodsDataSourceNo, interval, range, startDate, endDate, unitCd, authentication.getName()));
    }

    /**
     * todo 1. myBatis 페이지네이션 ㅋㅋㅋㅋ
     */
    @ApiOrder(6)
    @Parameters({
        @Parameter(name = "goodsNo", description = "상품 번호"),
        @Parameter(name = "goodsDataNo", description = "상품 데이터 번호"),
        @Parameter(name = "goodsDataSourceNo", description = "상품 데이터소스 번호")
    })
    @Operation(summary = "상품 데이터소스 raw 데이터 조회 API (with pagination)", description = "페이지네이션을 통해 상품 데이터소스 raw 데이터를 조회합니다.")
    @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
    @GetMapping("/goods/{goodsNo}/goods-data/{goodsDataNo}/goods-data-sources/{goodsDataSourceNo}/raw-data")
    public BaseResponse<PaginationRes<TableRes>> getGoodsDataSourcesRawData(
        @PathVariable(value = "goodsNo") Integer goodsNo,
        @PathVariable(value = "goodsDataNo") Integer goodsDataNo,
        @PathVariable(value = "goodsDataSourceNo") Integer goodsDataSourceNo
    ) {
        return null;
    }

    // raw-data 테이블 엑셀 다운로드?

}
