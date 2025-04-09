package com.sk.domain.shared.service.auth;

import com.sk.common.annotation.RequirePermission;
import com.sk.common.enums.ResultCode;
import com.sk.common.exception.CustomAuthorizationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.lang.reflect.Method;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class PermissionAspect {

    private final PermissionStrategyFactory strategyFactory;

    @Before("@annotation(requirePermission)")
    public void checkPermission(JoinPoint joinPoint, RequirePermission requirePermission) {
        PermissionStrategy strategy = strategyFactory.getStrategy();

        // 1. API 접근 가능 여부 체크 (role 기준)
        if (!strategy.canAccessApi()) {
            throw CustomAuthorizationException.of(ResultCode.Error.AUTH_INSUFFICIENT_PERMISSIONS);
        }

        // 2. 상품 권한이 필요한 경우
        if (StringUtils.hasText(requirePermission.goodsParam())) {
            Integer goodsNo = extractParameterValue(joinPoint, requirePermission.goodsParam());

            if (goodsNo != null) {
                // 2-1. VIEW 권한 체크
                if (requirePermission.requireViewPermission() && !strategy.canViewGoods(goodsNo)) {
                    throw CustomAuthorizationException.of(ResultCode.Error.AUTH_INSUFFICIENT_PERMISSIONS, "You do not have permission to VIEW the goods(goodsNo: %s)".formatted(goodsNo));
                }

                // 2-2. DOWNLOAD 권한 체크
                if (requirePermission.requireDownloadPermission() && !strategy.canDownloadGoods(goodsNo)) {
                    throw CustomAuthorizationException.of(ResultCode.Error.AUTH_INSUFFICIENT_PERMISSIONS, "You do not have permission to DOWNLOAD the goods(goodsNo: %s)".formatted(goodsNo));
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    private <T> T extractParameterValue(JoinPoint joinPoint, String paramName) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
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
