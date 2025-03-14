package com.sk.domain.admin.controller;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.sk.common.model.BaseResponse;
import com.sk.common.model.BaseResponseFactory;
import com.sk.common.model.PaginationRes;
import com.sk.domain.admin.dto.request.GoodsReq;
import com.sk.domain.admin.dto.response.GoodsRes;
import com.sk.domain.admin.service.AdminGoodsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Tag(name = "04. 데이타 프로바이더 상품 API", description = "데이타 프로바이더 상품 API 입니다.")
@Slf4j
@RestController
@RequestMapping("/api/v1/admin/goods")
@RequiredArgsConstructor
public class AdminGoodsController {
  private final AdminGoodsService goodsService;

  @Parameters({
      @Parameter(name = "providerNo", description = "프로바이더번호 (기본값: 1)"),
      @Parameter(name = "page", description = "페이지 번호 (기본값: 1)"),
      @Parameter(name = "size", description = "페이지당 항목 수 (기본값: 20)"),
      @Parameter(name = "searchText", description = "상품명 또는 상품설명 검색")
  })
  @Operation(summary = "목록 조회 - withpagination", description = "상품 목록을 조회합니다.")
  @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
  @GetMapping
  public BaseResponse<PaginationRes<GoodsRes>> getAllGoods(
      @RequestParam(value = "providerNo", required = true, defaultValue = "1") @Min(value = 1) int providerNo,
      @RequestParam(value = "page", required = false, defaultValue = "1") @Min(value = 1) int page,
      @RequestParam(value = "size", required = false, defaultValue = "20") @Min(value = 1) int size,
      @RequestParam(value = "searchText", required = false, defaultValue = "") String searchText) {

    return BaseResponseFactory.create(goodsService.getAllGoods(providerNo, page, size, searchText));
  }

  @Operation(summary = "상세 조회", description = "상품 1개를 조회합니다.")
  @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
  @GetMapping("/{goodsNo}")
  public BaseResponse<GoodsRes.Get> getGoods(
      @Parameter(name = "goodsNo", description = "상품번호") //
      @PathVariable int goodsNo) {
    return BaseResponseFactory.create(goodsService.getGoods(goodsNo));
  }

  @Operation(summary = "수정", description = "상품을 수정합니다.")
  @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
  @PutMapping("/{goodsNo}")
  public BaseResponse<GoodsRes.Update> updateGoods(@Valid @RequestBody GoodsReq.Update goodsReq,
      @Parameter(name = "goodsNo", description = "상품번호") //
      @PathVariable int goodsNo) {
    return BaseResponseFactory.create(goodsService.updateGoods(goodsReq, goodsNo));
  }

  @Operation(summary = "삭제", description = "상품을 삭제합니다.")
  @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
  @DeleteMapping("/{goodsNo}")
  public BaseResponse<GoodsRes.Delete> deleteGoods(
      @Parameter(name = "goodsNo", description = "상품번호") //
      @PathVariable int goodsNo) {
    return BaseResponseFactory.create(goodsService.deleteGoods(goodsNo));
  }
}
