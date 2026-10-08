package com.sk.common.config.security.mcp;

import com.sk.domain.shared.entity.master.tmd.CustomerCompany;
import com.sk.domain.shared.entity.master.tmd.McpAuthInfo;
import com.sk.domain.shared.repository.master.tmd.McpAuthInfoRepository;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Component;

/**
 * [MCP 인증] MCP 인증 키(tmd_mcpauthinfom) 검증. MCP API 보안 체인과 관리 API(by-mcp-user-id)가 함께 사용합니다.
 * AuthenticationProvider 가 아니므로 전역 AuthenticationManager 에 섞이지 않습니다.
 */
@Component
@RequiredArgsConstructor
public class McpCredentialVerifier {

    /** Bearer 토큰과 함께 MCP 인증 키를 전달하는 헤더. 값 형식은 MCP API 의 Authorization 과 같습니다. */
    public static final String MCP_AUTHORIZATION_HEADER = "X-MCP-Authorization";

    private static final String INVALID_CREDENTIALS = "Invalid MCP credentials.";
    private static final String BASIC_PREFIX = "Basic ";

    private final McpAuthInfoRepository mcpAuthInfoRepository;
    private final McpSecretHasher mcpSecretHasher;

    /**
     * ID 와 시크릿을 검증하고 키의 회사로 인증된 클라이언트를 돌려줍니다.
     *
     * @throws BadCredentialsException ID 가 없거나, 시크릿이 틀리거나, 키의 회사가 삭제된 경우
     */
    public McpClientAuthenticationToken verify(String mcpAuthId, String secret) {
        Optional<McpAuthInfo> found = mcpAuthInfoRepository.findByMcpAuthIdAndIsDeletedFalse(mcpAuthId);
        if (found.isEmpty()) {
            mcpSecretHasher.matchesDummy(secret);
            throw new BadCredentialsException(INVALID_CREDENTIALS);
        }

        McpAuthInfo mcpAuthInfo = found.get();
        boolean matched = mcpSecretHasher.matches(
            secret,
            mcpAuthInfo.getHashAlgorithmNm(),
            mcpAuthInfo.getSecretSaltValue(),
            mcpAuthInfo.getHashIterationCnt(),
            mcpAuthInfo.getSecretHashValue());
        CustomerCompany company = mcpAuthInfo.getCustomerCompany();
        if (!matched || company == null || BooleanUtils.isTrue(company.getIsDeleted())) {
            throw new BadCredentialsException(INVALID_CREDENTIALS);
        }

        return new McpClientAuthenticationToken(
            mcpAuthInfo.getMcpAuthId(),
            company.getCustomerCompanyNo(),
            BooleanUtils.isTrue(company.getIsMipManagementCompany()));
    }

    /**
     * "Basic base64(id:secret)" 형식(RFC 7617)의 값을 검증합니다. MCP API 에 보내는 Authorization 값과 같은 형식입니다.
     *
     * @throws IllegalArgumentException 형식이 잘못된 경우
     * @throws BadCredentialsException  인증정보가 틀린 경우
     */
    public McpClientAuthenticationToken verifyBasic(String basicAuthorization) {
        if (StringUtils.isBlank(basicAuthorization) || !StringUtils.startsWithIgnoreCase(basicAuthorization, BASIC_PREFIX)) {
            throw new IllegalArgumentException("MCP authorization must be 'Basic base64(id:secret)'.");
        }
        String decoded;
        try {
            decoded = new String(Base64.getDecoder().decode(basicAuthorization.substring(BASIC_PREFIX.length()).trim()), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("MCP authorization is not valid Base64.", e);
        }
        int delimiter = decoded.indexOf(':');
        if (delimiter <= 0) {
            throw new IllegalArgumentException("MCP authorization must contain 'id:secret'.");
        }
        return verify(decoded.substring(0, delimiter), decoded.substring(delimiter + 1));
    }

}
