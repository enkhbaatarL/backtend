package com.sk.domain.shared.service.permissions;

import com.sk.common.enums.ResultCode;
import com.sk.common.exception.CustomAuthorizationException;
import com.sk.common.utils.MDCUtil;
import com.sk.domain.mi.dto.response.PermissionRes;
import com.sk.domain.shared.entity.master.tam.AuthManageGoodsMapping;
import com.sk.domain.shared.enums.Claim;
import com.sk.domain.shared.enums.GoodsAuthCd;
import com.sk.domain.shared.enums.Role;
import com.sk.domain.shared.repository.master.tam.AuthManageGoodsMappingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * JWT 인증/권한 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthorizationService {

    private final AuthManageGoodsMappingRepository authManageGoodsMappingRepository;

    /**
     * JWT 토큰 조회
     */
    public Jwt getCurrentJwt() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!(authentication instanceof JwtAuthenticationToken)) {
            throw CustomAuthorizationException.of(ResultCode.Error.AUTH_REQUIRED);
        }
        return ((JwtAuthenticationToken) authentication).getToken();
    }

    /**
     * 회사번호 조회
     */
    public Integer getCompanyNo() {
        Jwt jwt = getCurrentJwt();
        Object companyNoObj = jwt.getClaim(Claim.COMPANY_NO.getCode());
        if (companyNoObj == null) {
            throw CustomAuthorizationException.of(ResultCode.Error.AUTH_INSUFFICIENT_PERMISSIONS, "Company not found");
        }
        try {
            return Integer.parseInt(companyNoObj.toString());
        } catch (NumberFormatException e) {
            throw CustomAuthorizationException.of(ResultCode.Error.AUTH_INVALID_CREDENTIALS, "Invalid company number");
        }
    }

    /**
     * 우선순위에 따른 회사번호 조회
     *   1. 헤더의 X-Company-No
     *   2. token claim
     */
    public Integer getCompanyNoWithPriority() {
        return Optional.ofNullable(MDCUtil.getValue(MDCUtil.REQUEST_X_COMPANY_NO))
            .map(Integer::parseInt)
            .orElseGet(this::getCompanyNo);
    }

    /**
     * 사용자번호 조회
     */
    public Integer getUserNo() {
        Jwt jwt = getCurrentJwt();
        Object userNoObj = jwt.getClaim(Claim.USER_NO.getCode());
        if (userNoObj == null) {
            throw CustomAuthorizationException.of(ResultCode.Error.AUTH_INSUFFICIENT_PERMISSIONS, "User not found");
        }
        try {
            return Integer.parseInt(userNoObj.toString());
        } catch (NumberFormatException e) {
            throw CustomAuthorizationException.of(ResultCode.Error.AUTH_INVALID_CREDENTIALS, "Invalid user number");
        }
    }

    /**
     * 역할 조회
     */
    public Role getCurrentRole() {
        Jwt jwt = getCurrentJwt();
        String role = jwt.getClaimAsString(Claim.ROLE.getCode());
        if (role == null) {
            throw CustomAuthorizationException.of(ResultCode.Error.AUTH_INSUFFICIENT_PERMISSIONS, "Role not found");
        }
        try {
            return Role.valueOf(role);
        } catch (IllegalArgumentException e) {
            throw CustomAuthorizationException.of(ResultCode.Error.AUTH_INVALID_CREDENTIALS, "Invalid role");
        }
    }

    /**
     * 타사접속 가능여부 조회
     */
    public boolean isThirdPartyAccessAllowed() {
        Jwt jwt = getCurrentJwt();
        Object isOtherServiceAccessPossible = jwt.getClaim(Claim.OTHER_SERVICE_ACCESS.getCode());
        if (isOtherServiceAccessPossible == null) {
            throw CustomAuthorizationException.of(ResultCode.Error.AUTH_INSUFFICIENT_PERMISSIONS, "Third party access info not found");
        }
        try {
            return Boolean.parseBoolean(isOtherServiceAccessPossible.toString());
        } catch (NumberFormatException e) {
            throw CustomAuthorizationException.of(ResultCode.Error.AUTH_INVALID_CREDENTIALS, "Invalid third party access format");
        }
    }

    /**
     * Subject 조회
     */
    public String getSubject() {
        Jwt jwt = getCurrentJwt();
        String subject = jwt.getSubject();
        if (subject == null) {
            throw CustomAuthorizationException.of(ResultCode.Error.AUTH_INSUFFICIENT_PERMISSIONS, "Subject not found");
        }
        try {
            return subject;
        } catch (NumberFormatException e) {
            throw CustomAuthorizationException.of(ResultCode.Error.AUTH_INVALID_CREDENTIALS, "Invalid subject format");
        }
    }

    /**
     * 관리자여부 조회
     */
    public boolean isAdmin() {
        return Role.ADMIN.equals(getCurrentRole());
    }

    /**
     * 회사 접근권한 검증
     */
    public void validateCompanyAccess(Integer companyNo, PermissionRes allowedPermissions) {
        if (!allowedPermissions.getPermittedCompanies().contains(companyNo)) {
            throw CustomAuthorizationException.of(ResultCode.Error.AUTH_INSUFFICIENT_PERMISSIONS,
                "No permission for company: %d".formatted(companyNo));
        }
    }

    /**
     * 관리자권한 검증
     */
    public void validateAdmin() {
        if (!isAdmin()) {
            throw CustomAuthorizationException.of(ResultCode.Error.AUTH_INSUFFICIENT_PERMISSIONS, "Admin permission required");
        }
    }

    /**
     * 프로바이더 접근권한 검증
     */
    public void validateProviderAccess(Integer providerNo, PermissionRes allowedPermissions) {
        if (!allowedPermissions.getPermittedProviders().contains(providerNo)) {
            throw CustomAuthorizationException.of(ResultCode.Error.AUTH_INSUFFICIENT_PERMISSIONS,
                "No permission for provider: %d".formatted(providerNo));
        }
    }

    /**
     * 특정상품 권한여부 조회
     */
    private boolean checkPermission(Integer userNo, Integer goodsNo, GoodsAuthCd authCd) {
        List<AuthManageGoodsMapping> userPermissions = authManageGoodsMappingRepository.findUserGoodsPermissions(userNo);

        return userPermissions.stream()
            .filter(mapping -> !mapping.getIsDeleted())
            .filter(mapping -> mapping.getCustomerCompanyGoodsMapping().getGoods().getGoodsNo().equals(goodsNo))
            .anyMatch(mapping -> mapping.getGoodsAuthCd() == authCd);
    }

}
