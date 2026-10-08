package com.sk.common.interceptor;

import com.sk.common.config.security.mcp.McpClientAuthenticationToken;
import com.sk.common.enums.ResultCode;
import com.sk.common.exception.AppHttpStatusException;
import com.sk.common.utils.DateUtil;
import com.sk.common.utils.MDCUtil;
import com.sk.domain.shared.entity.master.tmd.CustomerCompany;
import com.sk.domain.shared.entity.master.tmd.User;
import com.sk.domain.shared.enums.Claim;
import com.sk.domain.shared.enums.PermissionCondition;
import com.sk.domain.shared.enums.Role;
import com.sk.domain.shared.repository.master.tmd.CustomerCompanyRepository;
import com.sk.domain.shared.repository.master.tmd.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@RequiredArgsConstructor
public class McpUserContextInterceptor implements HandlerInterceptor {

    public static final String X_USER_ID = "X-User-Id";

    private static final String MCP_TOKEN_VALUE = "mcp-effective-user";
    // [MCP 인증] X-User-Id 위임 불가 시 403 메시지 (사용자 없음·회사 불일치는 같은 메시지)
    private static final String USER_ID_REQUIRED = "X-User-Id header is required.";
    private static final String USER_ID_NOT_ALLOWED = "X-User-Id is not allowed for this MCP key.";

    private final UserRepository userRepository;
    private final CustomerCompanyRepository customerCompanyRepository;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        removeOwnedMdcValues();

