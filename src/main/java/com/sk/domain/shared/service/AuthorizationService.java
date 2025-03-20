package com.sk.domain.shared.service;

import com.sk.common.enums.ResultCode;
import com.sk.common.exception.CustomAuthorizationException;
import com.sk.domain.mi.dto.response.PermissionRes;
import com.sk.domain.shared.entity.master.tam.AuthManageGoodsMapping;
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

/**
 * JWT 토큰 기반 인증, 권한 관련 기능을 제공하는 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthorizationService {

    private final AuthManageGoodsMappingRepository authManageGoodsMappingRepository;

    /**
     * 현재 인증된 사용자의 JWT 토큰을 반환합니다.
     *
     * @return JWT 토큰
     */
    public Jwt getCurrentJwt() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!(authentication instanceof JwtAuthenticationToken)) {
            throw CustomAuthorizationException.of(ResultCode.Error.AUTH_REQUIRED);
        }
        return ((JwtAuthenticationToken) authentication).getToken();
    }

    /**
     * 현재 인증된 사용자의 회사 번호를 반환합니다.
     *
     * @return 회사 번호
     */
    public Integer getCompanyNo() {
        Jwt jwt = getCurrentJwt();
        Object companyNoObj = jwt.getClaim("companyNo");
        if (companyNoObj == null) {
            throw CustomAuthorizationException.of(ResultCode.Error.AUTH_INSUFFICIENT_PERMISSIONS, "Company information not found in token");
        }
        try {
            return Integer.parseInt(companyNoObj.toString());
        } catch (NumberFormatException e) {
            throw CustomAuthorizationException.of(ResultCode.Error.AUTH_INVALID_CREDENTIALS, "Invalid company number format");
        }
    }

    /**
     * 현재 인증된 사용자 번호를 반환합니다.
     *
     * @return 사용자 번호
     */
    public Integer getUserNo() {
        Jwt jwt = getCurrentJwt();
        Object userNoObj = jwt.getClaim("userNo");
        if (userNoObj == null) {
            throw CustomAuthorizationException.of(ResultCode.Error.AUTH_INSUFFICIENT_PERMISSIONS, "user information not found in token");
        }
        try {
            return Integer.parseInt(userNoObj.toString());
        } catch (NumberFormatException e) {
            throw CustomAuthorizationException.of(ResultCode.Error.AUTH_INVALID_CREDENTIALS, "Invalid user number format");
        }
    }

    /**
     * 현재 인증된 사용자의 역할을 반환합니다.
     *
     * @return 사용자 역할
     */
    public Role getCurrentRole() {
        Jwt jwt = getCurrentJwt();
        String role = jwt.getClaimAsString("role");
        if (role == null) {
            throw CustomAuthorizationException.of(ResultCode.Error.AUTH_INSUFFICIENT_PERMISSIONS, "Role information not found in token");
        }
        try {
            return Role.valueOf(role);
        } catch (IllegalArgumentException e) {
            throw CustomAuthorizationException.of(ResultCode.Error.AUTH_INVALID_CREDENTIALS, "Invalid role: " + role);
        }
    }

    /**
     * 현재 사용자가 관리자인지 확인합니다.
     *
     * @return 관리자 여부
     */
    public boolean isAdmin() {
        return Role.ADMIN.equals(getCurrentRole());
    }


    /**
     * 현재 사용자가 특정 회사에 접근할 권한이 있는지 확인하고, 권한이 없으면 예외를 발생시킵니다.
     *
     * @param companyNo 접근하려는 회사 번호
     * @param allowedPermissions 허용된 권한
     * @throws CustomAuthorizationException 권한이 없는 경우 발생
     */
    public void validateCompanyAccess(Integer companyNo, PermissionRes allowedPermissions) {

        if (!allowedPermissions.getPermittedCompanies().contains(companyNo)) {
            throw CustomAuthorizationException.of(ResultCode.Error.AUTH_INSUFFICIENT_PERMISSIONS,
                "You don't have permission to access company: %s".formatted(companyNo));
        }

    }

    /**
     * 현재 사용자가 관리자인지 확인하고, 관리자가 아니면 예외를 발생시킵니다.
     *
     * @throws CustomAuthorizationException 관리자가 아닌 경우 발생
     */
    public void validateAdmin() {
        if (!isAdmin()) {
            throw CustomAuthorizationException.of(ResultCode.Error.AUTH_INSUFFICIENT_PERMISSIONS,
                "Admin permission required");
        }
    }

    /**
     * 현재 사용자가 특정 프로바이더 관련 리소스에 접근할 권한이 있는지 확인합니다.
     * 관리자만 프로바이더 관리 권한이 있다고 가정합니다.
     *
     * @param providerNo 접근하려는 프로바이더 번호
     * @throws CustomAuthorizationException 권한이 없는 경우 발생
     */
    public void validateProviderAccess(Integer providerNo, PermissionRes allowedPermissions) {

        if (!allowedPermissions.getPermittedProviders().contains(providerNo)) {
            throw CustomAuthorizationException.of(ResultCode.Error.AUTH_INSUFFICIENT_PERMISSIONS,
                    "You don't have permission to access provider: %s".formatted(providerNo));
        }

    }

    /**
     * 특정 사용자가 특정 상품에 대한 권한이 있는지 확인합니다.
     *
     * @param userNo 사용자 번호
     * @param goodsNo 상품 번호
     * @param authCd 권한 코드 (VIEW, DOWNLOAD)
     * @return 권한 여부
     */
    private boolean checkPermission(Integer userNo, Integer goodsNo, GoodsAuthCd authCd) {
        List<AuthManageGoodsMapping> userPermissions = authManageGoodsMappingRepository.findUserGoodsPermissions(userNo);

        return userPermissions.stream()
            .filter(mapping -> !mapping.getIsDeleted())
            .filter(mapping -> mapping.getCustomerCompanyGoodsMapping().getGoods().getGoodsNo().equals(goodsNo))
            .anyMatch(mapping -> mapping.getGoodsAuthCd() == authCd);
    }

    /**
     * 현재 인증된 사용자가 특정 상품에 대한 VIEW 권한이 있는지 확인하고, 권한이 없으면 예외를 발생시킵니다.
     *
     * @param goodsNo 상품 번호
     * @throws CustomAuthorizationException 권한이 없는 경우 발생
     */
    public void validateViewPermission(Integer goodsNo, PermissionRes allowedPermissions) {
        if (allowedPermissions.getGoodsViewableMap().getOrDefault(goodsNo, false)) {
            throw CustomAuthorizationException.of(ResultCode.Error.AUTH_INSUFFICIENT_PERMISSIONS,
                "You don't have VIEW permission for goods: " + goodsNo);
        }
    }

    /**
     * 현재 인증된 사용자가 특정 상품에 대한 DOWNLOAD 권한이 있는지 확인하고, 권한이 없으면 예외를 발생시킵니다.
     *
     * @param goodsNo 상품 번호
     * @throws CustomAuthorizationException 권한이 없는 경우 발생
     */
    public void validateDownloadPermission(Integer goodsNo, PermissionRes allowedPermissions) {
        if (allowedPermissions.getGoodsDownloadableMap().getOrDefault(goodsNo, false)) {
            throw CustomAuthorizationException.of(ResultCode.Error.AUTH_INSUFFICIENT_PERMISSIONS,
                "You don't have DOWNLOAD permission for goods: " + goodsNo);
        }
    }

}
