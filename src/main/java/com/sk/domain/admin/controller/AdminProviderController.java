package com.sk.domain.admin.controller;

import java.util.List;

import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.sk.common.annotation.ApiOrder;
import com.sk.common.model.BaseResponse;
import com.sk.common.model.BaseResponseFactory;
import com.sk.common.model.PaginationRes;
import com.sk.domain.admin.dto.request.provider.GoodsUpdateReq;
import com.sk.domain.admin.dto.request.provider.ProviderCreateReq;
import com.sk.domain.admin.dto.request.provider.ProviderUpdateReq;
import com.sk.domain.admin.dto.response.provider.GoodsDataSourceRes;
import com.sk.domain.admin.dto.response.provider.GoodsDeleteRes;
import com.sk.domain.admin.dto.response.provider.GoodsDetailRes;
import com.sk.domain.admin.dto.response.provider.GoodsRes;
import com.sk.domain.admin.dto.response.provider.GoodsUpdateRes;
import com.sk.domain.admin.dto.response.provider.ProviderCreateRes;
import com.sk.domain.admin.dto.response.provider.ProviderDeleteRes;
import com.sk.domain.admin.dto.response.provider.ProviderDetailRes;
import com.sk.domain.admin.dto.response.provider.ProviderLatestUpdatedRes;
import com.sk.domain.admin.dto.response.provider.ProviderNameRes;
import com.sk.domain.admin.dto.response.provider.ProviderRes;
import com.sk.domain.admin.dto.response.provider.ProviderUpdateRes;
import com.sk.domain.admin.service.AdminProviderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Tag(name = "03. 데이타 프로바이더 관리 API", description = "데이타 프로바이더 관리 API 입니다.")
@Slf4j
@RestController
@RequestMapping("/api/v1/admin/provider-manangement")
@RequiredArgsConstructor
public class AdminProviderController {
  private final AdminProviderService providerService;

  @ApiOrder(1)
  @Operation(summary = "데이타 프로바이더 목록 조회 - without pagination", description = "데이타 프로바이더 목록을 조회합니다.")
  @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
  @GetMapping("/provider/all")
  public BaseResponse<List<ProviderNameRes>> getAllProviders() {

    return BaseResponseFactory.create(providerService.getAllProviders());
  }

  @ApiOrder(2)
  @Parameters({
      @Parameter(name = "page", description = "페이지 번호 (기본값: 1)"),
      @Parameter(name = "size", description = "페이지당 항목 수 (기본값: 20)"),
  })
  @Operation(summary = "데이타 프로바이더 목록 조회 - with pagination", description = "데이타 프로바이더 목록을 조회합니다.")
  @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
  @GetMapping("/provider")
  public BaseResponse<PaginationRes<ProviderRes>> getAllProviders(
      @RequestParam(value = "page", required = false, defaultValue = "1") @Min(value = 1) int page,
      @RequestParam(value = "size", required = false, defaultValue = "20") @Min(value = 1) int size) {

    return BaseResponseFactory.create(providerService.getAllProviders(page, size));
  }

  @ApiOrder(3)
  @Operation(summary = "데이타 프로바이더 등록", description = "데이타 프로바이더를 등록합니다")
  @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
  @PostMapping("/provider")
  public BaseResponse<ProviderCreateRes> createProvider(
      @RequestBody @Valid ProviderCreateReq body) {
    return BaseResponseFactory.create(providerService.createProvider(body));
  }

  @ApiOrder(4)
  @Parameters({
      @Parameter(name = "providerNo", description = "프로바이더번호 (기본값: 5000)")
  })
  @Operation(summary = "데이타 프로바이더 상세 조회", description = "데이타 프로바이더 1개를 조회합니다.")
  @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
  @GetMapping("/providers/{providerNo}")
  public BaseResponse<ProviderDetailRes> getPrivider(
      @PathVariable(value = "providerNo") @Min(value = 1) Integer providerNo) {
    return BaseResponseFactory.create(providerService.getPrivider(providerNo));
  }

  @ApiOrder(5)
  @Parameters({
      @Parameter(name = "providerNo", description = "프로바이더번호 (기본값: 5000)")
  })
  @Operation(summary = "데이타 프로바이더 수정", description = "데이타 프로바이더를 수정합니다.")
  @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
  @PatchMapping("/providers/{providerNo}")
  public BaseResponse<ProviderUpdateRes> updateProvider(
      @RequestBody @Valid ProviderUpdateReq body,
      @PathVariable(value = "providerNo") @Min(value = 1) Integer providerNo) {
    return BaseResponseFactory.create(providerService.updateProvider(body, providerNo));
  }

  @ApiOrder(6)
  @Parameters({
      @Parameter(name = "providerNo", description = "프로바이더번호 (기본값: 5000)")
  })
  @Operation(summary = "데이타 프로바이더 삭제", description = "데이타 프로바이더를 삭제합니다.")
  @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
  @DeleteMapping("/providers/{providerNo}")
  public BaseResponse<ProviderDeleteRes> deleteProvider(
      @PathVariable(value = "providerNo") @Min(value = 1) Integer providerNo) {
    return BaseResponseFactory.create(providerService.deleteProvider(providerNo));
  }

