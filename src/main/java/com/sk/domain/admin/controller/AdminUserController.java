package com.sk.domain.admin.controller;

import java.util.List;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.sk.common.annotation.ApiOrder;
import com.sk.common.annotation.TrimmedNotEmpty;
import com.sk.common.model.BaseResponse;
import com.sk.common.model.BaseResponseFactory;
import com.sk.common.model.PaginationRes;
import com.sk.domain.admin.dto.request.AdminReq;
import com.sk.domain.admin.dto.request.UserReq;
import com.sk.domain.admin.dto.response.AdminRes;
import com.sk.domain.admin.dto.response.UserInfoRes;
import com.sk.domain.admin.dto.response.UserRes;
import com.sk.domain.admin.dto.response.UserWithCompanyRes;
import com.sk.domain.admin.dto.response.UserWithCreatorRes;
import com.sk.domain.admin.service.AdminUserService;
import com.sk.domain.shared.enums.UserRoleCd;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

//TODO: 호영:고객사 등록 시 사용자 목록에 모든 유저가 나와야 하는지 아니면 등록되지 않은 유저만 나와야 하는지

@Tag(name = "02. 사용자 API", description = "사용자 API 입니다.")
@Slf4j
@RestController
@RequestMapping("/api/v1/admin/user-management")
@ApiResponse(responseCode = "200", description = "HTTP Status Code is 200")
@RequiredArgsConstructor
public class AdminUserController {

    private final AdminUserService adminUserService;

    @ApiOrder(1)
    @Parameters({
            @Parameter(name = "page", description = "페이지 번호 (기본값: 1)"),
            @Parameter(name = "size", description = "페이지당 항목 수 (기본값: 10)"),
            @Parameter(name = "companyCd", description = "검색할 고객사 코드"),
            @Parameter(name = "userNm", description = "검색할 사용자 이름"),
            @Parameter(name = "emailAddress", description = "검색할 이메일 주소")
    })
    @Operation(summary = "그룹사 사용자 목록 인터페이스 테이블 조회 API", description = "사용자 목록을 인터페이스 테이블에서 조회합니다.")
    @GetMapping("/group-users")
    public BaseResponse<PaginationRes<UserInfoRes>> getUserInfos(
            @RequestParam(value = "page", required = false, defaultValue = "1") @Min(value = 1) int page,
            @RequestParam(value = "size", required = false, defaultValue = "10") @Min(value = 1) int size,
            @RequestParam(value = "companyCd", required = true) String companyCd,
            @RequestParam(value = "userNm", required = false) @TrimmedNotEmpty String userNm,
            @RequestParam(value = "emailAddress", required = false) @TrimmedNotEmpty String emailAddress) {
        return BaseResponseFactory
                .create(adminUserService.getUserInfos(page, size, companyCd, userNm, emailAddress));
    }

    @ApiOrder(2)
    @Operation(summary = "사용자 등록 API", description = "사용자 정보를 등록합니다.")
    @PostMapping("/user")
    public BaseResponse<List<UserRes>> postUserInfo(
            @RequestBody @Valid List<UserReq.Insert> userReq) {
        return BaseResponseFactory.create(adminUserService.postUserInfo(userReq));
    }

