package com.sk.domain.mi.controller;

import com.sk.common.annotation.ApiOrder;
import com.sk.common.model.BaseResponse;
import com.sk.common.model.BaseResponseFactory;
import com.sk.domain.mi.dto.request.ChartsPersonalizeReq;
import com.sk.domain.mi.dto.response.charts.ChartBundleRes;
import com.sk.domain.mi.dto.response.chart.ChartsPersonalizeRes;
import com.sk.domain.mi.service.MiDataDetailService;
import com.sk.domain.shared.enums.chart.Interval;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "05. 상품 데이터 상세 API", description = "상품 데이터 상세 API 입니다.")
@Slf4j
@RestController
@RequestMapping("/api/v1/mi/data-detail")
@RequiredArgsConstructor
public class MiDataDetailController {

    private final MiDataDetailService miDataDetailService;

//    @ApiOrder(1)
//    @Parameters({
//        @Parameter(name = "goodsDataNo", description = "상품 데이터 번호"),
//    })
//    @Operation(summary = "상품 데이터 상세 조회 API", description = "상품 데이터를 상세조회 한다.")
//    @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
//    @GetMapping("/goods-data/{goodsDataNo}")
//    public BaseResponse<GoodsDataDetailRes> getGoodsData(
//        @PathVariable(value = "goodsDataNo") Integer goodsDataNo
//    ) {
//        return BaseResponseFactory.create(miDataDetailService.getGoodsData(goodsDataNo));
//    }
//
//    @ApiOrder(2)
//    @Parameters({
//        @Parameter(name = "goodsDataNo", description = "상품 데이터 번호"),
//    })
//    @Operation(summary = "데이터소스 목록 조회 API", description = "상품 데이터 번호에 매핑된 데이터소스 목록을 조회합니다.")
//    @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
//    @GetMapping("/goods-data/{goodsDataNo}/goods-data-sources")
//    public BaseResponse<List<GoodsDataSourceRes>> getGoodsDataSources(
//        @PathVariable(value = "goodsDataNo") Integer goodsDataNo
//    ) {
//        return BaseResponseFactory.create(miDataDetailService.getGoodsDataSources(goodsDataNo));
//    }
//
//    @ApiOrder(3)
//    @Parameters({
//        @Parameter(name = "goodsDataNo", description = "상품 데이터 번호"),
//        @Parameter(name = "dataSourceNo", description = "상품 데이터소스 번호")
//    })
//    @Operation(summary = "데이터소스 단위 변환 목록 조회 API", description = "데이터소스에서 변환이 가능한 단위 목록을 조회합니다.")
//    @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
//    @GetMapping("/goods-data/{goodsDataNo}/goods-data-sources/{goodsDataSourceNo}/currentUnitCd-conversion")
//    public BaseResponse<List<UnitConversionRes>> getGoodsDataSourcesUnitConversion(
//        @PathVariable(value = "goodsDataNo") Integer goodsDataNo,
//        @PathVariable(value = "goodsDataSourceNo") Integer goodsDataSourceNo
//    ) {
//        return BaseResponseFactory.create(miDataDetailService.getGoodsDataSourcesUnitConversion(goodsDataNo, goodsDataSourceNo));
//    }
//
//    @ApiOrder(4)
//    @Parameters({
//        @Parameter(name = "goodsDataNo", description = "상품 데이터 번호"),
//        @Parameter(name = "dataSourceNo", description = "상품 데이터소스 번호"),
//        @Parameter(name = "chartsPersonalizeReqs", description = "차트 개인화 정보")
//    })
//    @Operation(summary = "데이터소스 차트 개인화 조회 API", description = "상품 데이터소스 차트를 개인 설정을 조회합니다.")
//    @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
//    @GetMapping("/goods-data/{goodsDataNo}/goods-data-sources/{goodsDataSourceNo}/charts-personalize")
//    public BaseResponse<List<ChartsPersonalizeRes>> getChartsPersonalize(
//        @PathVariable(value = "goodsDataNo") Integer goodsDataNo,
//        @PathVariable(value = "goodsDataSourceNo") Integer goodsDataSourceNo
//    ) {
//        return BaseResponseFactory.create(miDataDetailService.getChartsPersonalize(goodsDataNo, goodsDataSourceNo));
//    }
//
    @ApiOrder(5)
    @Parameters({
        @Parameter(name = "goodsDataNo", description = "상품 데이터 번호"),
        @Parameter(name = "dataSourceNo", description = "상품 데이터소스 번호"),
        @Parameter(name = "chartsPersonalizeReqs", description = "차트 개인화 정보")
    })
    @Operation(summary = "데이터소스 차트 개인화 설정 API", description = "상품 데이터소스 차트를 개인 설정합니다.")
    @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
    @PutMapping("/goods-data/{goodsDataNo}/goods-data-sources/{goodsDataSourceNo}/charts-personalize")
    public BaseResponse<List<ChartsPersonalizeRes>> putChartsPersonalize(
        @PathVariable(value = "goodsDataNo") Integer goodsDataNo,
        @PathVariable(value = "goodsDataSourceNo") Integer goodsDataSourceNo,
        @RequestBody @Valid List<ChartsPersonalizeReq> chartsPersonalizeReqs
    ) {
        return BaseResponseFactory.create(miDataDetailService.putChartsPersonalize(goodsDataNo, goodsDataSourceNo, chartsPersonalizeReqs));
    }

    @ApiOrder(6)
    @Parameters({
        @Parameter(name = "goodsNo", description = "상품 번호"),
        @Parameter(name = "goodsDataNo", description = "상품 데이터 번호"),
        @Parameter(name = "goodsDataSourceNo", description = "상품 데이터소스 번호"),
        @Parameter(name = "interval", description = "인터벌"),
        @Parameter(name = "startDate", description = "조회 시작일"),
        @Parameter(name = "endDate", description = "조회 종료일"),
        @Parameter(name = "unitConversionCd", description = "변환 단위"),
    })
    @Operation(summary = "데이터소스 차트 조회 API", description = "데이터소스 차트를 조회합니다.")
    @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
    @GetMapping("/goods/{goodsNo}/goods-data/{goodsDataNo}/goods-data-sources/{goodsDataSourceNo}/chart")
    public BaseResponse<ChartBundleRes> getGoodsDataSourceChart(
        @PathVariable(value = "goodsNo") Integer goodsNo,
        @PathVariable(value = "goodsDataNo") Integer goodsDataNo,
        @PathVariable(value = "goodsDataSourceNo") Integer goodsDataSourceNo,
        @RequestParam(value = "interval", required = false, defaultValue = "ONE_DAY") Interval interval,
        @RequestParam(value = "range", defaultValue = "3M") Interval range,
        @RequestParam(value = "startDate", required = false) String startDate,
        @RequestParam(value = "endDate", required = false) String endDate,
        @RequestParam(value = "unitConversionCd", required = false) String unitConversionCd
    ) {
        return BaseResponseFactory.create(miDataDetailService.getDynamicCharts(goodsDataNo, goodsDataSourceNo, interval, range, startDate, endDate, unitConversionCd));
    }

}
