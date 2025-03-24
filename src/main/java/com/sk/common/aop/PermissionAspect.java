package com.sk.common.aop;

import com.sk.common.annotation.RequirePermission;
import com.sk.common.enums.ResultCode;
import com.sk.common.exception.CustomAuthorizationException;
import com.sk.domain.mi.dto.response.PermissionRes;
import com.sk.domain.mi.service.MiUserService;
import com.sk.domain.shared.service.AuthorizationService;
import com.sk.domain.shared.enums.Role;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Arrays;


@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class PermissionAspect {

    private final MiUserService userService;
    private final AuthorizationService authService;

    /**
     * RequirePermission 어노테이션이 적용된 메서드 실행 전에 권한을 검증
     *
     * @param joinPoint 조인 포인트
     */
    @Before("@annotation(com.sk.common.annotation.RequirePermission)")
    public void checkPermission(JoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        RequirePermission annotation = method.getAnnotation(RequirePermission.class);

        // 1. 관리자 권한 검증
        if (annotation.requireAdmin()) {
            authService.validateAdmin();
            return; // todo mi 서비스의 경우 타사서비스접속가능여부(otcmp_svc_accs_psbl_yn)를 확인해야할 수 있음
        }

        PermissionRes goodsPermissions = userService.getUserGoodsPermissions(authService.getUserNo());

        // 2. 역할 검증
        if (annotation.roles().length > 0) {
            Role currentRole = authService.getCurrentRole();
            boolean hasRequiredRole = Arrays.asList(annotation.roles()).contains(currentRole);
            if (!hasRequiredRole) {
                throw CustomAuthorizationException.of(ResultCode.Error.AUTH_INSUFFICIENT_PERMISSIONS);
            }
        }

        // 3. 회사 접근 권한 검증
        if (StringUtils.hasText(annotation.companyParam())) {
            Integer companyNo = extractParameterValue(joinPoint, annotation.companyParam(), Integer.class);
            if (companyNo != null) {
                authService.validateCompanyAccess(companyNo, goodsPermissions);
            }
        }

        // 4. 프로바이더 접근 권한 검증
        if (StringUtils.hasText(annotation.providerParam())) {
            Integer providerNo = extractParameterValue(joinPoint, annotation.providerParam(), Integer.class);
            if (providerNo != null) {
                authService.validateProviderAccess(providerNo, goodsPermissions);
            }
        }

        // 5. 상품 권한 검증
        if (StringUtils.hasText(annotation.goodsParam())) {
            Integer goodsNo = extractParameterValue(joinPoint, annotation.goodsParam(), Integer.class);
            if (goodsNo != null) {
                // 5-1. VIEW 권한 검증
                if (annotation.requireViewPermission()) {
                    authService.validateViewPermission(goodsNo, goodsPermissions);
                }

                // 5-2. DOWNLOAD 권한 검증
                if (annotation.requireDownloadPermission()) {
                    authService.validateDownloadPermission(goodsNo, goodsPermissions);
                }
            }
        }
    }

    /**
     * 메서드 파라미터에서 특정 파라미터 값을 추출합니다.
     *
     * @param joinPoint 조인 포인트
     * @param paramName 파라미터 이름
     * @param type 파라미터 타입
     * @return 파라미터 값
     */
    @SuppressWarnings("unchecked")
    private <T> T extractParameterValue(JoinPoint joinPoint, String paramName, Class<T> type) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        Parameter[] parameters = method.getParameters();
        Object[] args = joinPoint.getArgs();

        for (int i = 0; i < parameters.length; i++) {
            Parameter parameter = parameters[i];
            if (parameter.getName().equals(paramName)) {
                Object arg = args[i];
                if (arg != null && type.isAssignableFrom(arg.getClass())) {
                    return (T) arg;
                }
            }
        }

        // Path Variable이나 RequestParam을 찾는 추가 로직
        String[] parameterNames = signature.getParameterNames();
        for (int i = 0; i < parameterNames.length; i++) {
            if (parameterNames[i].equals(paramName)) {
                Object arg = args[i];
                if (arg != null && type.isAssignableFrom(arg.getClass())) {
                    return (T) arg;
                }
            }
        }

        return null;
    }
}
