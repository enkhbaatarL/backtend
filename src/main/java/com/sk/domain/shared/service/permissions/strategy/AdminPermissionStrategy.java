package com.sk.domain.shared.service.permissions.strategy;

import com.sk.domain.shared.enums.PermissionCondition;
import com.sk.domain.shared.enums.Role;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

@RequiredArgsConstructor
public class AdminPermissionStrategy implements PermissionStrategy {

    @Override
    public boolean canAccessApi(Role[] allowedRoles, PermissionCondition condition) {
        return Arrays.stream(allowedRoles).anyMatch(role -> role == Role.ADMIN);
    }

    @Override
    public boolean canViewGoods(Integer goodsNo) {
        return false;
    }

    @Override
    public boolean canDownloadGoods(Integer goodsNo) {
        return false;
    }

}

