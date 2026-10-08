package com.sk.common.config.security.mcp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import com.sk.domain.shared.entity.master.tmd.CustomerCompany;
import com.sk.domain.shared.entity.master.tmd.McpAuthInfo;
import com.sk.domain.shared.repository.master.tmd.McpAuthInfoRepository;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

@DisplayName("MCP Basic Auth 인증")
class McpAuthenticationProviderTest {

    private final McpAuthInfoRepository repository = mock(McpAuthInfoRepository.class);
    private final McpSecretHasher hasher = new McpSecretHasher(1000);
    private final McpCredentialVerifier verifier = new McpCredentialVerifier(repository, hasher);
    private final McpAuthenticationProvider provider = new McpAuthenticationProvider(verifier);

    @Test
    @DisplayName("ID 와 시크릿이 맞으면 키의 회사로 인증된다")
    void authenticatesWithCompany() {
        String secret = hasher.generateSecret();
        CustomerCompany company = company(1001, false, true);
        given(repository.findByMcpAuthIdAndIsDeletedFalse("skcc-mcp"))
            .willReturn(Optional.of(McpAuthInfo.of("skcc-mcp", hasher.hash(secret), company)));

        Authentication result = provider.authenticate(UsernamePasswordAuthenticationToken.unauthenticated("skcc-mcp", secret));

        assertThat(result).isInstanceOf(McpClientAuthenticationToken.class);
        McpClientAuthenticationToken client = (McpClientAuthenticationToken) result;
        assertThat(client.isAuthenticated()).isTrue();
        assertThat(client.getCustomerCompanyNo()).isEqualTo(1001);
        assertThat(client.isManagementCompany()).isTrue();
        assertThat(client.getCredentials()).isNull();
    }

    @Test
    @DisplayName("시크릿이 틀리면 거부한다")
    void rejectsWrongSecret() {
        CustomerCompany company = company(1001, false, false);
        given(repository.findByMcpAuthIdAndIsDeletedFalse("skcc-mcp"))
            .willReturn(Optional.of(McpAuthInfo.of("skcc-mcp", hasher.hash(hasher.generateSecret()), company)));

        assertThatThrownBy(() -> provider.authenticate(UsernamePasswordAuthenticationToken.unauthenticated("skcc-mcp", "wrong")))
            .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    @DisplayName("없는 ID 는 거부한다")
    void rejectsUnknownId() {
        given(repository.findByMcpAuthIdAndIsDeletedFalse("unknown")).willReturn(Optional.empty());

        assertThatThrownBy(() -> provider.authenticate(UsernamePasswordAuthenticationToken.unauthenticated("unknown", "any")))
            .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    @DisplayName("키의 회사가 삭제됐으면 거부한다")
    void rejectsDeletedCompany() {
        String secret = hasher.generateSecret();
        CustomerCompany company = company(1001, true, false);
        given(repository.findByMcpAuthIdAndIsDeletedFalse("skcc-mcp"))
            .willReturn(Optional.of(McpAuthInfo.of("skcc-mcp", hasher.hash(secret), company)));

        assertThatThrownBy(() -> provider.authenticate(UsernamePasswordAuthenticationToken.unauthenticated("skcc-mcp", secret)))
            .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    @DisplayName("Basic base64(id:secret) 문자열로도 같은 검증을 한다")
    void verifiesBasicAuthorizationString() {
        String secret = hasher.generateSecret();
        CustomerCompany company = company(1001, false, false);
        given(repository.findByMcpAuthIdAndIsDeletedFalse("skcc-mcp01"))
            .willReturn(Optional.of(McpAuthInfo.of("skcc-mcp01", hasher.hash(secret), company)));

        McpClientAuthenticationToken client = verifier.verifyBasic("Basic " + base64("skcc-mcp01:" + secret));

        assertThat(client.getCustomerCompanyNo()).isEqualTo(1001);
        assertThatThrownBy(() -> verifier.verifyBasic("Basic " + base64("skcc-mcp01:wrong")))
            .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    @DisplayName("Basic 형식이 아니면 형식 오류로 구분한다")
    void rejectsMalformedBasicAuthorization() {
        assertThatThrownBy(() -> verifier.verifyBasic(null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> verifier.verifyBasic("Bearer abc")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> verifier.verifyBasic("Basic !!!not-base64")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> verifier.verifyBasic("Basic " + base64("no-delimiter"))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> verifier.verifyBasic("Basic " + base64(":secret-only"))).isInstanceOf(IllegalArgumentException.class);
    }

    private static String base64(String value) {
        return java.util.Base64.getEncoder().encodeToString(value.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    private static CustomerCompany company(int companyNo, boolean deleted, boolean managementCompany) {
        CustomerCompany company = mock(CustomerCompany.class);
        given(company.getCustomerCompanyNo()).willReturn(companyNo);
        given(company.getIsDeleted()).willReturn(deleted);
        given(company.getIsMipManagementCompany()).willReturn(managementCompany);
        return company;
    }

}
