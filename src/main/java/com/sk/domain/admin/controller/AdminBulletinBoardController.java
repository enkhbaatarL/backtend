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
import com.sk.domain.admin.service.AdminBulletinBoardService;
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

/**
 * todo
 * 
 * 공통
 * 1. 카테고리는 어떻게?
 * 2. 
 * 3. 
 * 4. 
 *
 * 공지사항
 * 1. 파일 처리는 어떻게?
 * 2. 
 *
 * FAQ
 * 1. 
 * 2. 
 */

@Tag(name = "04. 게시판 관리 API", description = "게시판 관리 API 입니다.")
@Slf4j
@RestController
@RequestMapping("/api/v1/admin/bulletinboard-manangement")
@RequiredArgsConstructor
public class AdminBulletinBoardController {
  private final AdminBulletinBoardService adminBulletinBoardService;

  // @ApiOrder(1)
  // @Parameters({
  // @Parameter(name = "page", description = "페이지 번호 (기본값: 1)"),
  // @Parameter(name = "size", description = "페이지당 항목 수 (기본값: 20)"),
  // })
  // @Operation(summary = "공지사항 목록 조회 - with pagination", description = "공지사항 목록을 조회합니다.")
  // @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
  // @GetMapping("/provider")
  // public BaseResponse<PaginationRes<ProviderRes>> getAllProviders(
  // @RequestParam(value = "page", required = false, defaultValue = "1") @Min(value = 1) int page,
  // @RequestParam(value = "size", required = false, defaultValue = "20") @Min(value = 1) int size) {

  // return BaseResponseFactory.create(adminBulletinBoardService.getAllNotices(page, size));
  // }

}