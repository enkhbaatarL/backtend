package com.sk.domain.admin.controller;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
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
import com.sk.common.annotation.RequirePermission;
import com.sk.common.annotation.UserResolve;
import com.sk.common.enums.ResultCode;
import com.sk.common.exception.AppErrorException;
import com.sk.common.model.BaseResponse;
import com.sk.common.model.BaseResponseFactory;
import com.sk.common.model.PaginationRes;
import com.sk.domain.admin.dto.response.bbs.FaqFilterRes;
import com.sk.domain.admin.dto.response.bbs.FaqRes;
import com.sk.domain.admin.dto.response.bbs.NoticeDetailRes;
import com.sk.domain.mi.dto.response.bbs.MiFaqRes;
import com.sk.domain.mi.dto.response.bbs.MiNoticeDetailRes;
import com.sk.domain.mi.dto.response.bbs.MiNoticeRes;
import com.sk.domain.mi.service.MiBulletinBoardService;
import com.sk.domain.shared.entity.master.tcm.BulletinBoard;
import com.sk.domain.shared.entity.master.tmd.User;
import com.sk.domain.shared.enums.FileRelationshipObjectSeparateCode;
import com.sk.domain.shared.enums.Role;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Tag(name = "04. 게시판 API", description = "게시판 API 입니다.")
@Slf4j
@RestController
@RequestMapping("/api/v1/mi/bulletinboard")
@RequiredArgsConstructor
public class MiBulletinBoardController {
    private final MiBulletinBoardService miBulletinBoardService;

    /*******************************************************************
     * 
     * 공지사항
     * 
     *******************************************************************/

    @ApiOrder(1)
    @Parameters({
            @Parameter(name = "page", description = "페이지 번호 (기본값: 1)"),
            @Parameter(name = "size", description = "페이지당 항목 수 (기본값: 20)"),
            @Parameter(name = "category", description = "카테고리"),
            @Parameter(name = "keyword", description = "제목 또는 내용")
    })
    @Operation(summary = "공지사항 목록 조회 - with pagination", description = "공지사항 목록을 조회합니다.")
    @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
    @GetMapping("/notice")
    public BaseResponse<PaginationRes<MiNoticeRes>> getAllNotices(
            @UserResolve User user,
            @RequestParam(value = "page", required = false, defaultValue = "1") @Min(value = 1) int page,
            @RequestParam(value = "size", required = false, defaultValue = "20") @Min(value = 1) int size,
            @RequestParam(value = "customerCompanyNo", required = true, defaultValue = "1") @Min(value = 1) int customerCompanyNo,
            @RequestParam(value = "category", required = false, defaultValue = "") String category,
            @RequestParam(value = "keyword", required = false, defaultValue = "") String keyword) {

        return BaseResponseFactory.create(miBulletinBoardService.getAllNotices(user, page, size, customerCompanyNo, category, keyword));
    }

    @ApiOrder(2)
    @Parameters({
            @Parameter(name = "noticeNo", description = "공지사항번호 (기본값: 1)")
    })
    @Operation(summary = "공지사항 상세 조회", description = "공지사항 1개를 조회합니다.")
    @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
    @GetMapping("/notices/{noticeNo}")
    public BaseResponse<MiNoticeDetailRes> getNotice(
            @UserResolve User user,
            @RequestParam(value = "customerCompanyNo", required = true, defaultValue = "1") @Min(value = 1) int customerCompanyNo,
            @PathVariable(value = "noticeNo") @Min(value = 1) Integer noticeNo) {
        return BaseResponseFactory.create(miBulletinBoardService.getNotice(user, customerCompanyNo, noticeNo));
    }

    /*******************************************************************
     * 
     * FAQ
     * 
     *******************************************************************/

    @ApiOrder(3)
    @Parameters({
            @Parameter(name = "page", description = "페이지 번호 (기본값: 1)"),
            @Parameter(name = "size", description = "페이지당 항목 수 (기본값: 20)"),
            @Parameter(name = "category", description = "카테고리"),
            @Parameter(name = "keyword", description = "제목 또는 내용")
    })
    @Operation(summary = "FAQ 목록 조회 - with pagination", description = "FAQ 목록을 조회합니다.")
    @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
    @GetMapping("/faq")
    public BaseResponse<PaginationRes<MiFaqRes>> getAllFaq(
            @RequestParam(value = "page", required = false, defaultValue = "1") @Min(value = 1) int page,
            @RequestParam(value = "size", required = false, defaultValue = "20") @Min(value = 1) int size,
            @RequestParam(value = "category", required = false, defaultValue = "") String category,
            @RequestParam(value = "keyword", required = false, defaultValue = "") String keyword) {

        return BaseResponseFactory.create(miBulletinBoardService.getAllFaq(page, size, category, keyword));
    }
}