  @ApiOrder(7)
  @Parameters({
      @Parameter(name = "providerNo", description = "프로바이더번호 (기본값: 5000)")
  })
  @Operation(summary = "데이타 프로바이더 최신 수정일, 수정자", description = "데이타 프로바이더의 최신 수정일과 수정자를 반납합니다.")
  @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
  @GetMapping("/providers/{providerNo}/latest-updated/")
  public BaseResponse<ProviderLatestUpdatedRes> getLatestUpdated(
      @PathVariable(value = "providerNo") @Min(value = 1) Integer providerNo) {
    return BaseResponseFactory.create(providerService.getLatestUpdated(providerNo));
  }

  @ApiOrder(8)
  @Parameters({
      @Parameter(name = "page", description = "페이지 번호 (기본값: 1)"),
      @Parameter(name = "size", description = "페이지당 항목 수 (기본값: 20)"),
      @Parameter(name = "keyword", description = "상품명 또는 상품설명 검색"),
      @Parameter(name = "providerNo", description = "프로바이더번호 (기본값: 5000)")
  })
  @Operation(summary = "상품 목록 조회 - with pagination", description = "상품 목록을 조회합니다.")
  @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
  @GetMapping("/providers/{providerNo}/goods")
  public BaseResponse<PaginationRes<GoodsRes>> getAllGoods(
      @RequestParam(value = "page", required = false, defaultValue = "1") @Min(value = 1) int page,
      @RequestParam(value = "size", required = false, defaultValue = "20") @Min(value = 1) int size,
      @RequestParam(value = "keyword", required = false, defaultValue = "") String keyword,
      @PathVariable(value = "providerNo") @Min(value = 1) Integer providerNo) {
    return BaseResponseFactory.create(providerService.getAllGoods(page, size, keyword, providerNo));
  }

  @ApiOrder(9)
  @Parameters({
      @Parameter(name = "providerNo", description = "데이타 프로바이더 번호 (기본값: 5000)"),
      @Parameter(name = "goodsNo", description = "상품 번호 (기본값: 70000)")
  })
  @Operation(summary = "상품 상세 조회", description = "상품 1개를 조회합니다.")
  @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
  @GetMapping("/providers/{providerNo}/goods/{goodsNo}")
  public BaseResponse<GoodsDetailRes> getGoods(
      @PathVariable(value = "providerNo") @Min(value = 1) Integer providerNo,
      @PathVariable(value = "goodsNo") @Min(value = 1) Integer goodsNo) {
    return BaseResponseFactory.create(providerService.getGoods(providerNo, goodsNo));
  }

  @ApiOrder(10)
  @Parameters({
      @Parameter(name = "providerNo", description = "데이타 프로바이더 번호 (기본값: 5000)"),
      @Parameter(name = "goodsNo", description = "상품 번호 (기본값: 70000)")
  })
  @Operation(summary = "상품 수정", description = "상품을 수정합니다.")
  @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
  @PatchMapping("/providers/{providerNo}/goods/{goodsNo}")
  public BaseResponse<GoodsUpdateRes> updateGoods(
      @RequestBody @Valid GoodsUpdateReq body,
      @PathVariable(value = "providerNo") @Min(value = 1) Integer providerNo,
      @PathVariable(value = "goodsNo") @Min(value = 1) Integer goodsNo) {
    return BaseResponseFactory.create(providerService.updateGoods(providerNo, goodsNo, body));
  }

  @ApiOrder(11)
  @Parameters({
      @Parameter(name = "providerNo", description = "데이타 프로바이더 번호 (기본값: 5000)"),
      @Parameter(name = "goodsNo", description = "상품 번호 (기본값: 70000)")
  })
  @Operation(summary = "상품 삭제", description = "상품을 삭제합니다.")
  @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
  @DeleteMapping("/providers/{providerNo}/goods/{goodsNo}")
  public BaseResponse<GoodsDeleteRes> deleteGoods(
      @PathVariable(value = "providerNo") @Min(value = 1) Integer providerNo,
      @PathVariable(value = "goodsNo") @Min(value = 1) Integer goodsNo) {
    return BaseResponseFactory.create(providerService.deleteGoods(providerNo, goodsNo));
  }

  @ApiOrder(12)
  @Parameters({
      @Parameter(name = "page", description = "페이지 번호 (기본값: 1)"),
      @Parameter(name = "size", description = "페이지당 항목 수 (기본값: 20)"),
      @Parameter(name = "keyword", description = "데이타명 또는 데이타설명 검색"),
      @Parameter(name = "providerNo", description = "데이타 프로바이더 번호 (기본값: 5000)"),
      @Parameter(name = "goodsNo", description = "상품번호 (기본값: 70000)")
  })
  @Operation(summary = "데이타셋 목록 조회 - withpagination", description = "데이터셋 목록을 조회합니다.")
  @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
  @GetMapping("/providers/{providerNo}/goods/{goodsNo}/goodsdatasource")
  public BaseResponse<PaginationRes<GoodsDataSourceRes>> getGoodsDataSoueces(
      @RequestParam(value = "page", required = false, defaultValue = "1") @Min(value = 1) int page,
      @RequestParam(value = "size", required = false, defaultValue = "20") @Min(value = 1) int size,
      @RequestParam(value = "keyword", required = false, defaultValue = "") String keyword,
      @PathVariable(value = "providerNo") @Min(value = 1) Integer providerNo,
      @PathVariable(value = "goodsNo") @Min(value = 1) Integer goodsNo) {

    return BaseResponseFactory.create(providerService.getGoodsDataSoueces(page, size, keyword, providerNo, goodsNo));
  }
}
