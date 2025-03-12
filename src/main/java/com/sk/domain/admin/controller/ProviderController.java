package com.sk.domain.admin.controller;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.sk.common.enums.ResultCode;
import com.sk.common.model.BaseResponse;
import com.sk.common.model.BaseResponseFactory;
import com.sk.common.model.PaginationRes;
import com.sk.domain.admin.dto.request.ProviderReq;
import com.sk.domain.admin.dto.response.ProviderRes;
import com.sk.domain.admin.service.ProviderService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Tag(name = "03. 데이타 프로바이더 API", description = "데이타 프로바이더 API 입니다.")
@Slf4j
@RestController
@RequestMapping("/api/v1/provider")
@RequiredArgsConstructor
public class ProviderController {
  private final ProviderService service;

  @Operation(summary = "목록 조회", description = "데이타 프로바이더 목록을 조회합니다. with pagination")
  @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
  @GetMapping
  public BaseResponse<PaginationRes<ProviderRes>> all(
      @RequestParam(value = "page", required = false, defaultValue = "1") @Min(value = 1) int page,
      @RequestParam(value = "size", required = false, defaultValue = "10") @Min(value = 1) int size) {

    return BaseResponseFactory.create(service.all(page, size));
  }

  @Operation(summary = "등록", description = "데이타 프로바이더를 등록합니다")
  @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
  @PostMapping("/")
  public BaseResponse<ProviderRes> newProvider(@RequestBody ProviderReq newProvider) {
    return BaseResponseFactory.create(service.newProvider(newProvider));
  }

  @Operation(summary = "상세 조회", description = "데이타 프로바이더 1개를 조회합니다.")
  @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
  @GetMapping("/{id}")
  public BaseResponse<ProviderRes> one(@PathVariable Integer id) {
    return BaseResponseFactory.create(service.one(id));
  }

  @Operation(summary = "수정", description = "데이타 프로바이더를 수정합니다.")
  @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
  @PutMapping("/{id}")
  public BaseResponse<ProviderRes> replaceProvider(@RequestBody ProviderReq newProvider, @PathVariable Integer id) {
    return BaseResponseFactory.create(service.replaceProvider(newProvider, id));
  }

  @Operation(summary = "삭제", description = "데이타 프로바이더를 삭제합니다.")
  @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
  @DeleteMapping("/{id}")
  public BaseResponse<?> deleteProvider(@PathVariable Integer id) {
    service.deleteProvider(id);

    return new BaseResponse<>(ResultCode.Success.OK.getCode(), ResultCode.Success.OK.getMessage());
  }
}
