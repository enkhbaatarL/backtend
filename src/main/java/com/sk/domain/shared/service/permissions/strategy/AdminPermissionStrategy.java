package com.sk.domain.shared.service.permissions.strategy;

import com.sk.domain.shared.enums.PermissionCondition;
import com.sk.domain.shared.enums.Role;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

/**
 * ADMIN 권한 전략
 */
@RequiredArgsConstructor
public class AdminPermissionStrategy implements PermissionStrategy {

    @Override
    public boolean canAccessApi(Role[] allowedRoles, PermissionCondition condition) {
        return Arrays.stream(allowedRoles).anyMatch(role -> role == Role.ADMIN);
    }

    @Override
    public boolean canViewGoods(Integer goodsNo) {
        // 관리자는 상품 조회 권한 없음
        return false;
    }

    @Override
    public boolean canDownloadGoods(Integer goodsNo) {
        // 관리자는 상품 다운로드 권한 없음
        return false;
    }

    @Override
    public boolean canAccessCompany(Integer companyNo) {
        // 관리자는 회사 접근 권한 없음
        return false;
    }
}

