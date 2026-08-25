package com.sk.common.interceptor;

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
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;
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

    private final UserRepository userRepository;
    private final CustomerCompanyRepository customerCompanyRepository;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        removeOwnedMdcValues();

        try {
            User user = findEffectiveUser(request.getHeader(X_USER_ID));
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
            throw authenticationRequired();
        }

        List<User> users = userRepository.findByUserLoginIdAndIsDeletedFalse(userIdHeader);
        if (users.isEmpty()) {
            throw authenticationRequired();
        }

        User user = users.get(0);
        validateRequiredUserContext(user);
        return user;
    }

    private void validateRequiredUserContext(User user) {
        CustomerCompany company = user.getCustomerCompany();
        if (StringUtils.isBlank(user.getUserLoginId())
            || user.getUserRoleCd() == null
            || company == null
            || company.getCustomerCompanyNo() == null
            || StringUtils.isBlank(company.getCustomerCompanyCd())
            || !isActiveCompany(company)) {
            throw authenticationRequired();
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
        return Role.ADMIN.getCode().equals(user.getUserRoleCd().getCode())
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

    private void clearOwnedContext() {
        SecurityContextHolder.clearContext();
        removeOwnedMdcValues();
    }

    private void removeOwnedMdcValues() {
        MDCUtil.remove(MDCUtil.REQUEST_USER_ID);
        MDCUtil.remove(MDCUtil.REQUEST_X_COMPANY_NO);
    }
}
