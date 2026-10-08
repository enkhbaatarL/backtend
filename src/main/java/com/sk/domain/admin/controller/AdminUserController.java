package com.sk.domain.admin.controller;

import java.util.List;
import java.beans.PropertyEditorSupport;

import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.sk.common.annotation.AccessIpCheck;
import com.sk.common.annotation.ApiOrder;
import com.sk.common.annotation.RequiredPermission;
import com.sk.common.annotation.TrimmedNotEmpty;
import com.sk.common.config.security.mcp.McpClientAuthenticationToken;
import com.sk.common.config.security.mcp.McpCredentialVerifier;
import com.sk.common.enums.ResultCode;
import com.sk.common.exception.AppErrorException;
import com.sk.common.exception.AppHttpStatusException;
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
import com.sk.domain.shared.enums.Claim;
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
    // [MCP 인증] by-mcp-user-id 의 MCP 키 검증
    private final McpCredentialVerifier mcpCredentialVerifier;

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
            관리자 또는 고객사 클라이언트가 로그인 아이디로 사용자 한 명을 조회합니다.
            기존 GET /users/{userNo}와 같은 상세 응답이며 개인정보 동의 일시와 등록·수정 이력을 포함합니다.
            개인 ADMIN은 허용 IP에서 기존 전체 고객사 조회 범위를 유지합니다.
            API_PLUS_OPERATOR는 토큰 companyCd와 같은 회사의 USER 또는 회사와 무관하게
            isOtherServiceAccessPossible=true인 ADMIN만 조회합니다. OPERATOR는 조회하지 않습니다.
            """, security = @SecurityRequirement(name = "BearerAuth"))
    @ApiResponse(responseCode = "200", description = "기존 번호 상세와 동일한 사용자 상세. 다건 일치도 첫 행 한 건 반환",
            useReturnTypeSchema = true, content = @Content(mediaType = "application/json"))
    @GetMapping(value = "/users/by-login-id", produces = "application/json")
    @RequiredPermission(roles = {Role.ADMIN, Role.API_PLUS_OPERATOR})
    public BaseResponse<UserDetailRes> getUserByLoginId(
            @Parameter(description = "로그인 ID 문자열. 이메일 형식으로 제한하지 않습니다. 앞뒤 공백·제어문자는 불허하며 "
                    + "숫자 ID의 앞 0과 대소문자를 보존합니다. URL 인코딩하며 +는 %2B로 전달합니다.",
                    required = true, schema = @Schema(type = "string", minLength = 1, maxLength = 50,
                            example = "sample.user@example.com"))
            @RequestParam("userLoginId") @NotBlank @Size(min = 1, max = 50) String userLoginId,
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt) {
        validateLoginId(userLoginId);
        return BaseResponseFactory.create(findUserByLoginId(userLoginId, jwt));
    }

    // [MCP 인증] by-login-id + (1) X-MCP-Authorization 키 검증 (2) Bearer 토큰 회사 == 키 회사
    @ApiOrder(13)
    @Operation(summary = "MCP 인증 키와 로그인 ID로 사용자 상세 조회", description = """
            로그인 ID로 사용자 상세 조회(GET /users/by-login-id)와 같은 업무를 수행하되, 먼저 MCP 인증 키를 검증합니다.
            1. X-MCP-Authorization 헤더의 MCP 인증 키(Basic base64(id:secret))를 검증합니다.
               형식 오류는 400(REQ-003), 인증정보 불일치는 401(AUTH-002)입니다.
            2. Bearer 토큰의 회사와 MCP 인증 키의 회사가 같아야 합니다. 다르면 403(AUTH-007)입니다.
            3. 이후 by-login-id 와 같은 권한 범위로 사용자를 조회합니다.
            MCP 인증 키는 쿼리 파라미터가 아닌 헤더로 받습니다. 쿼리 문자열은 요청 로그에 남기 때문입니다.
            """, security = @SecurityRequirement(name = "BearerAuth"))
    @ApiResponse(responseCode = "200", description = "by-login-id 와 같은 사용자 상세. 다건 일치도 첫 행 한 건 반환",
            useReturnTypeSchema = true, content = @Content(mediaType = "application/json"))
    @GetMapping(value = "/users/by-mcp-user-id", produces = "application/json")
    @RequiredPermission(roles = {Role.ADMIN, Role.API_PLUS_OPERATOR})
    public BaseResponse<UserDetailRes> getUserByMcpUserId(
            @Parameter(in = ParameterIn.HEADER, name = McpCredentialVerifier.MCP_AUTHORIZATION_HEADER, required = true,
                    description = "MCP 인증 키. MCP API 에 보내는 Authorization 과 같은 'Basic base64(id:secret)' 형식입니다.",
                    schema = @Schema(type = "string", example = "Basic c2tjYy1tY3AwMTpzZWNyZXQ="))
            @RequestHeader(McpCredentialVerifier.MCP_AUTHORIZATION_HEADER) String mcpAuthorization,
            @Parameter(description = "로그인 ID 문자열. by-login-id 의 userLoginId 와 같은 규칙입니다.",
                    required = true, schema = @Schema(type = "string", minLength = 1, maxLength = 50,
                            example = "sample.user@example.com"))
            @RequestParam("userLoginId") @NotBlank @Size(min = 1, max = 50) String userLoginId,
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt) {
        McpClientAuthenticationToken mcpClient = verifyMcpAuthorization(mcpAuthorization);
        validateSameCompany(mcpClient, jwt);
        validateLoginId(userLoginId);
        return BaseResponseFactory.create(findUserByLoginId(userLoginId, jwt));
    }

    // [MCP 인증] 형식 오류 400, 인증정보 불일치 401
    private McpClientAuthenticationToken verifyMcpAuthorization(String mcpAuthorization) {
        try {
            return mcpCredentialVerifier.verifyBasic(mcpAuthorization);
        } catch (IllegalArgumentException e) {
            throw AppErrorException.of(ResultCode.Error.REQ_INVALID_PARAMETER, e.getMessage());
        } catch (AuthenticationException e) {
            throw AppHttpStatusException.of(HttpStatus.UNAUTHORIZED, ResultCode.Error.AUTH_INVALID_CREDENTIALS, e.getMessage());
        }
    }

    // [MCP 인증] Bearer 토큰 companyNo 와 키 회사가 다르면 403
    private void validateSameCompany(McpClientAuthenticationToken mcpClient, Jwt jwt) {
        Object tokenCompanyNo = jwt.getClaim(Claim.COMPANY_NO.getCode());
        if (tokenCompanyNo == null || !tokenCompanyNo.toString().equals(String.valueOf(mcpClient.getCustomerCompanyNo()))) {
            throw AppHttpStatusException.of(HttpStatus.FORBIDDEN, ResultCode.Error.AUTH_ACCESS_DENIED,
                    "The bearer token company does not match the MCP key company.");
        }
    }

    // [MCP 인증] by-login-id 의 기존 검사를 그대로 추출 (by-login-id 동작 변경 없음)
    private void validateLoginId(String userLoginId) {
        if (userLoginId.codePoints().anyMatch(Character::isISOControl)
                || Character.isSpaceChar(userLoginId.codePointAt(0))
                || Character.isSpaceChar(userLoginId.codePointBefore(userLoginId.length()))) {
            throw AppErrorException.of(ResultCode.Error.REQ_INVALID_PARAMETER);
        }
    }

    // [MCP 인증] by-login-id 의 기존 조회 분기를 그대로 추출 (by-login-id 동작 변경 없음)
    private UserDetailRes findUserByLoginId(String userLoginId, Jwt jwt) {
        Role callerRole = Role.valueOf(jwt.getClaimAsString(Claim.ROLE.getCode()));
        return callerRole == Role.ADMIN
                ? adminUserService.getUserByLoginId(userLoginId)
                : adminUserService.getUserByLoginIdForClient(userLoginId, jwt.getClaimAsString(Claim.COMPANY_CD.getCode()));
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
