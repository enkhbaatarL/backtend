package com.sk.domain.shared.service.permissions.strategy;

import com.sk.domain.mi.dto.response.PermissionRes;
import com.sk.domain.shared.enums.PermissionCondition;
import com.sk.domain.shared.enums.Role;
import com.sk.domain.shared.service.permissions.AuthorizationService;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

/**
 * OPERATOR 권한 전략
 * - OPERATOR 를 허용한 API 는 그대로 접근 가능
 * - [운영자 서비스 접속] 타사접속 가능 운영자는 자기 회사 USER 와 같은 권한으로 USER 허용 API 에도 접근 (회사는 token claim 으로 고정)
 */
@RequiredArgsConstructor
public class OperatorPermissionStrategy implements PermissionStrategy {

    private final AuthorizationService authService;

    @Override
    public boolean canAccessApi(Role[] allowedRoles, PermissionCondition condition) {
        if (Arrays.stream(allowedRoles).anyMatch(role -> role == Role.OPERATOR)) {
            return true;
        }
        if (!authService.isServiceAccessOperator()) {
            return false;
        }
        // [운영자 서비스 접속] USER 조건(SK가스 사용자 한정 등)도 USER 와 똑같이 평가한다
        PermissionStrategy userStrategy = condition == PermissionCondition.SKGAS_COMPANY_USER_ONLY
            ? new SKGasUserPermissionStrategy(authService)
            : new UserPermissionStrategy(authService);
        return userStrategy.canAccessApi(allowedRoles, condition);
    }

    @Override
    public boolean canViewGoods(Integer goodsNo) {
        // [운영자 서비스 접속] 타사접속 가능 운영자만 자기 회사 매핑 상품 조회 (불가 운영자는 기존대로 권한 없음)
        if (!authService.isServiceAccessOperator()) {
            return false;
        }
        PermissionRes permissions = authService.getPermissions();
        return permissions.getGoodsViewableMap(authService.getCompanyNo()).getOrDefault(goodsNo, Boolean.FALSE);
    }

    @Override
    public boolean canDownloadGoods(Integer goodsNo) {
        // [운영자 서비스 접속] 타사접속 가능 운영자만 자기 회사 매핑 상품 다운로드 (불가 운영자는 기존대로 권한 없음)
        if (!authService.isServiceAccessOperator()) {
            return false;
        }
        PermissionRes permissions = authService.getPermissions();
        return permissions.getGoodsDownloadableMap(authService.getCompanyNo()).getOrDefault(goodsNo, Boolean.FALSE);
    }

}

