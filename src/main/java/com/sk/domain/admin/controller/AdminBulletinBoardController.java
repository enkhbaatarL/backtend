package com.sk.domain.admin.controller;

import java.util.Arrays;
import java.util.List;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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
import com.sk.common.annotation.UserResolve;
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
import com.sk.domain.admin.dto.response.bbs.FaqRes;
import com.sk.domain.admin.dto.response.bbs.NoticeDetailRes;
import com.sk.domain.admin.dto.response.bbs.NoticeFilterRes;
import com.sk.domain.admin.dto.response.bbs.NoticeRes;
import com.sk.domain.admin.dto.response.bbs.PopupRes;
import com.sk.domain.admin.dto.response.file.FileDownloadRes;
import com.sk.domain.admin.dto.response.file.FileUploadRes;
import com.sk.domain.admin.service.AdminBulletinBoardService;
import com.sk.domain.admin.service.AdminFileService;
import com.sk.domain.shared.entity.master.tcm.BulletinBoard;
import com.sk.domain.shared.entity.master.tmd.User;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

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
    private static final long MAX_SIZE = 10 * 1024 * 1024;

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
    public BaseResponse<PaginationRes<NoticeRes>> getAllNotices(
            @RequestParam(value = "page", required = false, defaultValue = "1") @Min(value = 1) int page,
            @RequestParam(value = "size", required = false, defaultValue = "20") @Min(value = 1) int size,
            @RequestParam(value = "category", required = false, defaultValue = "") String category,
            @RequestParam(value = "status", required = false, defaultValue = "") String status,
            @RequestParam(value = "target", required = false, defaultValue = "") Integer target,
            @RequestParam(value = "keyword", required = false, defaultValue = "") String keyword) {

        return BaseResponseFactory.create(adminBulletinBoardService.getAllNotices(page, size, category, status, target, keyword));
    }

    @ApiOrder(3)
    @Operation(summary = "공지사항 등록", description = "공지사항을 등록합니다")
    @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
    @PostMapping("/notice")
    public BaseResponse<String> createNotice(
            @UserResolve User user,
            @RequestBody @Valid NoticeCreateReq body) {

        adminBulletinBoardService.createNotice(user, body);

        return BaseResponseFactory.create("success");
    }

    @ApiOrder(4)
    @Parameters({
            @Parameter(name = "noticeNo", description = "공지사항번호 (기본값: 1)")
    })
    @Operation(summary = "공지사항 상세 조회", description = "공지사항 1개를 조회합니다.")
    @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
    @GetMapping("/notices/{noticeNo}")
    public BaseResponse<NoticeDetailRes> getNotice(
            @UserResolve User user,
            @PathVariable(value = "noticeNo") @Min(value = 1) Integer noticeNo) {
        return BaseResponseFactory.create(adminBulletinBoardService.getNotice(user, noticeNo));
    }

    @ApiOrder(5)
    @Parameters({
            @Parameter(name = "noticeNo", description = "공지사항번호 (기본값: 1)")
    })
    @Operation(summary = "공지사항 수정", description = "공지사항을 수정합니다.")
    @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
    @PatchMapping("/notices/{noticeNo}")
    public BaseResponse<String> updateNotice(
            @UserResolve User user,
            @RequestBody @Valid NoticeUpdateReq body,
            @PathVariable(value = "noticeNo") @Min(value = 1) Integer noticeNo) {

        adminBulletinBoardService.updateNotice(user, body, noticeNo);

        return BaseResponseFactory.create("success");
    }

    @ApiOrder(6)
    @Parameters({
            @Parameter(name = "noticeNo", description = "공지사항번호 (기본값: 1)")
    })
    @Operation(summary = "공지사항 삭제", description = "공지사항을 삭제합니다.")
    @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
    @DeleteMapping("/notices/{noticeNo}")
    public BaseResponse<String> deleteNotice(
            @UserResolve User user,
            @PathVariable(value = "noticeNo") @Min(value = 1) Integer noticeNo) {

        adminBulletinBoardService.deleteNotice(user, noticeNo);

        return BaseResponseFactory.create("success");
    }

    @ApiOrder(7)
    @Operation(summary = "공지사항 첨부 파일 업로드", description = "공지사항 첨부 파일을 업로드합니다. 20MB 이하의 jpg, png만 허용")
    @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
    @PostMapping(value = "/notice/upload-file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public BaseResponse<FileUploadRes> uploadFile(
            @UserResolve User user,
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
            throw AppErrorException.of(ResultCode.Error.FILE_FORMAT_ERROR);
        }

        if (file.getSize() > MAX_SIZE) {
            throw AppErrorException.of(ResultCode.Error.FILE_SIZE_ERROR);
        }

        return BaseResponseFactory.create(adminFileService.uploadFile(user, file, true));
    }

    @ApiOrder(8)
    @Operation(summary = "공지사항 첨부 파일 다운로드", description = "공지사항 첨부 파일을 다운로드합니다.")
    @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
    @PostMapping("/notice/download-file/{fileNo}")
    public ResponseEntity<byte[]> downloadFile(
            @UserResolve User user,
            @PathVariable(value = "fileNo") @Min(value = 1) Long fileNo) {

        FileDownloadRes fileDownloadRes = adminFileService.downloadFile(user, fileNo);

        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        httpHeaders.setContentLength(fileDownloadRes.fileData().length);
        httpHeaders.setContentDispositionFormData("attachment", fileDownloadRes.fileNm());

        return new ResponseEntity<>(fileDownloadRes.fileData(), httpHeaders, HttpStatus.OK);
    }

    @ApiOrder(9)
    @Operation(summary = "공지사항 팝업 조회", description = "공지사항 팝업을 조회합니다. 없으면 data에 null 리턴합니다.")
    @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
    @GetMapping("/notices/popup")
    public BaseResponse<PopupRes> getPopup(@UserResolve User user) {
        return BaseResponseFactory.create(adminBulletinBoardService.getPopup(user));
    }

    /*******************************************************************
     * 
     * FAQ
     * 
     *******************************************************************/
    @ApiOrder(9)
    @Operation(summary = "FAQ 필터 조회", description = "FAQ 필터를 조회합니다.")
    @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
    @GetMapping("/faq/filter")
    public BaseResponse<FaqFilterRes> getFaqFilter() {

        return BaseResponseFactory.create(adminBulletinBoardService.getFaqFilter());
    }

    @ApiOrder(10)
    @Parameters({
            @Parameter(name = "page", description = "페이지 번호 (기본값: 1)"),
            @Parameter(name = "size", description = "페이지당 항목 수 (기본값: 20)"),
            @Parameter(name = "category", description = "카테고리"),
            @Parameter(name = "keyword", description = "제목 또는 내용")
    })
    @Operation(summary = "FAQ 목록 조회 - with pagination", description = "FAQ 목록을 조회합니다.")
    @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
    @GetMapping("/faq")
    public BaseResponse<PaginationRes<FaqRes>> getAllFaq(
            @RequestParam(value = "page", required = false, defaultValue = "1") @Min(value = 1) int page,
            @RequestParam(value = "size", required = false, defaultValue = "20") @Min(value = 1) int size,
            @RequestParam(value = "category", required = false, defaultValue = "") String category,
            @RequestParam(value = "keyword", required = false, defaultValue = "") String keyword) {

        return BaseResponseFactory.create(adminBulletinBoardService.getAllFaq(page, size, category, keyword));
    }

    @ApiOrder(11)
    @Operation(summary = "FAQ 등록", description = "FAQ를 등록합니다")
    @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
    @PostMapping("/faq")
    public BaseResponse<String> createFaq(
            @RequestBody() @Valid FaqCreateReq body) {

        adminBulletinBoardService.createFaq(body);

        return BaseResponseFactory.create("success");
    }

    @ApiOrder(12)
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

    @ApiOrder(13)
    @Parameters({
            @Parameter(name = "faqNo", description = "FAQ 번호 (기본값: 1)")
    })
    @Operation(summary = "FAQ 수정", description = "FAQ를 수정합니다.")
    @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
    @PatchMapping("/faq/{faqNo}")
    public BaseResponse<String> updateFaq(
            @RequestBody @Valid FaqUpdateReq body,
            @PathVariable(value = "faqNo") @Min(value = 1) Integer faqNo) {

        adminBulletinBoardService.updateFaq(body, faqNo);

        return BaseResponseFactory.create("success");
    }

    @ApiOrder(14)
    @Parameters({
            @Parameter(name = "faqNo", description = "FAQ 번호 (기본값: 1)")
    })
    @Operation(summary = "FAQ 삭제", description = "FAQ를 삭제합니다.")
    @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
    @DeleteMapping("/faq/{faqNo}")
    public BaseResponse<String> deleteFaq(
            @PathVariable(value = "faqNo") @Min(value = 1) Integer faqNo) {

        adminBulletinBoardService.deleteFaq(faqNo);

        return BaseResponseFactory.create("success");
    }
}