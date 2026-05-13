package com.sk.domain.shared.service.permissions.strategy;

import com.sk.common.annotation.PermissionRule;
import com.sk.domain.shared.enums.PermissionCondition;
import com.sk.domain.shared.enums.Role;
import java.util.Arrays;

/**
 * SYSTEM 권한 전략
 */
public class SystemPermissionStrategy implements PermissionStrategy {

    @Override
    public boolean canAccessApi(Role[] allowedRoles, PermissionCondition condition) {
        return Arrays.stream(allowedRoles).anyMatch(role -> role == Role.SYSTEM);
    }

    @Override
    public boolean canAccessApi(PermissionRule[] rules) {
        return Arrays.stream(rules).anyMatch(rule -> rule.role() == Role.SYSTEM);
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
