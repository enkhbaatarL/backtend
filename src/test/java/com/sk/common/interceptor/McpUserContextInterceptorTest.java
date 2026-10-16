package com.sk.common.interceptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import com.sk.common.config.security.mcp.McpClientAuthenticationToken;
import com.sk.common.exception.AppHttpStatusException;
import com.sk.domain.shared.entity.master.tmd.CustomerCompany;
import com.sk.domain.shared.entity.master.tmd.User;
import com.sk.domain.shared.enums.UserRoleCd;
import com.sk.domain.shared.repository.master.tmd.CustomerCompanyRepository;
import com.sk.domain.shared.repository.master.tmd.UserRepository;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

@DisplayName("MCP 사용자 컨텍스트 — 키 회사와 사용자 회사 검사")
class McpUserContextInterceptorTest {

    private static final int COMPANY_A = 1001;
    private static final int COMPANY_B = 2002;

    private final UserRepository userRepository = mock(UserRepository.class);
    private final CustomerCompanyRepository customerCompanyRepository = mock(CustomerCompanyRepository.class);
    private final McpUserContextInterceptor interceptor = new McpUserContextInterceptor(userRepository, customerCompanyRepository, "");

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("사용자 회사와 키 회사가 같으면 통과하고 사용자 컨텍스트로 교체한다")
    void allowsSameCompany() {
        authenticateClient(COMPANY_A, false);
        givenUser("user@a.com", COMPANY_A, UserRoleCd.USER);

        assertThat(preHandle("user@a.com")).isTrue();
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isInstanceOf(JwtAuthenticationToken.class);
    }

    @Test
    @DisplayName("회사가 다르면 403")
    void rejectsOtherCompany() {
        authenticateClient(COMPANY_A, false);
        givenUser("user@b.com", COMPANY_B, UserRoleCd.USER);

        assertForbidden("user@b.com");
    }

    @Test
    @DisplayName("ADMIN 이라도 고객사 키면 회사가 다를 때 403")
    void rejectsAdminWithCustomerKey() {
        authenticateClient(COMPANY_A, false);
        givenUser("admin@b.com", COMPANY_B, UserRoleCd.ADMIN);

        assertForbidden("admin@b.com");
    }

    @Test
    @DisplayName("ADMIN 이 관리회사 키로 요청하면 회사가 달라도 통과")
    void allowsAdminWithManagementKey() {
        authenticateClient(COMPANY_A, true);
        givenUser("admin@b.com", COMPANY_B, UserRoleCd.ADMIN);

        assertThat(preHandle("admin@b.com")).isTrue();
    }

    @Test
    @DisplayName("관리회사 키라도 일반 사용자는 회사가 다르면 403")
    void rejectsUserWithManagementKey() {
        authenticateClient(COMPANY_A, true);
        givenUser("user@b.com", COMPANY_B, UserRoleCd.USER);

        assertForbidden("user@b.com");
    }

    @Test
    @DisplayName("키 인증은 됐지만 X-User-Id 가 없으면 403")
    void rejectsMissingUserIdAsForbidden() {
        authenticateClient(COMPANY_A, false);

        assertForbidden("");
    }

    @Test
    @DisplayName("X-User-Id 가 없으면 mcp.default-id 사용자로 처리한다")
    void usesDefaultUserIdWhenHeaderMissing() {
        McpUserContextInterceptor withDefault = new McpUserContextInterceptor(userRepository, customerCompanyRepository, "default@a.com");
        authenticateClient(COMPANY_A, false);
        givenUser("default@a.com", COMPANY_A, UserRoleCd.USER);

        assertThat(withDefault.preHandle(new MockHttpServletRequest(), new MockHttpServletResponse(), new Object())).isTrue();
        assertThat(((JwtAuthenticationToken) SecurityContextHolder.getContext().getAuthentication()).getName()).isEqualTo("default@a.com");
    }

    @Test
    @DisplayName("X-User-Id 가 있으면 mcp.default-id 보다 헤더를 우선한다")
    void prefersHeaderOverDefaultUserId() {
        McpUserContextInterceptor withDefault = new McpUserContextInterceptor(userRepository, customerCompanyRepository, "default@a.com");
        authenticateClient(COMPANY_A, false);
        givenUser("user@a.com", COMPANY_A, UserRoleCd.USER);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(McpUserContextInterceptor.X_USER_ID, "user@a.com");

        assertThat(withDefault.preHandle(request, new MockHttpServletResponse(), new Object())).isTrue();
        assertThat(SecurityContextHolder.getContext().getAuthentication().getName()).isEqualTo("user@a.com");
    }

    @Test
    @DisplayName("mcp.default-id 사용자도 회사가 다르면 403")
    void rejectsDefaultUserOfOtherCompany() {
        McpUserContextInterceptor withDefault = new McpUserContextInterceptor(userRepository, customerCompanyRepository, "default@b.com");
        authenticateClient(COMPANY_A, false);
        givenUser("default@b.com", COMPANY_B, UserRoleCd.USER);

        assertThatThrownBy(() -> withDefault.preHandle(new MockHttpServletRequest(), new MockHttpServletResponse(), new Object()))
            .isInstanceOf(AppHttpStatusException.class)
            .extracting("httpStatus").isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    @DisplayName("키 인증은 됐지만 X-User-Id 사용자가 없으면 403")
    void rejectsUnknownUserAsForbidden() {
        authenticateClient(COMPANY_A, false);
        given(userRepository.findByUserLoginIdAndIsDeletedFalse("nobody@a.com")).willReturn(List.of());

        assertForbidden("nobody@a.com");
    }

    @Test
    @DisplayName("Basic Auth 인증 없이 들어온 요청은 401")
    void rejectsWithoutClientAuthentication() {
        givenUser("user@a.com", COMPANY_A, UserRoleCd.USER);

        assertThatThrownBy(() -> preHandle("user@a.com"))
            .isInstanceOf(AppHttpStatusException.class)
            .extracting("httpStatus").isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    private boolean preHandle(String userId) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(McpUserContextInterceptor.X_USER_ID, userId);
        return interceptor.preHandle(request, new MockHttpServletResponse(), new Object());
    }

    private void assertForbidden(String userId) {
        assertThatThrownBy(() -> preHandle(userId))
            .isInstanceOf(AppHttpStatusException.class)
            .extracting("httpStatus").isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    private static void authenticateClient(int companyNo, boolean managementCompany) {
        SecurityContextHolder.getContext().setAuthentication(new McpClientAuthenticationToken("key", companyNo, managementCompany));
    }

    private void givenUser(String loginId, int companyNo, UserRoleCd role) {
        CustomerCompany company = mock(CustomerCompany.class);
        given(company.getCustomerCompanyNo()).willReturn(companyNo);
        given(company.getCustomerCompanyCd()).willReturn("C" + companyNo);
        given(company.getIsUsed()).willReturn(true);
        given(company.getIsDeleted()).willReturn(false);
        given(company.getUseBeginDt()).willReturn("20000101");
        given(company.getUseEndDt()).willReturn("29991231");

        User user = mock(User.class);
        given(user.getUserNo()).willReturn(1);
        given(user.getUserLoginId()).willReturn(loginId);
        given(user.getUserRoleCd()).willReturn(role);
        given(user.getCustomerCompany()).willReturn(company);
        given(user.getIsOtherServiceAccessPossible()).willReturn(false);
        given(userRepository.findByUserLoginIdAndIsDeletedFalse(loginId)).willReturn(List.of(user));
    }

}