    @ApiOrder(3)
    @Parameters({
            @Parameter(name = "page", description = "페이지 번호 (기본값: 1)"),
            @Parameter(name = "size", description = "페이지당 항목 수 (기본값: 10)"),
            @Parameter(name = "userRoleCd", description = "사용자 역할 코드"),
            @Parameter(name = "companyNo", description = "검색할 고객사 번호"),
            @Parameter(name = "departmentNm", description = "검색할 부서 이름"),
            @Parameter(name = "userNm", description = "검색할 사용자 이름"),
            @Parameter(name = "emailAddress", description = "검색할 이메일 주소")
    })
    @Operation(summary = "사용자 목록 조회 API", description = "사용자 목록을 조회합니다.")
    @GetMapping("/users")
    public BaseResponse<PaginationRes<UserWithCreatorRes>> getUsers(
            @RequestParam(value = "page", required = false, defaultValue = "1") @Min(value = 1) int page,
            @RequestParam(value = "size", required = false, defaultValue = "10") @Min(value = 1) int size,
            @RequestParam(value = "userRoleCd", required = true) UserRoleCd userRoleCd,
            @RequestParam(value = "companyNo", required = false) Integer companyNo,
            @RequestParam(value = "departmentNm", required = false) @TrimmedNotEmpty String departmentNm,
            @RequestParam(value = "userNm", required = false) @TrimmedNotEmpty String userNm,
            @RequestParam(value = "emailAddress", required = false) @TrimmedNotEmpty String emailAddress) {
        return BaseResponseFactory
                .create(adminUserService.getUsers(page, size, userRoleCd, companyNo, departmentNm, userNm,
                        emailAddress));
    }

    @ApiOrder(4)
    @Parameters({
            @Parameter(name = "userNo", description = "사용자 번호")
    })
    @Operation(summary = "사용자 상세 조회 API", description = "사용자 상세 정보를 조회합니다.")
    @GetMapping("/users/{userNo}")
    public BaseResponse<UserWithCompanyRes> getUser(
            @PathVariable(value = "userNo") Integer userNo) {
        return BaseResponseFactory.create(adminUserService.getUser(userNo));
    }

    @ApiOrder(5)
    @Parameters({
            @Parameter(name = "userNo", description = "사용자 번호")
    })
    @Operation(summary = "사용자 등록 삭제 API", description = "사용자 정보를 삭제합니다.")
    @DeleteMapping("/users/{userNo}")
    public BaseResponse<UserRes> deleteUser(
            @PathVariable(value = "userNo") Integer userNo) {
        return BaseResponseFactory.create(adminUserService.deleteUser(userNo));
    }

    @ApiOrder(6)
    @Operation(summary = "어드민 관리자 등록 API", description = "어드민 관리자 정보를 등록합니다.")
    @PostMapping("/admin-user")
    public BaseResponse<UserRes> postAdminUser(
            @RequestBody @Valid AdminReq.Insert adminReq) {
        return BaseResponseFactory.create(adminUserService.postAdminUser(adminReq));
    }

    @ApiOrder(7)
    @Parameters({
            @Parameter(name = "userNo", description = "사용자 번호")
    })
    @Operation(summary = "어드민 관리자 상세 조회 API", description = "어드민 관리자 상세 정보를 조회합니다.")
    @GetMapping("/admin-users/{userNo}")
    public BaseResponse<AdminRes> getAdminUser(
            @PathVariable(value = "userNo") Integer userNo) {
        return BaseResponseFactory.create(adminUserService.getAdminUser(userNo));
    }

    @ApiOrder(8)
    @Parameters({
            @Parameter(name = "userNo", description = "사용자 번호")
    })
    @Operation(summary = "어드민 관리자 수정 API", description = "어드민 관리자 정보를 수정합니다. (접근 허용 IP는 수정되는 값이 아닌 적용 되는 값 모두 필요)")
    @PatchMapping("/admin-users/{userNo}")
    public BaseResponse<UserRes> patchAdminUser(
            @PathVariable(value = "userNo") Integer userNo,
            @RequestBody @Valid AdminReq.Update adminReq) {
        return BaseResponseFactory.create(adminUserService.patchAdminUser(userNo, adminReq));
    }

    @ApiOrder(9)
    @Parameters({
            @Parameter(name = "userNo", description = "사용자 번호")
    })
    @Operation(summary = "어드민 관리자 삭제 API", description = "어드민 관리자 정보를 삭제합니다.")
    @DeleteMapping("/admin-users/{userNo}")
    public BaseResponse<UserRes> deleteAdminUser(
            @PathVariable(value = "userNo") Integer userNo) {
        return BaseResponseFactory.create(adminUserService.deleteAdminUser(userNo));
    }
}