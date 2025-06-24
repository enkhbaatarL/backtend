package com.sk.domain.shared.service.permissions.strategy;

import com.sk.domain.mi.dto.response.PermissionRes;
import com.sk.domain.shared.enums.PermissionCondition;
import com.sk.domain.shared.enums.Role;
import com.sk.domain.shared.service.permissions.AuthorizationService;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

/**
 * USER 권한 전략
 */
@RequiredArgsConstructor
public class UserPermissionStrategy implements PermissionStrategy {

    private final AuthorizationService authorizationService;

    @Override
    public boolean canAccessApi(Role[] allowedRoles, PermissionCondition condition) {
        return Arrays.stream(allowedRoles).anyMatch(role -> role == Role.USER);
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

}