        try {
            // [MCP 인증] Basic Auth 클라이언트 확인 → X-User-Id 사용자 조회 → 회사 일치 검사
            McpClientAuthenticationToken client = findAuthenticatedClient();
            User user = findEffectiveUser(request.getHeader(X_USER_ID));
            validateCompanyAccess(client, user);
            JwtAuthenticationToken authentication = createAuthentication(user);

            MDCUtil.setValue(MDCUtil.REQUEST_USER_ID, user.getUserNo().toString());
            if (isThirdPartyAccessAdmin(user)) {
                Integer skGasCompanyNo = findSkGasCompanyNo();
                MDCUtil.setValue(MDCUtil.REQUEST_X_COMPANY_NO, skGasCompanyNo.toString());
            }

            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(authentication);
            SecurityContextHolder.setContext(context);
            return true;
        } catch (RuntimeException exception) {
            clearOwnedContext();
            throw exception;
        }
    }

    @Override
    public void afterCompletion(
        HttpServletRequest request,
        HttpServletResponse response,
        Object handler,
        Exception exception
    ) {
        clearOwnedContext();
    }

    private User findEffectiveUser(String userIdHeader) {
        if (StringUtils.isBlank(userIdHeader)) {
            throw userContextDenied(USER_ID_REQUIRED);
        }

        List<User> users = userRepository.findByUserLoginIdAndIsDeletedFalse(userIdHeader);
        if (users.isEmpty()) {
            throw userContextDenied(USER_ID_NOT_ALLOWED);
        }

        User user = users.get(0);
        validateRequiredUserContext(user);
        return user;
    }

    /**
     * [MCP 인증] Basic Auth 로 인증된 MCP 클라이언트를 가져옵니다. 보안 체인을 거치지 않은 요청은 거부합니다(fail-closed).
     */
    private McpClientAuthenticationToken findAuthenticatedClient() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof McpClientAuthenticationToken client && client.isAuthenticated()) {
            return client;
        }
        throw authenticationRequired();
    }

    /**
     * [MCP 인증] X-User-Id 사용자의 회사와 키의 회사가 같아야 합니다.
     * 예외: ADMIN 사용자는 관리회사 키로 요청할 때만 회사가 달라도 허용합니다.
     */
    private void validateCompanyAccess(McpClientAuthenticationToken client, User user) {
        Integer userCompanyNo = user.getCustomerCompany().getCustomerCompanyNo();
        if (Objects.equals(userCompanyNo, client.getCustomerCompanyNo())) {
            return;
        }
        if (isAdmin(user) && client.isManagementCompany()) {
            return;
        }
        throw userContextDenied(USER_ID_NOT_ALLOWED);
    }

    // [MCP 인증] isThirdPartyAccessAdmin 의 ADMIN 판정을 추출해 회사 검사와 공유
    private boolean isAdmin(User user) {
        return Role.ADMIN.getCode().equals(user.getUserRoleCd().getCode());
    }

    private void validateRequiredUserContext(User user) {
        CustomerCompany company = user.getCustomerCompany();
        if (StringUtils.isBlank(user.getUserLoginId())
            || user.getUserRoleCd() == null
            || company == null
            || company.getCustomerCompanyNo() == null
            || StringUtils.isBlank(company.getCustomerCompanyCd())
            || !isActiveCompany(company)) {
            throw userContextDenied(USER_ID_NOT_ALLOWED);
        }
    }

    private boolean isActiveCompany(CustomerCompany company) {
        String useBeginDt = company.getUseBeginDt();
        String useEndDt = company.getUseEndDt();
        String today = DateUtil.currentYmd();

        return BooleanUtils.isTrue(company.getIsUsed())
            && BooleanUtils.isFalse(company.getIsDeleted())
            && StringUtils.isNotBlank(useBeginDt)
            && StringUtils.isNotBlank(useEndDt)
            && useBeginDt.compareTo(today) <= 0
            && useEndDt.compareTo(today) >= 0;
    }

    private JwtAuthenticationToken createAuthentication(User user) {
        CustomerCompany company = user.getCustomerCompany();
        Instant issuedAt = Instant.now();
        Jwt jwt = Jwt.withTokenValue(MCP_TOKEN_VALUE)
            .header("typ", "JWT")
            .subject(user.getUserLoginId())
            .issuedAt(issuedAt)
            .expiresAt(issuedAt.plusSeconds(300))
            .claim(Claim.USER_NO.getCode(), user.getUserNo())
            .claim(Claim.COMPANY_NO.getCode(), company.getCustomerCompanyNo())
            .claim(Claim.COMPANY_CD.getCode(), company.getCustomerCompanyCd())
            .claim(Claim.ROLE.getCode(), user.getUserRoleCd().getCode())
            .claim(Claim.AUTH_METHOD.getCode(), Claim.AuthMethodValue.MCP.getCode())
            .claim(
                Claim.OTHER_SERVICE_ACCESS.getCode(),
                BooleanUtils.isTrue(user.getIsOtherServiceAccessPossible()))
            .build();
        return new JwtAuthenticationToken(jwt, List.of());
    }

    private boolean isThirdPartyAccessAdmin(User user) {
        return isAdmin(user)
            && BooleanUtils.isTrue(user.getIsOtherServiceAccessPossible());
    }

    private Integer findSkGasCompanyNo() {
        String skGasCompanyCode = PermissionCondition.AllowedCompany.SKGAS.getCode();
        return customerCompanyRepository
            .findByCustomerCompanyCdAndIsMipManagementCompanyFalseAndIsDeletedFalse(skGasCompanyCode)
            .map(CustomerCompany::getCustomerCompanyNo)
            .orElseThrow(this::authenticationRequired);
    }

    private AppHttpStatusException authenticationRequired() {
        return AppHttpStatusException.of(HttpStatus.UNAUTHORIZED, ResultCode.Error.AUTH_REQUIRED, null);
    }

    /**
     * [MCP 인증] 키 인증은 통과했으나 X-User-Id 로 위임할 수 없는 경우(403).
     * 사용자 없음·비활성·회사 불일치를 같은 메시지로 응답해 다른 회사의 사용자 ID 존재 여부가 드러나지 않게 합니다.
     */
    private AppHttpStatusException userContextDenied(String reason) {
        return AppHttpStatusException.of(HttpStatus.FORBIDDEN, ResultCode.Error.AUTH_ACCESS_DENIED, reason);
    }

    private void clearOwnedContext() {
        SecurityContextHolder.clearContext();
        removeOwnedMdcValues();
    }

    private void removeOwnedMdcValues() {
        MDCUtil.remove(MDCUtil.REQUEST_USER_ID);
        MDCUtil.remove(MDCUtil.REQUEST_X_COMPANY_NO);
    }
}
