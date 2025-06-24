package com.sk.domain.shared.service.permissions.strategy;

import com.sk.domain.shared.enums.PermissionCondition;
import com.sk.domain.shared.enums.Role;
import com.sk.domain.shared.service.permissions.AuthorizationService;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

/**
 * OPERATOR 권한 전략
 */
@RequiredArgsConstructor
public class OperatorPermissionStrategy implements PermissionStrategy {

    private final AuthorizationService authService;

    @Override
    public boolean canAccessApi(Role[] allowedRoles, PermissionCondition condition) {
        return Arrays.stream(allowedRoles).anyMatch(role -> role == Role.OPERATOR);
    }

    @Override
    public boolean canViewGoods(Integer goodsNo) {
        // 운영자는 상품에 접근 권한 없음
        return false;
    }

    @Override
    public boolean canDownloadGoods(Integer goodsNo) {
        // 운영자는 다운로드 권한도 없음
        return false;
    }

}

