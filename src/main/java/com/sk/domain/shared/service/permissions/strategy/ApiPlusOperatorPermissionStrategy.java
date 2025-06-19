package com.sk.domain.shared.service.permissions.strategy;

import com.sk.domain.mi.dto.response.PermissionRes;
import com.sk.domain.shared.enums.PermissionCondition;
import com.sk.domain.shared.enums.Role;
import com.sk.domain.shared.service.permissions.AuthorizationService;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

/**
 * API_PLUS_OPERATOR 권한 전략
 * - API를 통하여 상품(Private) 업로드를 위한 토큰
 * - 자신의 회사가 '회사권한'을 가졌다면 해당하는 상품 리소스에 접근이 가능
 */
@RequiredArgsConstructor
public class ApiPlusOperatorPermissionStrategy implements PermissionStrategy {

    private final AuthorizationService authorizationService;

    @Override
    public boolean canAccessApi(Role[] allowedRoles, PermissionCondition condition) {
        return Arrays.stream(allowedRoles).anyMatch(role -> role == Role.API_PLUS_OPERATOR);
    }

    @Override
    public boolean canViewGoods(Integer goodsNo) {
        PermissionRes permissions = authorizationService.getPermissions();
        return permissions.getGoodsViewableMap(authorizationService.getCompanyNoWithPriority()).getOrDefault(goodsNo, Boolean.FALSE);
    }

    @Override
    public boolean canDownloadGoods(Integer goodsNo) {
        PermissionRes permissions = authorizationService.getPermissions();
        return permissions.getGoodsDownloadableMap(authorizationService.getCompanyNoWithPriority()).getOrDefault(goodsNo, Boolean.FALSE);
    }

    @Override
    public boolean canAccessCompany(Integer companyNo) {
        return companyNo.equals(authorizationService.getCompanyNo());
    }

}
