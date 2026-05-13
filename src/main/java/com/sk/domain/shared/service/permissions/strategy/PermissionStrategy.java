package com.sk.domain.shared.service.permissions.strategy;

import com.sk.common.annotation.PermissionRule;
import com.sk.domain.shared.enums.PermissionCondition;
import com.sk.domain.shared.enums.Role;

public interface PermissionStrategy {

    boolean canAccessApi(Role[] allowedRoles, PermissionCondition condition);

    default boolean canAccessApi(PermissionRule[] rules) {
        return false;
    }

    boolean canViewGoods(Integer goodsNo);

    boolean canDownloadGoods(Integer goodsNo);

}
