package com.sk.domain.admin.controller;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.sk.common.model.BaseResponse;
import com.sk.common.model.BaseResponseFactory;
import com.sk.common.model.PaginationRes;
import com.sk.domain.admin.dto.request.ProviderReq;
import com.sk.domain.admin.dto.response.ProviderRes;
import com.sk.domain.admin.service.AdminProviderService;
import com.sk.domain.shared.entity.master.tmd.Provider;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Tag(name = "03. 데이타 프로바이더 API", description = "데이타 프로바이더 API 입니다.")
@Slf4j
@RestController
@RequestMapping("/api/v1/admin/provider")
@RequiredArgsConstructor
public class AdminProviderController {
  private final AdminProviderService providerService;

  @Operation(summary = "목록 조회 - without pagination", description = "데이타 프로바이더 목록을 조회합니다.")
  @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
  @GetMapping("/list")
  public BaseResponse<List<Provider>> getAllProviders() {

    return BaseResponseFactory.create(providerService.getAllProviders());
  }

  @Operation(summary = "목록 조회 - with pagination", description = "데이타 프로바이더 목록을 조회합니다.")
  @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
  @GetMapping
  public BaseResponse<PaginationRes<ProviderRes>> getAllProviders(
      @RequestParam(value = "page", required = false, defaultValue = "1") @Min(value = 1) int page,
      @RequestParam(value = "size", required = false, defaultValue = "20") @Min(value = 1) int size) {

    return BaseResponseFactory.create(providerService.getAllProviders(page, size));
  }

  @Operation(summary = "등록", description = "데이타 프로바이더를 등록합니다")
  @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
  @PostMapping
  public BaseResponse<ProviderRes.Create> createProvider(@Valid @RequestBody ProviderReq.Create providerReq) {
    return BaseResponseFactory.create(providerService.createProvider(providerReq));
  }

  @Operation(summary = "상세 조회", description = "데이타 프로바이더 1개를 조회합니다.")
  @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
  @GetMapping("/{providerNo}")
  public BaseResponse<ProviderRes.Get> getPrivider(@PathVariable int providerNo) {
    return BaseResponseFactory.create(providerService.getPrivider(providerNo));
  }

  @Operation(summary = "수정", description = "데이타 프로바이더를 수정합니다.")
  @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
  @PutMapping("/{providerNo}")
  public BaseResponse<ProviderRes.Update> update(@Valid @RequestBody ProviderReq.Update providerReq,
      @PathVariable int providerNo) {
    return BaseResponseFactory.create(providerService.updateProvider(providerReq, providerNo));
  }

  @Operation(summary = "삭제", description = "데이타 프로바이더를 삭제합니다.")
  @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
  @DeleteMapping("/{providerNo}")
  public BaseResponse<ProviderRes.Delete> delete(@PathVariable int providerNo) {
    return BaseResponseFactory.create(providerService.deleteProvider(providerNo));
  }

  @Operation(summary = "수정일", description = "데이타 프로바이더의 수정일을 반납합니다.")
  @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
  @GetMapping("/latest-modifydt/{providerNo}")
  public BaseResponse<ProviderRes.ModifyDt> getLatestModifyDt(@PathVariable int providerNo) {
    return BaseResponseFactory.create(providerService.getLatestModifyDt(providerNo));
  }
}
