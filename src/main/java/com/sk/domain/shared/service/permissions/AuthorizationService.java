package com.sk.domain.shared.service.permissions;

import com.sk.common.enums.ResultCode;
import com.sk.common.exception.AppErrorException;
import com.sk.common.exception.CustomAuthorizationException;
import com.sk.common.utils.MDCUtil;
import com.sk.domain.mi.dto.response.PermissionRes;
import com.sk.domain.mi.service.MiGoodsPermissionService;
import com.sk.domain.shared.entity.master.tmd.CustomerCompany;
import com.sk.domain.shared.enums.Claim;
import com.sk.domain.shared.enums.Role;
import com.sk.domain.shared.repository.master.tam.AuthManageGoodsMappingRepository;
import com.sk.domain.shared.repository.master.tmd.CustomerCompanyRepository;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;

/**
 * JWT 인증/권한 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthorizationService {

    private final MiGoodsPermissionService goodsPermissionService;
    private final AuthManageGoodsMappingRepository authManageGoodsMappingRepository;
    private final CustomerCompanyRepository customerCompanyRepository;

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
            throw CustomAuthorizationException.of(ResultCode.Error.AUTH_INSUFFICIENT_PERMISSIONS, "companyNo not found");
        }
        try {
            return Integer.parseInt(companyNoObj.toString());
        } catch (NumberFormatException e) {
            throw CustomAuthorizationException.of(ResultCode.Error.AUTH_INVALID_CREDENTIALS, "Invalid companyNo: %s".formatted(companyNoObj.toString()));
        }
    }

    /**
     * 회사코드 조회
     */
    public String getCompanyCd() {
        Jwt jwt = getCurrentJwt();
        String companyCd = jwt.getClaimAsString(Claim.COMPANY_CD.getCode());
        if (companyCd == null) {
            throw CustomAuthorizationException.of(ResultCode.Error.AUTH_INSUFFICIENT_PERMISSIONS, "companyCd not found");
        }
        return companyCd;
    }

    /**
     * 우선순위에 따른 회사번호 조회
     * 1. 헤더의 X-Company-No
     * 2. token claim
     */
    public Integer getCompanyNoWithPriority() {
        return Optional.ofNullable(MDCUtil.getValue(MDCUtil.REQUEST_X_COMPANY_NO))
            .map(Integer::parseInt)
            .orElseGet(this::getCompanyNo);
    }

    /**
     * 회사코드 조회.
     * 1. 헤더의 X-Company-No 기준 고객사
     * 2. token claim 의 companyNo 기준 고객사
     *
     * Admin 이 특정 고객사 컨텍스트로 서비스를 사용할 때 JWT companyCd 대신
     * 선택된 고객사 코드를 사용해야 하는 파일 암복호화 등에서 사용한다.
     */
    public String getCompanyCdWithPriority() {
        Integer companyNo = getCompanyNoWithPriority();
        CustomerCompany customerCompany = customerCompanyRepository.findByCustomerCompanyNoAndIsDeletedFalse(companyNo)
            .orElseThrow(() -> AppErrorException.of(
                ResultCode.Error.RSC_NOT_FOUND,
                "customer company no: %d".formatted(companyNo)));
        return customerCompany.getCustomerCompanyCd();
    }

    /**
     * 사용자번호 조회
     */
    public Integer getUserNo() {
        Jwt jwt = getCurrentJwt();
        Object userNoObj = jwt.getClaim(Claim.USER_NO.getCode());
        if (userNoObj == null) {
            throw CustomAuthorizationException.of(ResultCode.Error.AUTH_INSUFFICIENT_PERMISSIONS, "userNo not found");
        }
        try {
            return Integer.parseInt(userNoObj.toString());
        } catch (NumberFormatException e) {
            throw CustomAuthorizationException.of(ResultCode.Error.AUTH_INVALID_CREDENTIALS, "Invalid userNo: %s".formatted(userNoObj.toString()));
        }
    }

    /**
     * 역할 조회
     */
    public Role getCurrentRole() {
        Jwt jwt = getCurrentJwt();
        String role = jwt.getClaimAsString(Claim.ROLE.getCode());
        if (role == null) {
            throw CustomAuthorizationException.of(ResultCode.Error.AUTH_INSUFFICIENT_PERMISSIONS, "role not found");
        }
        try {
            return Role.valueOf(role);
        } catch (IllegalArgumentException e) {
            throw CustomAuthorizationException.of(ResultCode.Error.AUTH_INVALID_CREDENTIALS, "Invalid role: %s".formatted(role));
        }
    }

    /**
     * 타사접속 가능여부 조회
     */
    public boolean isThirdPartyAccessAllowed() {
        Jwt jwt = getCurrentJwt();
        Object isOtherServiceAccessPossible = jwt.getClaim(Claim.OTHER_SERVICE_ACCESS.getCode());
        if (isOtherServiceAccessPossible == null) {
            throw CustomAuthorizationException.of(ResultCode.Error.AUTH_INSUFFICIENT_PERMISSIONS, "isOtherServiceAccessPossible not found");
        }
        try {
            return Boolean.parseBoolean(isOtherServiceAccessPossible.toString());
        } catch (NumberFormatException e) {
            throw CustomAuthorizationException.of(ResultCode.Error.AUTH_INVALID_CREDENTIALS, "Invalid isOtherServiceAccessPossible format");
        }
    }

    /**
     * Subject 조회
     *  - 사용자 로그인아이디
     *  - 회사코드
     */
    public String getSubject() {
        Jwt jwt = getCurrentJwt();
        String subject = jwt.getSubject();
        if (subject == null) {
            throw CustomAuthorizationException.of(ResultCode.Error.AUTH_INSUFFICIENT_PERMISSIONS, "subject not found");
        }
        try {
            return subject;
        } catch (NumberFormatException e) {
            throw CustomAuthorizationException.of(ResultCode.Error.AUTH_INVALID_CREDENTIALS, "Invalid subject format");
        }
    }

    public boolean hasGoodsViewPermission(Integer goodsNo) {
        PermissionRes permissions = getPermissions();
        Map<Integer, Boolean> goodsViewableMap = permissions.getGoodsViewableMap(getCompanyNoWithPriority());
        return goodsViewableMap.getOrDefault(goodsNo, Boolean.FALSE);
    }

    public boolean hasGoodsDownloadPermission(Integer goodsNo) {
        PermissionRes permissions = getPermissions();
        Map<Integer, Boolean> goodsDownloadableMap = permissions.getGoodsDownloadableMap(getCompanyNoWithPriority());
        return goodsDownloadableMap.getOrDefault(goodsNo, Boolean.FALSE);
    }

    public PermissionRes getPermissions() {
        return switch (getCurrentRole()) {
            case USER -> goodsPermissionService.getUserGoodsPermissions(getUserNo());
            case ADMIN -> goodsPermissionService.getAccessAdminGoodsPermissions();
            case API_PLUS_OPERATOR -> goodsPermissionService.getPlusOperatorGoodsPermissions(getCompanyNo());
            default -> throw AppErrorException.of(ResultCode.Error.REQ_INVALID, "role is not supported");
        };
    }

}
