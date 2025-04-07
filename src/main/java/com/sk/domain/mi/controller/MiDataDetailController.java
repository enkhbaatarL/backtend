package com.sk.domain.mi.controller;

import com.sk.common.annotation.ApiOrder;
import com.sk.common.model.BaseResponse;
import com.sk.common.model.BaseResponseFactory;
import com.sk.domain.mi.dto.response.PermissionRes;
import com.sk.domain.mi.service.MiDataDetailService;
import com.sk.domain.mi.service.MiUserService;
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
    @Operation(summary = "상품 데이터 차트 조회 API", description = "상품 데이터 차트를 조회힙나디.")
    @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
    @GetMapping("/goods-data/{goodsDataNo}/charts")
    public BaseResponse<PermissionRes> getGoodsDataCharts(
        @PathVariable(value = "goodsDataNo") Integer goodsDataNo
    ) {
        return BaseResponseFactory.create(miDataDetailService.getGoodsDataCharts(goodsDataNo));
    }

}
