package com.sk.domain.admin.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.sk.common.model.BaseResponse;
import com.sk.common.model.BaseResponseFactory;
import com.sk.common.model.PaginationRes;
import com.sk.domain.admin.dto.response.GoodsDataSourceRes;
import com.sk.domain.admin.service.AdminGoodsDataSourceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Tag(name = "05. 데이타 프로바이더 데이타 API", description = "데이타 프로바이더 데이타 API 입니다.")
@Slf4j
@RestController
@RequestMapping("/api/v1/admin/goodsdatasource")
@RequiredArgsConstructor
public class AdminGoodsDataSourceController {
  private final AdminGoodsDataSourceService goodsDataSourceService;

  @Operation(summary = "목록 조회", description = "데이타 프로바이더 데이터 목록을 조회합니다. withpagination")
  @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
  @GetMapping
  public BaseResponse<PaginationRes<GoodsDataSourceRes.All>> all(
      @RequestParam(value = "goodsNo", required = true, defaultValue = "1") @Min(value = 1) int goodsNo,
      @RequestParam(value = "page", required = false, defaultValue = "1") @Min(value = 1) int page,
      @RequestParam(value = "size", required = false, defaultValue = "10") @Min(value = 1) int size,
      @RequestParam(value = "searchText", required = false, defaultValue = "") String searchText) {

    return BaseResponseFactory.create(goodsDataSourceService.all(goodsNo, page, size, searchText));
  }

}
