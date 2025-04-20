package com.sk.domain.shared.service.permissions.strategy;

import com.sk.domain.shared.enums.PermissionCondition;
import com.sk.domain.shared.enums.Role;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

/**
 * API_SUB_ADMIN 역할을 위한 권한 전략
 * - API를 통하여 RPA 상품(Public) 업로드를 위한 토큰
 */
@RequiredArgsConstructor
public class ApiSubAdminPermissionStrategy implements PermissionStrategy {

    @Override
    public boolean canAccessApi(Role[] allowedRoles, PermissionCondition condition) {
        return Arrays.stream(allowedRoles).anyMatch(role -> role == Role.API_SUB_ADMIN);
    }

    @Override
    public boolean canViewGoods(Integer goodsNo) {
        // 서브 관리자는 상품 조회 권한 없음
        return false;
    }

    @Override
    public boolean canDownloadGoods(Integer goodsNo) {
        // 서브 관리자는 상품 다운로드 권한 없음
        return false;
    }

}
