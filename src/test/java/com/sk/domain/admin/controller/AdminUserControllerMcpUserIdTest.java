package com.sk.domain.admin.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.sk.common.config.security.mcp.McpClientAuthenticationToken;
import com.sk.common.config.security.mcp.McpCredentialVerifier;
import com.sk.common.exception.AppErrorException;
import com.sk.common.exception.AppHttpStatusException;
import com.sk.domain.admin.dto.response.user.UserDetailRes;
import com.sk.domain.admin.service.AdminUserService;
import com.sk.domain.shared.enums.Claim;
import com.sk.domain.shared.enums.Role;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.oauth2.jwt.Jwt;

@DisplayName("MCP 인증 키와 로그인 ID로 사용자 상세 조회 (by-mcp-user-id)")
class AdminUserControllerMcpUserIdTest {

    private static final String BASIC = "Basic c2tjYy1tY3AwMTpzZWNyZXQ=";
    private static final String LOGIN_ID = "user@a.com";

    private final AdminUserService adminUserService = mock(AdminUserService.class);
    private final McpCredentialVerifier verifier = mock(McpCredentialVerifier.class);
    private final AdminUserController controller = new AdminUserController(adminUserService, verifier);

    @Test
    @DisplayName("키 검증 → 회사 일치 확인 후 by-login-id 와 같은 고객사 범위로 조회한다")
    void findsUserForClientAfterVerification() {
        UserDetailRes detail = mock(UserDetailRes.class);
        given(verifier.verifyBasic(BASIC)).willReturn(new McpClientAuthenticationToken("skcc-mcp01", 1001, false));
        given(adminUserService.getUserByLoginIdForClient(LOGIN_ID, "SKCC")).willReturn(detail);

        assertThat(controller.getUserByMcpUserId(BASIC, LOGIN_ID, jwt(Role.API_PLUS_OPERATOR, 1001, "SKCC")).getData()).isSameAs(detail);
    }

    @Test
    @DisplayName("ADMIN 토큰이면 by-login-id 와 같은 전체 범위로 조회한다")
    void findsUserForAdmin() {
        given(verifier.verifyBasic(BASIC)).willReturn(new McpClientAuthenticationToken("sk-mcp", 1, true));

        controller.getUserByMcpUserId(BASIC, LOGIN_ID, jwt(Role.ADMIN, 1, "SK"));

        verify(adminUserService).getUserByLoginId(LOGIN_ID);
    }

    @Test
    @DisplayName("토큰 회사와 키 회사가 다르면 403 이고 사용자를 조회하지 않는다")
    void rejectsDifferentCompany() {
        given(verifier.verifyBasic(BASIC)).willReturn(new McpClientAuthenticationToken("skcc-mcp01", 1001, false));

        assertThatThrownBy(() -> controller.getUserByMcpUserId(BASIC, LOGIN_ID, jwt(Role.API_PLUS_OPERATOR, 2002, "OTHER")))
            .isInstanceOf(AppHttpStatusException.class)
            .extracting("httpStatus").isEqualTo(HttpStatus.FORBIDDEN);
        verifyNoInteractions(adminUserService);
    }

    @Test
    @DisplayName("키 인증정보가 틀리면 401 이고 사용자를 조회하지 않는다")
    void rejectsWrongCredentials() {
        given(verifier.verifyBasic(BASIC)).willThrow(new BadCredentialsException("Invalid MCP credentials."));

        assertThatThrownBy(() -> controller.getUserByMcpUserId(BASIC, LOGIN_ID, jwt(Role.API_PLUS_OPERATOR, 1001, "SKCC")))
            .isInstanceOf(AppHttpStatusException.class)
            .extracting("httpStatus").isEqualTo(HttpStatus.UNAUTHORIZED);
        verifyNoInteractions(adminUserService);
    }

    @Test
    @DisplayName("키 형식이 잘못되면 400 이고 사용자를 조회하지 않는다")
    void rejectsMalformedAuthorization() {
        given(verifier.verifyBasic("Bearer x")).willThrow(new IllegalArgumentException("bad format"));

        assertThatThrownBy(() -> controller.getUserByMcpUserId("Bearer x", LOGIN_ID, jwt(Role.API_PLUS_OPERATOR, 1001, "SKCC")))
            .isInstanceOf(AppErrorException.class);
        verifyNoInteractions(adminUserService);
    }

    @Test
    @DisplayName("키 검증을 먼저 하므로 로그인 ID 형식 오류보다 인증 실패가 우선한다")
    void verifiesCredentialBeforeLoginId() {
        given(verifier.verifyBasic(BASIC)).willThrow(new BadCredentialsException("Invalid MCP credentials."));

        assertThatThrownBy(() -> controller.getUserByMcpUserId(BASIC, " user@a.com", jwt(Role.API_PLUS_OPERATOR, 1001, "SKCC")))
            .isInstanceOf(AppHttpStatusException.class);
        verify(adminUserService, never()).getUserByLoginIdForClient(anyString(), anyString());
    }

    private static Jwt jwt(Role role, int companyNo, String companyCd) {
        return Jwt.withTokenValue("token")
            .header("alg", "none")
            .issuedAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(60))
            .claim(Claim.ROLE.getCode(), role.name())
            .claim(Claim.COMPANY_NO.getCode(), companyNo)
            .claim(Claim.COMPANY_CD.getCode(), companyCd)
            .build();
    }

}
