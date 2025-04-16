package com.sk.domain.mi.controller;

import com.sk.common.annotation.ApiOrder;
import com.sk.common.model.BaseResponse;
import com.sk.common.model.BaseResponseFactory;
import com.sk.domain.mi.dto.response.GoodsDataDetailRes;
import com.sk.domain.mi.dto.response.GoodsDataSourceRes;
import com.sk.domain.mi.service.MiDataDetailService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "05. 상품 데이터 상세 API", description = "상품 데이터 상세 API 입니다.")
@Slf4j
@RestController
@RequestMapping("/api/v1/mi/data-detail")
@RequiredArgsConstructor
public class MiDataDetailController {

    private final MiDataDetailService miDataDetailService;

    @ApiOrder(1)
    @Parameters({
        @Parameter(name = "goodsDataNo", description = "상품 데이터 번호"),
    })
    @Operation(summary = "상품 데이터 상세 조회 API", description = "상품 데이터를 상세조회 한다.")
    @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
    @GetMapping("/goods-data/{goodsDataNo}")
    public BaseResponse<GoodsDataDetailRes> getGoodsData(
        @PathVariable(value = "goodsDataNo") Integer goodsDataNo
    ) {
        return BaseResponseFactory.create(miDataDetailService.getGoodsData(goodsDataNo));
    }

    @ApiOrder(2)
    @Parameters({
        @Parameter(name = "goodsDataNo", description = "상품 데이터 번호"),
    })
    @Operation(summary = "데이터소스 목록 조회 API", description = "상품 데이터 번호에 매핑된 데이터소스 목록을 조회합니다.")
    @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
    @GetMapping("/goods-data/{goodsDataNo}/data-sources")
    public BaseResponse<List<GoodsDataSourceRes>> getGoodsDataSources(
        @PathVariable(value = "goodsDataNo") Integer goodsDataNo
    ) {
        return BaseResponseFactory.create(miDataDetailService.getGoodsDataSources(goodsDataNo));
    }

    @ApiOrder(3)
    @Parameters({
        @Parameter(name = "goodsDataNo", description = "상품 데이터 번호"),
        @Parameter(name = "dataSourceNo", description = "상품 데이터소스 번호"),
    })
    @Operation(summary = "데이터소스 차트 조회 API", description = "데이터소스 차트를 조회합니다.")
    @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
    @GetMapping("/goods-data/{goodsDataNo}/data-sources/{dataSourceNo}/chart")
    public BaseResponse<?> getGoodsDataSourceChart(
        @PathVariable(value = "goodsDataNo") Integer goodsDataNo,
        @PathVariable(value = "dataSourceNo") Integer dataSourceNo
    ) {
        return BaseResponseFactory.create(miDataDetailService.getGoodsDataSourceChart(goodsDataNo, dataSourceNo));
    }

}
