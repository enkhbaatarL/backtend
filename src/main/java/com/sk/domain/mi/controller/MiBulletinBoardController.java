package com.sk.domain.mi.controller;

import com.sk.common.annotation.ApiOrder;
import com.sk.common.annotation.RequiredPermission;
import com.sk.common.model.BaseResponse;
import com.sk.common.model.BaseResponseFactory;
import com.sk.common.model.PaginationRes;
import com.sk.common.utils.RoleUtil;
import com.sk.domain.admin.dto.response.bbs.FaqFilterRes;
import com.sk.domain.admin.dto.response.bbs.NoticeFilterRes;
import com.sk.domain.admin.dto.response.bbs.PopupRes;
import com.sk.domain.mi.dto.response.bbs.MiFaqRes;
import com.sk.domain.mi.dto.response.bbs.MiNoticeDetailRes;
import com.sk.domain.mi.dto.response.bbs.MiNoticeRes;
import com.sk.domain.mi.service.MiBulletinBoardService;
import com.sk.domain.shared.enums.Role;
import com.sk.domain.shared.service.permissions.AuthorizationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Profile("!dedicated")
@Tag(name = "07. 게시판 API", description = "게시판 API 입니다.")
@Slf4j
@RestController
@RequestMapping("/api/v1/mi/bulletinboard")
@RequiredArgsConstructor
public class MiBulletinBoardController {
    private final AuthorizationService authorizationService;
    private final MiBulletinBoardService miBulletinBoardService;

    /*******************************************************************
     *
     * 공지사항
     *
     *******************************************************************/

    @ApiOrder(1)
    @Operation(summary = "공지사항 필터 조회", description = "공지사항 필터를 조회합니다.")
    @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
    @GetMapping("/notice/filter")
    @RequiredPermission(roles = {Role.ADMIN, Role.USER, Role.OPERATOR})
    public BaseResponse<NoticeFilterRes> getNoticeFilter() {

        return BaseResponseFactory.create(miBulletinBoardService.getNoticeFilter());
    }

    @ApiOrder(2)
    @Parameters({
            @Parameter(name = "page", description = "페이지번호 (기본값: 1)"),
            @Parameter(name = "size", description = "페이지당항목수 (기본값: 20)"),
            @Parameter(name = "category", description = "카테고리"),
            @Parameter(name = "keyword", description = "검색키워드")
    })
    @Operation(summary = "공지사항 목록 조회 - with pagination", description = "공지사항 목록을 조회합니다.")
    @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
    @GetMapping("/notice")
    @RequiredPermission(roles = {Role.ADMIN, Role.USER, Role.OPERATOR})
    public BaseResponse<PaginationRes<MiNoticeRes>> getAllNotices(
            @RequestParam(value = "customerCompanyNo", required = true, defaultValue = "1") @Min(value = 1) int customerCompanyNo,
            @RequestParam(value = "page", required = false, defaultValue = "1") @Min(value = 1) int page,
            @RequestParam(value = "size", required = false, defaultValue = "20") @Min(value = 1) int size,
            @RequestParam(value = "category", required = false, defaultValue = "") String category,
            @RequestParam(value = "keyword", required = false, defaultValue = "") String keyword) {

        int userNo = authorizationService.getUserNo();
        Role role = authorizationService.getCurrentRole();
        int cNo = RoleUtil.isAdmin(role) ? customerCompanyNo : authorizationService.getCompanyNo();

        return BaseResponseFactory.create(miBulletinBoardService.getAllNotices(cNo, userNo, role, page, size, category, keyword));
    }

    @ApiOrder(3)
    @Parameters({
            @Parameter(name = "customerCompanyNo", description = "조회할 고객사 번호. 일반 사용자는 로그인한 고객사 기준으로 조회합니다."),
            @Parameter(name = "noticeNo", description = "공지사항 번호")
    })
    @Operation(
            summary = "공지사항 상세 조회",
            description = """
                    현재 게시기간에 포함되고, 전체 게시이거나 조회 고객사에 공개된 공지사항 1건을 조회합니다.

                    조회 범위 밖이거나 삭제된 공지사항은 `RSC-001`로 처리하며, 읽음 기록을 남기지 않습니다.
                    """)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 가능한 공지사항 상세 반환"),
            @ApiResponse(responseCode = "400", description = "조회 범위 밖이거나 존재하지 않는 공지사항 (`RSC-001`)")
    })
    @GetMapping("/notices/{noticeNo}")
    @RequiredPermission(roles = {Role.ADMIN, Role.USER, Role.OPERATOR})
    public BaseResponse<MiNoticeDetailRes> getNotice(
            @RequestParam(value = "customerCompanyNo", required = true, defaultValue = "1") @Min(value = 1) int customerCompanyNo,
            @PathVariable(value = "noticeNo") @Min(value = 1) Integer noticeNo) {

        int userNo = authorizationService.getUserNo();
        Role role = authorizationService.getCurrentRole();
        int cNo = RoleUtil.isAdmin(role) ? customerCompanyNo : authorizationService.getCompanyNo();

        return BaseResponseFactory.create(miBulletinBoardService.getNotice(cNo, userNo, noticeNo));
    }

    @ApiOrder(4)
    @Operation(summary = "공지사항 팝업 조회", description = "공지사항 팝업을 조회합니다. 없으면 data에 null 리턴합니다.")
    @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
    @GetMapping("/notices/popup")
    @RequiredPermission(roles = {Role.ADMIN, Role.USER, Role.OPERATOR})
    public BaseResponse<PopupRes> getPopup(
            @RequestParam(value = "customerCompanyNo", required = true, defaultValue = "1") @Min(value = 1) int customerCompanyNo) {

        Role role = authorizationService.getCurrentRole();
        int cNo = RoleUtil.isAdmin(role) ? customerCompanyNo : authorizationService.getCompanyNo();

        return BaseResponseFactory.create(miBulletinBoardService.getPopup(cNo));
    }

    // todo : 알림용 공지사항 리스트 조회

    /*******************************************************************
     *
     * FAQ
     *
     *******************************************************************/
    @ApiOrder(11)
    @Operation(summary = "FAQ 필터 조회", description = "FAQ 필터를 조회합니다.")
    @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
    @GetMapping("/faq/filter")
    @RequiredPermission(roles = {Role.ADMIN, Role.USER, Role.OPERATOR})
    public BaseResponse<FaqFilterRes> getFaqFilter() {

        return BaseResponseFactory.create(miBulletinBoardService.getFaqFilter());
    }

    @ApiOrder(12)
    @Parameters({
            @Parameter(name = "page", description = "페이지번호 (기본값: 1)"),
            @Parameter(name = "size", description = "페이지당항목수 (기본값: 20)"),
            @Parameter(name = "category", description = "카테고리"),
            @Parameter(name = "keyword", description = "검색키워드")
    })
    @Operation(summary = "FAQ 목록 조회 - with pagination", description = "FAQ 목록을 조회합니다.")
    @ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
    @GetMapping("/faq")
    @RequiredPermission(roles = {Role.ADMIN, Role.USER, Role.OPERATOR})
    public BaseResponse<PaginationRes<MiFaqRes>> getAllFaq(
            @RequestParam(value = "page", required = false, defaultValue = "1") @Min(value = 1) int page,
            @RequestParam(value = "size", required = false, defaultValue = "20") @Min(value = 1) int size,
            @RequestParam(value = "category", required = false, defaultValue = "") String category,
            @RequestParam(value = "keyword", required = false, defaultValue = "") String keyword) {

        return BaseResponseFactory.create(miBulletinBoardService.getAllFaq(page, size, category, keyword));
    }
}
