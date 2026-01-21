package com.sk.common.aop;

import com.sk.common.annotation.RequiredPermission;
import com.sk.common.enums.ResultCode;
import com.sk.common.exception.CustomAuthorizationException;
import com.sk.domain.shared.enums.PermissionCondition;
import com.sk.domain.shared.enums.Role;
import com.sk.domain.shared.service.permissions.GoodsNestedEntityResolver;
import com.sk.domain.shared.service.permissions.PermissionStrategyFactory;
import com.sk.domain.shared.service.permissions.strategy.PermissionStrategy;
import java.util.Arrays;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.BooleanUtils;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class PermissionAspect {

    private final PermissionStrategyFactory strategyFactory;
    private final GoodsNestedEntityResolver goodsNestedEntityResolver;

    /**
     * 주어진 @RequiredPermission 어노테이션을 기반으로 API 접근 권한을 검사합니다.
     *
     * @param joinPoint          호출된 메서드의 실행 시점 정보
     * @param requiredPermission 메서드에 요구되는 권한 정보
     */
    @Before("@annotation(requiredPermission)")
    public void checkPermission(JoinPoint joinPoint, RequiredPermission requiredPermission) {

        // allowedRoles
        Role[] roles = requiredPermission.roles();
        PermissionCondition condition = requiredPermission.condition();

        PermissionStrategy strategy = strategyFactory.getStrategy(condition);

        List<String> allowedRoleCode = Arrays.stream(roles)
            .map(role -> (role == Role.ADMIN && condition == PermissionCondition.THIRD_PARTY_ADMIN_ONLY)
                ? "%s(%s)".formatted(role.getCode(), condition) : role.getCode()).toList();

        // 1. API 접근 가능 여부 체크
        if (BooleanUtils.isFalse(strategy.canAccessApi(roles, condition))) {
            throw CustomAuthorizationException.of(ResultCode.Error.AUTH_INSUFFICIENT_PERMISSIONS, "This API is allowed only for the following roles: %s".formatted(allowedRoleCode));
        }

        // 2-1. 상품 권한이 필요한 경우 (상품 하위 엔티티 식별자를 통해 접근)
        if (StringUtils.hasText(requiredPermission.goodsNestedParam())) {

            Object goodsNestedValue = extractParameterValue(joinPoint, requiredPermission.goodsNestedParam());
            Integer goodsNo = goodsNestedEntityResolver.resolve(requiredPermission.goodsNestedParam(), goodsNestedValue);

            if (goodsNo != null) {
                checkViewOrDownload(strategy, goodsNo, requiredPermission);
            }

        }

        // 2-2. 상품 권한이 필요한 경우 (상품 엔티티 식별자를 통해 접근)
        if (StringUtils.hasText(requiredPermission.goodsParam())) {

            Object goodsNoValue = extractParameterValue(joinPoint, requiredPermission.goodsParam());
            Integer goodsNo = toInt(goodsNoValue);

            if (goodsNo != null) {
                checkViewOrDownload(strategy, goodsNo, requiredPermission);
            }
        }

    }

    /**
     * 주어진 상품 번호(goodsNo)에 대해 보기(View) 또는 다운로드(Download) 권한을 확인합니다.
     *
     * @param strategy 권한 확인에 사용할 PermissionStrategy 객체입니다.
     * @param goodsNo  확인할 상품의 식별자 번호입니다.
     * @param rp       권한 확인 조건을 제공하는 RequiredPermission 객체입니다.
     */
    private void checkViewOrDownload(PermissionStrategy strategy, Integer goodsNo, RequiredPermission rp) {
        // [VIEW] 권한 체크
        if (rp.requireViewPermission() && BooleanUtils.isFalse(strategy.canViewGoods(goodsNo))) {
            throw CustomAuthorizationException.of(ResultCode.Error.AUTH_INSUFFICIENT_PERMISSIONS, "You do not have permission to 'VIEW' the goods(goodsNo: %s)".formatted(goodsNo));
        }
        // [DOWNLOAD] 권한 체크
        if (rp.requireDownloadPermission() && BooleanUtils.isFalse(strategy.canDownloadGoods(goodsNo))) {
            throw CustomAuthorizationException.of(ResultCode.Error.AUTH_INSUFFICIENT_PERMISSIONS, "You do not have permission to 'DOWNLOAD' the goods(goodsNo: %s)".formatted(goodsNo));
        }
    }

    /**
     * 메서드 호출 시 전달된 매개변수 중 지정된 이름(paramName)을 가진 값을 추출합니다.
     *
     * @param joinPoint 호출된 메서드의 실행 시점 정보를 포함합니다.
     * @param paramName 추출하려는 매개변수의 이름입니다.
     * @param <T>       반환값의 타입입니다.
     * @return 주어진 paramName에 해당하는 매개변수 값. 값이 없으면 null을 반환합니다.
     */
    private Object extractParameterValue(JoinPoint joinPoint, String paramName) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String[] parameterNames = signature.getParameterNames();
        Object[] args = joinPoint.getArgs();

        for (int i = 0; i < parameterNames.length; i++) {
            if (parameterNames[i].equals(paramName)) {
                return args[i];
            }
        }
        return null;
    }

    private Integer toInt(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Integer intValue) {
            return intValue;
        }
        if (value instanceof Number numberValue) {
            return Math.toIntExact(numberValue.longValue());
        }
        if (value instanceof String stringValue) {
            return Integer.valueOf(stringValue);
        }
        return null;
    }

}
