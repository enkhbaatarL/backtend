package com.sk.common.aop;

import com.sk.common.annotation.RequiredPermission;
import com.sk.common.enums.ResultCode;
import com.sk.common.exception.CustomAuthorizationException;
import com.sk.domain.shared.enums.PermissionCondition;
import com.sk.domain.shared.enums.Role;
import com.sk.domain.shared.service.permissions.GoodsNestedEntityResolver;
import com.sk.domain.shared.service.permissions.PermissionStrategyFactory;
import com.sk.domain.shared.service.permissions.strategy.PermissionStrategy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.BooleanUtils;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.List;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class PermissionAspect {

    private final PermissionStrategyFactory strategyFactory;
    private final GoodsNestedEntityResolver goodsNestedEntityResolver;

    /**
     * Performs a permission check for methods annotated with @RequiredPermission.
     *
     * @param joinPoint          The join point representing the intercepted method call.
     * @param requiredPermission The @RequiredPermission annotation specifying the required permissions.
     */
    @Before("@annotation(requiredPermission)")
    public void checkPermission(JoinPoint joinPoint, RequiredPermission requiredPermission) {

        // allowedRoles
        Role[] roles = requiredPermission.roles();
        PermissionCondition condition = requiredPermission.condition();

        PermissionStrategy strategy = strategyFactory.getStrategy(condition);

        List<String> allowedRoleCode = Arrays.stream(roles)
            .map(role -> {
                if (role == Role.ADMIN && condition == PermissionCondition.THIRD_PARTY_ADMIN_ONLY) {
                    return "%s(%s)".formatted(role.getCode(), condition);
                } else {
                    return "%s" .formatted(role.getCode());
                }
            })
            .toList();

        // 1. API 접근 가능 여부 체크
        if (BooleanUtils.isFalse(strategy.canAccessApi(roles, condition))) {
            throw CustomAuthorizationException.of(ResultCode.Error.AUTH_INSUFFICIENT_PERMISSIONS, "This API is allowed only for the following roles: %s".formatted(allowedRoleCode));
        }

        // 2-1. 상품 권한이 필요한 경우 (상품 하위 엔티티 식별자를 통해 접근)
        if (StringUtils.hasText(requiredPermission.goodsNestedParam())) {

            Integer goodsNestedNo = extractParameterValue(joinPoint, requiredPermission.goodsNestedParam());
            Integer goodsNo = goodsNestedEntityResolver.resolve(requiredPermission.goodsNestedParam(), goodsNestedNo);

            if (goodsNo != null) {
                checkViewOrDownload(strategy, goodsNo, requiredPermission);
            }

        }

        // 2-2. 상품 권한이 필요한 경우 (상품 엔티티 식별자를 통해 접근)
        if (StringUtils.hasText(requiredPermission.goodsParam())) {

            Integer goodsNo = extractParameterValue(joinPoint, requiredPermission.goodsParam());

            if (goodsNo != null) {
                checkViewOrDownload(strategy, goodsNo, requiredPermission);
            }

        }
    }

    private void checkViewOrDownload(PermissionStrategy strat, Integer goodsNo, RequiredPermission rp) {
        // [VIEW] 권한 체크
        if (rp.requireViewPermission() && BooleanUtils.isFalse(strat.canViewGoods(goodsNo))) {
            throw CustomAuthorizationException.of(ResultCode.Error.AUTH_INSUFFICIENT_PERMISSIONS, "You do not have permission to 'VIEW' the goods(goodsNo: %s)".formatted(goodsNo));
        }
        // [DOWNLOAD] 권한 체크
        if (rp.requireDownloadPermission() && BooleanUtils.isFalse(strat.canDownloadGoods(goodsNo))) {
            throw CustomAuthorizationException.of(ResultCode.Error.AUTH_INSUFFICIENT_PERMISSIONS, "You do not have permission to 'DOWNLOAD' the goods(goodsNo: %s)".formatted(goodsNo));
        }
    }

    @SuppressWarnings("unchecked")
    private <T> T extractParameterValue(JoinPoint joinPoint, String paramName) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String[] parameterNames = signature.getParameterNames();
        Object[] args = joinPoint.getArgs();

        for (int i = 0; i < parameterNames.length; i++) {
            if (parameterNames[i].equals(paramName)) {
                return (T) args[i];
            }
        }
        return null;
    }

}
