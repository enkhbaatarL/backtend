package com.sk.domain.shared.service.permissions.strategy;

import com.sk.domain.mi.dto.response.PermissionRes;
import com.sk.domain.shared.enums.PermissionCondition;
import com.sk.domain.shared.enums.Role;
import com.sk.domain.shared.service.permissions.AuthorizationService;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

/**
 * 타사접근권한이 있는 ADMIN 권한 전략
 */
@RequiredArgsConstructor
public class AdminWithThirdPartyAccessStrategy implements PermissionStrategy {

    private final AuthorizationService authorizationService;

    @Override
    public boolean canAccessApi(Role[] allowedRoles, PermissionCondition condition) {
        return Arrays.stream(allowedRoles).anyMatch(role -> role == Role.ADMIN) && authorizationService.isThirdPartyAccessAllowed();
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
        // 타사 접근 권한이 있는 ADMIN은 모든 회사에 접근 가능
        return true;
    }

}


