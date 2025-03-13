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
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
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

  @Operation(summary = "목록 조회", description = "데이타 프로바이더 목록을 조회합니다. withpagination")
  @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
  @GetMapping
  public BaseResponse<PaginationRes<GoodsRes.All>> all(
      @RequestParam(value = "providerNo", required = true, defaultValue = "5000") @Min(value = 1) int providerNo,
      @RequestParam(value = "page", required = false, defaultValue = "1") @Min(value = 1) int page,
      @RequestParam(value = "size", required = false, defaultValue = "10") @Min(value = 1) int size,
      @RequestParam(value = "searchText", required = false, defaultValue = "") String searchText) {

    return BaseResponseFactory.create(goodsService.all(providerNo, page, size, searchText));
  }

  @Operation(summary = "상세 조회", description = "상품 1개를 조회합니다.")
  @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
  @GetMapping("/{goodsNo}")
  public BaseResponse<GoodsRes.One> one(@PathVariable int goodsNo) {
    return BaseResponseFactory.create(goodsService.one(goodsNo));
  }

  @Operation(summary = "수정", description = "상품을 수정합니다.")
  @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
  @PutMapping("/{goodsNo}")
  public BaseResponse<GoodsRes.Update> update(@RequestBody GoodsReq.Update goodsReq, @PathVariable int goodsNo) {
    return BaseResponseFactory.create(goodsService.update(goodsReq, goodsNo));
  }

  @Operation(summary = "삭제", description = "상품을 삭제합니다.")
  @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
  @DeleteMapping("/{goodsNo}")
  public BaseResponse<GoodsRes.Delete> delete(@PathVariable int goodsNo) {
    return BaseResponseFactory.create(goodsService.delete(goodsNo));
  }
}
