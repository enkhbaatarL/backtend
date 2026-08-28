package com.sk.domain.admin.controller;

import java.util.List;
import java.beans.PropertyEditorSupport;

import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.sk.common.annotation.AccessIpCheck;
import com.sk.common.annotation.ApiOrder;
import com.sk.common.annotation.RequiredPermission;
import com.sk.common.annotation.TrimmedNotEmpty;
import com.sk.common.model.BaseResponse;
import com.sk.common.model.BaseResponseFactory;
import com.sk.common.model.PaginationRes;
import com.sk.domain.admin.dto.request.user.AdminReq;
import com.sk.domain.admin.dto.request.user.UserReq;
import com.sk.domain.admin.dto.response.user.AdminRes;
import com.sk.domain.admin.dto.response.user.UserDetailRes;
import com.sk.domain.admin.dto.response.user.UserInfoRes;
import com.sk.domain.admin.dto.response.user.UserRes;
import com.sk.domain.admin.service.AdminUserService;
import com.sk.domain.shared.enums.Role;
import com.sk.domain.shared.enums.UserRoleCd;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Profile("!dedicated")
@AccessIpCheck
@Tag(name = "02. 사용자 API", description = "사용자 API 입니다.")
@Slf4j
@RestController
@RequestMapping("/api/v1/admin/user-management")
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
    @ApiResponse(responseCode = "200", description = "사용자 목록 조회 성공")
    @GetMapping("/group-users")
    @RequiredPermission(roles = {Role.ADMIN})
    public BaseResponse<PaginationRes<UserInfoRes>> getUserInfos(
            @RequestParam(value = "page", required = false, defaultValue = "1") @Min(value = 1) int page,
            @RequestParam(value = "size", required = false, defaultValue = "10") @Min(value = 1) int size,
            @RequestParam(value = "companyCd", required = true) String companyCd,
            @RequestParam(value = "userNm", required = false) @TrimmedNotEmpty String userNm,
            @RequestParam(value = "emailAddress", required = false) @TrimmedNotEmpty String emailAddress) {
        return BaseResponseFactory
                .create(adminUserService.getUserInfos(page, size, companyCd, userNm, emailAddress));
    }

    /**
     * 2025-06-11 사용 안함
    @ApiOrder(2)
    @Parameters({
            @Parameter(name = "page", description = "페이지 번호 (기본값: 1)"),
            @Parameter(name = "size", description = "페이지당 항목 수 (기본값: 10)"),
            @Parameter(name = "companyType", description = "검색할 회사 타입, 고객사 => CUSTOMER, 관리사 => MANAGER"),
            @Parameter(name = "companyCd", description = "검색할 고객사 코드"),
            @Parameter(name = "userNm", description = "검색할 사용자 이름"),
            @Parameter(name = "emailAddress", description = "검색할 이메일 주소")
    })
    @Operation(summary = "그룹사 운영자 / 관리자 목록 인터페이스 테이블 조회 API", description = "운영자 / 관리자 목록을 인터페이스 테이블에서 조회합니다.")
    @ApiResponse(responseCode = "200", description = "사용자 목록 조회 성공")
    @GetMapping("/group-admin-users")
    public BaseResponse<PaginationRes<UserInfoRes>> getAdminUserInfos(
            @RequestParam(value = "page", required = false, defaultValue = "1") @Min(value = 1) int page,
            @RequestParam(value = "size", required = false, defaultValue = "10") @Min(value = 1) int size,
            @RequestParam(value = "companyType", required = true) CompanyType companyType,
            @RequestParam(value = "companyCd", required = true) String companyCd,
            @RequestParam(value = "userNm", required = false) @TrimmedNotEmpty String userNm,
            @RequestParam(value = "emailAddress", required = false) @TrimmedNotEmpty String emailAddress) {
        return BaseResponseFactory
                .create(adminUserService.getAdminUserInfos(page, size, companyType, companyCd, userNm, emailAddress));
    }
    */

    @ApiOrder(3)
    @Operation(summary = "사용자 등록 API", description = "사용자 정보를 등록합니다.")
    @ApiResponse(responseCode = "200", description = "사용자 등록 성공")
    @PostMapping("/users")
	@RequiredPermission(roles = {Role.ADMIN})
    public BaseResponse<List<UserRes>> saveUserInfo(
            @RequestBody @Valid UserReq.Insert userReq) {
        return BaseResponseFactory.create(adminUserService.saveUserInfo(userReq));
    }

    @ApiOrder(4)
    @Parameters({
            @Parameter(name = "page", description = "페이지 번호 (기본값: 1)"),
            @Parameter(name = "size", description = "페이지당 항목 수 (기본값: 10)"),
            @Parameter(name = "companyNo", description = "검색할 고객사 번호"),
            @Parameter(name = "departmentNm", description = "검색할 부서 이름"),
            @Parameter(name = "userNm", description = "검색할 사용자 이름"),
            @Parameter(name = "emailAddress", description = "검색할 이메일 주소")
    })
    @Operation(summary = "사용자 목록 조회 API", description = "사용자 목록을 조회합니다.")
    @ApiResponse(responseCode = "200", description = "사용자 목록 조회 성공")
    @GetMapping("/users")
	@RequiredPermission(roles = {Role.ADMIN})
    public BaseResponse<PaginationRes<UserDetailRes>> getUsers(
            @RequestParam(value = "page", required = false, defaultValue = "1") @Min(value = 1) int page,
            @RequestParam(value = "size", required = false, defaultValue = "10") @Min(value = 1) int size,
            @RequestParam(value = "companyNo", required = false) Integer companyNo,
            @RequestParam(value = "departmentNm", required = false) @TrimmedNotEmpty String departmentNm,
            @RequestParam(value = "userNm", required = false) @TrimmedNotEmpty String userNm,
            @RequestParam(value = "emailAddress", required = false) @TrimmedNotEmpty String emailAddress) {
        return BaseResponseFactory
                .create(adminUserService.getUsers(page, size, companyNo, departmentNm, userNm,
                        emailAddress));
    }

    @ApiOrder(5)
    @Parameters({
            @Parameter(name = "userNo", description = "사용자 번호")
    })
    @Operation(summary = "사용자 상세 조회 API", description = "사용자 상세 정보를 조회합니다.")
    @ApiResponse(responseCode = "200", description = "사용자 상세 조회 성공")
    @GetMapping("/users/{userNo}")
	@RequiredPermission(roles = {Role.ADMIN})
    public BaseResponse<UserDetailRes> getUser(
            @PathVariable(value = "userNo") Integer userNo) {
        return BaseResponseFactory.create(adminUserService.getUser(userNo));
    }

    @ApiOrder(12)
    @Operation(summary = "로그인 ID로 사용자 상세 조회", description = """
            관리자가 로그인 아이디로 모든 고객사의 사용자 중 한 명을 조회합니다.
            기존 GET /users/{userNo}와 같은 상세 응답이며 개인정보 동의 일시와 등록·수정 이력을 포함합니다.
            ADMIN 인증과 호출 관리자의 허용 IP가 필요합니다. 소속 회사가 달라도 조회하며 회사 헤더로 범위를 바꾸지 않습니다.
            여러 계정이 일치하면 조회 결과의 첫 행 하나를 선택합니다. 역할 우선순위와 반복 호출의 동일 계정 선택은 보장하지 않습니다.
            조회로 사용자 설정을 변경하지 않으며 페이지 접근 가능 여부는 제공하지 않습니다.
            GET /users는 emailAddress 검색과 페이지 처리를 제공하는 기존 목록 API입니다.
            """, security = @SecurityRequirement(name = "BearerAuth"))
    @ApiResponse(responseCode = "200", description = "기존 번호 상세와 동일한 사용자 상세. 다건 일치도 첫 행 한 건 반환",
            useReturnTypeSchema = true, content = @Content(mediaType = "application/json"))
    @GetMapping(value = "/users/by-login-id", produces = "application/json")
    @RequiredPermission(roles = {Role.ADMIN})
    public BaseResponse<UserDetailRes> getUserByLoginId(
            @Parameter(description = "로그인 ID 문자열. 이메일 형식으로 제한하지 않습니다. 앞뒤 공백·제어문자는 불허하며 "
                    + "숫자 ID의 앞 0과 대소문자를 보존합니다. URL 인코딩하며 +는 %2B로 전달합니다.",
                    required = true, schema = @Schema(type = "string", minLength = 1, maxLength = 50,
                            example = "sample.user@example.com"))
            @RequestParam("userLoginId") @NotBlank @Size(min = 1, max = 50)
            @Pattern(regexp = "^[^\\p{Z}\\p{Cc}\\s](?:[^\\p{Cc}]*[^\\p{Z}\\p{Cc}\\s])?$") String userLoginId) {
        return BaseResponseFactory.create(adminUserService.getUserByLoginId(userLoginId));
    }

    @InitBinder("userLoginId")
    private void preserveLoginId(WebDataBinder binder) {
        // The shared String converter trims inputs; validate this parameter's original value.
        binder.registerCustomEditor(String.class, new PropertyEditorSupport() {
            @Override
            public void setAsText(String text) {
                setValue(text);
            }
        });
    }

    @ApiOrder(6)
    @Parameters({
            @Parameter(name = "userNo", description = "사용자 번호")
    })
    @Operation(summary = "사용자 등록 삭제 API", description = "사용자 정보를 삭제합니다.")
    @ApiResponse(responseCode = "200", description = "사용자 등록 삭제 성공")
    @DeleteMapping("/users/{userNo}")
	@RequiredPermission(roles = {Role.ADMIN})
    public BaseResponse<UserRes> deleteUser(
            @PathVariable(value = "userNo") Integer userNo) {
        return BaseResponseFactory.create(adminUserService.deleteUser(userNo));
    }

    @ApiOrder(7)
    @Operation(summary = "운영자, 관리자 등록 API", description = "운영자, 관리자를 등록합니다.")
    @ApiResponse(responseCode = "200", description = "운영자, 관리자 등록 성공")
    @PostMapping("/admin-user")
	@RequiredPermission(roles = {Role.ADMIN})
    public BaseResponse<UserRes> saveAdminUser(
            @RequestBody @Valid AdminReq.Insert adminReq) {
        return BaseResponseFactory.create(adminUserService.saveAdminUser(adminReq));
    }

    @ApiOrder(8)
    @Parameters({
            @Parameter(name = "page", description = "페이지 번호 (기본값: 1)"),
            @Parameter(name = "size", description = "페이지당 항목 수 (기본값: 10)"),
            @Parameter(name = "userRoleCd", description = "검색할 사용자 역할 코드 (ADMIN 또는 OPERATOR)", schema = @Schema(type = "string", allowableValues = {
                    "ADMIN", "OPERATOR" }, example = "ADMIN")),
            @Parameter(name = "companyNo", description = "검색할 고객사 번호"),
            @Parameter(name = "departmentNm", description = "검색할 부서 이름"),
            @Parameter(name = "userNm", description = "검색할 사용자 이름"),
            @Parameter(name = "emailAddress", description = "검색할 이메일 주소")
    })
    @Operation(summary = "운영자, 관리자 목록 조회 API", description = "운영자, 관리자 목록을 조회합니다.")
    @ApiResponse(responseCode = "200", description = "운영자, 관리자 목록 조회 성공")
    @GetMapping("/admin-users")
	@RequiredPermission(roles = {Role.ADMIN})
    public BaseResponse<PaginationRes<UserDetailRes>> getAdminUsers(
            @RequestParam(value = "page", required = false, defaultValue = "1") @Min(value = 1) int page,
            @RequestParam(value = "size", required = false, defaultValue = "10") @Min(value = 1) int size,
            @RequestParam(value = "userRoleCd", required = false) UserRoleCd userRoleCd,
            @RequestParam(value = "companyNo", required = false) Integer companyNo,
            @RequestParam(value = "departmentNm", required = false) @TrimmedNotEmpty String departmentNm,
            @RequestParam(value = "userNm", required = false) @TrimmedNotEmpty String userNm,
            @RequestParam(value = "emailAddress", required = false) @TrimmedNotEmpty String emailAddress) {
        return BaseResponseFactory
                .create(adminUserService.getAdminUsers(page, size, userRoleCd, companyNo, departmentNm, userNm,
                        emailAddress));
    }

    @ApiOrder(9)
    @Parameters({
            @Parameter(name = "userNo", description = "사용자 번호")
    })
    @Operation(summary = "운영자, 관리자 상세 조회 API", description = "운영자, 관리자 상세 정보를 조회합니다.")
    @ApiResponse(responseCode = "200", description = "운영자, 관리자 상세 조회 성공")
    @GetMapping("/admin-users/{userNo}")
	@RequiredPermission(roles = {Role.ADMIN})
    public BaseResponse<AdminRes> getAdminUser(
            @PathVariable(value = "userNo") Integer userNo) {
        return BaseResponseFactory.create(adminUserService.getAdminUser(userNo));
    }

    @ApiOrder(10)
    @Parameters({
            @Parameter(name = "userNo", description = "사용자 번호")
    })
    @Operation(summary = "운영자, 관리자 수정 API", description = "운영자, 관리자 정보를 수정합니다. (접근 허용 IP는 수정되는 값이 아닌 적용 되는 값 모두 필요)")
    @ApiResponse(responseCode = "200", description = "운영자, 관리자 수정 성공")
    @PatchMapping("/admin-users/{userNo}")
	@RequiredPermission(roles = {Role.ADMIN})
    public BaseResponse<UserRes> patchAdminUser(
            @PathVariable(value = "userNo") Integer userNo,
            @RequestBody @Valid AdminReq.Update adminReq) {
        return BaseResponseFactory.create(adminUserService.patchAdminUser(userNo, adminReq));
    }

    @ApiOrder(11)
    @Parameters({
            @Parameter(name = "userNo", description = "사용자 번호")
    })
    @Operation(summary = "운영자, 관리자 삭제 API", description = "운영자, 관리자 정보를 삭제합니다.")
    @ApiResponse(responseCode = "200", description = "운영자, 관리자 삭제 성공")
    @DeleteMapping("/admin-users/{userNo}")
	@RequiredPermission(roles = {Role.ADMIN})
    public BaseResponse<UserRes> deleteAdminUser(
            @PathVariable(value = "userNo") Integer userNo) {
        return BaseResponseFactory.create(adminUserService.deleteAdminUser(userNo));
    }
}
