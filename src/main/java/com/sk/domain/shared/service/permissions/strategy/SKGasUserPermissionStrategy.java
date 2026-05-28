package com.sk.domain.shared.service.permissions.strategy;

import com.sk.domain.mi.dto.response.PermissionRes;
import com.sk.domain.shared.enums.PermissionCondition;
import com.sk.domain.shared.enums.Role;
import com.sk.domain.shared.service.permissions.AuthorizationService;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

/**
 * SK Gas USER 권한 전략
 */
@RequiredArgsConstructor
public class SKGasUserPermissionStrategy implements PermissionStrategy {

    private final AuthorizationService authorizationService;

    @Override
    public boolean canAccessApi(Role[] allowedRoles, PermissionCondition condition) {
        return Arrays.stream(allowedRoles)
            .filter(user -> condition == PermissionCondition.SKGAS_COMPANY_USER_ONLY)
            .anyMatch(role -> role == Role.USER)
            && PermissionCondition.AllowedCompany.SKGAS.getCode().equals(authorizationService.getCompanyCd());
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

