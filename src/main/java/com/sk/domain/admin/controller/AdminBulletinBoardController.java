package com.sk.domain.admin.controller;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.sk.common.annotation.ApiOrder;
import com.sk.common.model.BaseResponse;
import com.sk.common.model.BaseResponseFactory;
import com.sk.common.model.PaginationRes;
import com.sk.domain.admin.dto.request.bbs.BulletinBoardCreateReq;
import com.sk.domain.admin.dto.request.provider.ProviderUpdateReq;
import com.sk.domain.admin.dto.response.bbs.BulletinBoardFilterRes;
import com.sk.domain.admin.service.AdminBulletinBoardService;
import com.sk.domain.shared.entity.master.tcm.BulletinBoard;

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
 * 1. 카테고리는 어떻게? -> enum 으로 임시 작업
 * 2. 
 * 3. 
 * 4. 
 *
 * 공지사항
 * 1. 파일 처리는 어떻게? ->
 * 2. 게시여부 관련 처리는 어떻게? 기획에 질문 던져놨음, isDisplayed, isDeleted, isPinned, isPopup -> 게시날짜 사용해서 처리
 * 3. 팝업용 api 만들어주기
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

  @ApiOrder(1)
  @Operation(summary = "공지사항 필터 조회", description = "공지사항 필터를 조회합니다.")
  @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
  @GetMapping("/notice/filter")
  public BaseResponse<BulletinBoardFilterRes> getNoticeFilter() {

    return BaseResponseFactory.create(adminBulletinBoardService.getNoticeFilter());
  }

  @ApiOrder(2)
  @Parameters({
      @Parameter(name = "page", description = "페이지 번호 (기본값: 1)"),
      @Parameter(name = "size", description = "페이지당 항목 수 (기본값: 20)"),
      @Parameter(name = "category", description = "카테고리"),
      @Parameter(name = "status", description = "게시여부"),
      @Parameter(name = "target", description = "게시대상"),
      @Parameter(name = "keyword", description = "제목 또는 내용")
  })
  @Operation(summary = "공지사항 목록 조회 - with pagination", description = "공지사항 목록을 조회합니다.")
  @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
  @GetMapping("/notice")
  public BaseResponse<PaginationRes<BulletinBoard>> getAllNotices(
      @RequestParam(value = "page", required = false, defaultValue = "1") @Min(value = 1) int page,
      @RequestParam(value = "size", required = false, defaultValue = "20") @Min(value = 1) int size,
      @RequestParam(value = "category", required = false, defaultValue = "") String category,
      @RequestParam(value = "status", required = false, defaultValue = "") String status,
      @RequestParam(value = "target", required = false, defaultValue = "") String target,
      @RequestParam(value = "keyword", required = false, defaultValue = "") String keyword) {

    return BaseResponseFactory.create(adminBulletinBoardService.getAllNotices(page, size, category, status, target, keyword));
  }

  @ApiOrder(3)
  @Operation(summary = "공지사항 등록", description = "공지사항을 등록합니다")
  @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
  @PostMapping(value = "/notice", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public BaseResponse<BulletinBoard> createNotice(
      @RequestPart(value = "body") @Valid BulletinBoardCreateReq body,
      @RequestPart(value = "file", required = false) MultipartFile file) {

    return BaseResponseFactory.create(adminBulletinBoardService.createNotice(body, file));
  }

  @ApiOrder(4)
  @Parameters({
      @Parameter(name = "bulletinNo", description = "공지사항번호 (기본값: 1)")
  })
  @Operation(summary = "공지사항 상세 조회", description = "공지사항 1개를 조회합니다.")
  @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
  @GetMapping("/notices/{bulletinNo}")
  public BaseResponse<String> getNotice(
      @PathVariable(value = "bulletinNo") @Min(value = 1) Integer bulletinNo) {
    return BaseResponseFactory.create("todo");
  }

  @ApiOrder(5)
  @Parameters({
      @Parameter(name = "bulletinNo", description = "공지사항번호 (기본값: 1)")
  })
  @Operation(summary = "공지사항 수정", description = "공지사항을 수정합니다.")
  @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
  @PatchMapping("/notices/{bulletinNo}")
  public BaseResponse<String> updateNotice(@Valid @RequestBody ProviderUpdateReq body,
      @PathVariable(value = "bulletinNo") @Min(value = 1) Integer bulletinNo) {
    return BaseResponseFactory.create("todo");
  }

  @ApiOrder(6)
  @Parameters({
      @Parameter(name = "bulletinNo", description = "공지사항번호 (기본값: 1)")
  })
  @Operation(summary = "공지사항 삭제", description = "공지사항을 삭제합니다.")
  @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
  @DeleteMapping("/notices/{bulletinNo}")
  public BaseResponse<String> deleteNotice(
      @PathVariable(value = "bulletinNo") @Min(value = 1) Integer bulletinNo) {
    return BaseResponseFactory.create("todo");
  }

  @ApiOrder(7)
  @Operation(summary = "FAQ 필터 조회", description = "FAQ 필터를 조회합니다.")
  @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
  @GetMapping("/faq/filter")
  public BaseResponse<BulletinBoardFilterRes> getFaqFilter() {

    return BaseResponseFactory.create(adminBulletinBoardService.getFaqFilter());
  }

  @ApiOrder(8)
  @Parameters({
      @Parameter(name = "page", description = "페이지 번호 (기본값: 1)"),
      @Parameter(name = "size", description = "페이지당 항목 수 (기본값: 20)"),
      @Parameter(name = "category", description = "카테고리")
  })
  @Operation(summary = "FAQ 목록 조회 - with pagination", description = "FAQ 목록을 조회합니다.")
  @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
  @GetMapping("/faq")
  public BaseResponse<PaginationRes<BulletinBoard>> getAllFaq(
      @RequestParam(value = "page", required = false, defaultValue = "1") @Min(value = 1) int page,
      @RequestParam(value = "size", required = false, defaultValue = "20") @Min(value = 1) int size,
      @RequestParam(value = "category", required = false, defaultValue = "") String category) {

    return BaseResponseFactory.create(adminBulletinBoardService.getAllFaq(page, size, category));
  }

  @ApiOrder(9)
  @Operation(summary = "FAQ 등록", description = "FAQ를 등록합니다")
  @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
  @PostMapping(value = "/faq", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public BaseResponse<String> createFaq(
      @RequestPart(value = "body") @Valid BulletinBoardCreateReq body,
      @RequestPart(value = "file", required = false) MultipartFile file) {

    return BaseResponseFactory.create("todo");
  }

  @ApiOrder(10)
  @Parameters({
      @Parameter(name = "bulletinNo", description = "FAQ 번호 (기본값: 1)")
  })
  @Operation(summary = "FAQ 상세 조회", description = "FAQ 1개를 조회합니다.")
  @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
  @GetMapping("/faq/{bulletinNo}")
  public BaseResponse<String> getPrivider(
      @PathVariable(value = "bulletinNo") @Min(value = 1) Integer bulletinNo) {
    return BaseResponseFactory.create("todo");
  }

  @ApiOrder(11)
  @Parameters({
      @Parameter(name = "bulletinNo", description = "FAQ 번호 (기본값: 1)")
  })
  @Operation(summary = "FAQ 수정", description = "FAQ를 수정합니다.")
  @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
  @PatchMapping("/faq/{bulletinNo}")
  public BaseResponse<String> update(@Valid @RequestBody ProviderUpdateReq body,
      @PathVariable(value = "bulletinNo") @Min(value = 1) Integer bulletinNo) {
    return BaseResponseFactory.create("todo");
  }

  @ApiOrder(12)
  @Parameters({
      @Parameter(name = "bulletinNo", description = "FAQ 번호 (기본값: 1)")
  })
  @Operation(summary = "FAQ 삭제", description = "FAQ를 삭제합니다.")
  @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
  @DeleteMapping("/faq/{bulletinNo}")
  public BaseResponse<String> delete(
      @PathVariable(value = "bulletinNo") @Min(value = 1) Integer bulletinNo) {
    return BaseResponseFactory.create("todo");
  }

}