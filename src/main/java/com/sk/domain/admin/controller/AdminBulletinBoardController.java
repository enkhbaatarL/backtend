package com.sk.domain.admin.controller;

import java.util.Arrays;
import java.util.List;

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
import com.sk.common.enums.ResultCode;
import com.sk.common.exception.AppErrorException;
import com.sk.common.model.BaseResponse;
import com.sk.common.model.BaseResponseFactory;
import com.sk.common.model.PaginationRes;
import com.sk.domain.admin.dto.request.bbs.FaqCreateReq;
import com.sk.domain.admin.dto.request.bbs.FaqUpdateReq;
import com.sk.domain.admin.dto.request.bbs.NoticeCreateReq;
import com.sk.domain.admin.dto.request.bbs.NoticeUpdateReq;
import com.sk.domain.admin.dto.response.bbs.FaqFilterRes;
import com.sk.domain.admin.dto.response.bbs.NoticeDetailRes;
import com.sk.domain.admin.dto.response.bbs.NoticeFileRes;
import com.sk.domain.admin.dto.response.bbs.NoticeFilterRes;
import com.sk.domain.admin.service.AdminBulletinBoardService;
import com.sk.domain.admin.service.AdminFileService;
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
 * 1. 파일 처리는 어떻게? -> 컨트롤러 만들고 어디 저장 할지 확인 필요
 * 2. 팝업용 api 만들어주기 ->
 * 3. 
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
  private final AdminFileService adminFileService;

  /*******************************************************************
   * 
   * 공지사항
   * 
   *******************************************************************/
  private static final List<String> ALLOWED_EXTENSIONS = Arrays.asList("jpg", "png");
  private static final long MAX_SIZE = 20 * 1024 * 1024;

  @ApiOrder(1)
  @Operation(summary = "공지사항 필터 조회", description = "공지사항 필터를 조회합니다.")
  @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
  @GetMapping("/notice/filter")
  public BaseResponse<NoticeFilterRes> getNoticeFilter() {

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
      @RequestBody @Valid NoticeCreateReq body) {

    return BaseResponseFactory.create(adminBulletinBoardService.createNotice(body));
  }

  @ApiOrder(4)
  @Parameters({
      @Parameter(name = "noticeNo", description = "공지사항번호 (기본값: 1)")
  })
  @Operation(summary = "공지사항 상세 조회", description = "공지사항 1개를 조회합니다.")
  @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
  @GetMapping("/notices/{noticeNo}")
  public BaseResponse<NoticeDetailRes> getNotice(
      @PathVariable(value = "noticeNo") @Min(value = 1) Integer noticeNo) {
    return BaseResponseFactory.create(adminBulletinBoardService.getNotice(noticeNo));
  }

  @ApiOrder(5)
  @Parameters({
      @Parameter(name = "noticeNo", description = "공지사항번호 (기본값: 1)")
  })
  @Operation(summary = "공지사항 수정", description = "공지사항을 수정합니다.")
  @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
  @PatchMapping("/notices/{noticeNo}")
  public BaseResponse<BulletinBoard> updateNotice(
      @RequestBody @Valid NoticeUpdateReq body,
      @PathVariable(value = "noticeNo") @Min(value = 1) Integer noticeNo) {
    return BaseResponseFactory.create(adminBulletinBoardService.updateNotice(body, noticeNo));
  }

  @ApiOrder(6)
  @Parameters({
      @Parameter(name = "noticeNo", description = "공지사항번호 (기본값: 1)")
  })
  @Operation(summary = "공지사항 삭제", description = "공지사항을 삭제합니다.")
  @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
  @DeleteMapping("/notices/{noticeNo}")
  public BaseResponse<BulletinBoard> deleteNotice(
      @PathVariable(value = "noticeNo") @Min(value = 1) Integer noticeNo) {
    return BaseResponseFactory.create(adminBulletinBoardService.deleteNotice(noticeNo));
  }

  @ApiOrder(7)
  @Operation(summary = "공지사항 첨부 파일 업로드", description = "공지사항 첨부 파일을 업로드합니다. 20MB 이하의 jpg, png")
  @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
  @PostMapping(value = "/", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public BaseResponse<NoticeFileRes> uploadFile(
      @RequestPart(value = "file", required = false) MultipartFile file) {

    if (file == null) {
      throw AppErrorException.of(ResultCode.Error.REQ_INVALID_PARAMETER);
    }

    String fileName = file.getOriginalFilename();
    if (fileName == null || fileName.lastIndexOf('.') == -1) {
      throw AppErrorException.of(ResultCode.Error.REQ_INVALID);
    }

    String extension = fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase();
    if (!ALLOWED_EXTENSIONS.contains(extension)) {
      throw AppErrorException.of(ResultCode.Error.REQ_NOT_ALLOWED_FILE_FORMAT);
    }

    if (file.getSize() > MAX_SIZE) {
      throw AppErrorException.of(ResultCode.Error.REQ_NOT_ALLOWED_FILE_SIZE);
    }

    Integer fileId = adminFileService.uploadFile(file);

    return BaseResponseFactory.create(NoticeFileRes.of(fileId));
  }

  /*******************************************************************
   * 
   * FAQ
   * 
   *******************************************************************/
  @ApiOrder(8)
  @Operation(summary = "FAQ 필터 조회", description = "FAQ 필터를 조회합니다.")
  @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
  @GetMapping("/faq/filter")
  public BaseResponse<FaqFilterRes> getFaqFilter() {

    return BaseResponseFactory.create(adminBulletinBoardService.getFaqFilter());
  }

  @ApiOrder(9)
  @Parameters({
      @Parameter(name = "page", description = "페이지 번호 (기본값: 1)"),
      @Parameter(name = "size", description = "페이지당 항목 수 (기본값: 20)"),
      @Parameter(name = "category", description = "카테고리"),
      @Parameter(name = "keyword", description = "제목 또는 내용")
  })
  @Operation(summary = "FAQ 목록 조회 - with pagination", description = "FAQ 목록을 조회합니다.")
  @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
  @GetMapping("/faq")
  public BaseResponse<PaginationRes<BulletinBoard>> getAllFaq(
      @RequestParam(value = "page", required = false, defaultValue = "1") @Min(value = 1) int page,
      @RequestParam(value = "size", required = false, defaultValue = "20") @Min(value = 1) int size,
      @RequestParam(value = "category", required = false, defaultValue = "") String category,
      @RequestParam(value = "keyword", required = false, defaultValue = "") String keyword) {

    return BaseResponseFactory.create(adminBulletinBoardService.getAllFaq(page, size, category, keyword));
  }

  @ApiOrder(10)
  @Operation(summary = "FAQ 등록", description = "FAQ를 등록합니다")
  @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
  @PostMapping("/faq")
  public BaseResponse<BulletinBoard> createFaq(
      @RequestBody() @Valid FaqCreateReq body) {

    return BaseResponseFactory.create(adminBulletinBoardService.createFaq(body));
  }

  @ApiOrder(11)
  @Parameters({
      @Parameter(name = "faqNo", description = "FAQ 번호 (기본값: 1)")
  })
  @Operation(summary = "FAQ 상세 조회", description = "FAQ 1개를 조회합니다.")
  @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
  @GetMapping("/faq/{faqNo}")
  public BaseResponse<BulletinBoard> getFaq(
      @PathVariable(value = "faqNo") @Min(value = 1) Integer faqNo) {
    return BaseResponseFactory.create(adminBulletinBoardService.getFaq(faqNo));
  }

  @ApiOrder(12)
  @Parameters({
      @Parameter(name = "faqNo", description = "FAQ 번호 (기본값: 1)")
  })
  @Operation(summary = "FAQ 수정", description = "FAQ를 수정합니다.")
  @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
  @PatchMapping("/faq/{faqNo}")
  public BaseResponse<BulletinBoard> updateFaq(
      @RequestBody @Valid FaqUpdateReq body,
      @PathVariable(value = "faqNo") @Min(value = 1) Integer faqNo) {
    return BaseResponseFactory.create(adminBulletinBoardService.updateFaq(body, faqNo));
  }

  @ApiOrder(13)
  @Parameters({
      @Parameter(name = "faqNo", description = "FAQ 번호 (기본값: 1)")
  })
  @Operation(summary = "FAQ 삭제", description = "FAQ를 삭제합니다.")
  @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
  @DeleteMapping("/faq/{faqNo}")
  public BaseResponse<BulletinBoard> deleteFaq(
      @PathVariable(value = "faqNo") @Min(value = 1) Integer faqNo) {
    return BaseResponseFactory.create(adminBulletinBoardService.deleteFaq(faqNo));
  }

}