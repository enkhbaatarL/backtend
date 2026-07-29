package com.sk.common.aop;

import java.util.List;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.sk.common.enums.ResultCode;
import com.sk.common.exception.AppErrorException;
import com.sk.domain.shared.entity.master.tam.AdminPermission;
import com.sk.domain.shared.entity.master.tmd.User;
import com.sk.domain.shared.enums.Claim;
import com.sk.domain.shared.enums.Role;
import com.sk.domain.shared.repository.master.tam.AdminPermissionRepository;
import com.sk.domain.shared.repository.master.tmd.UserRepository;
import com.sk.domain.shared.service.CommonService;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class AccessIpCheckAspect {

    private final CommonService commonService;
    private final UserRepository userRepository;

    @Around("@annotation(com.sk.common.annotation.AccessIpCheck) || @within(com.sk.common.annotation.AccessIpCheck)")
    public Object accessIpCheck(ProceedingJoinPoint joinPoint) throws Throwable {

        String userNo = null;

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            log.info("Authentication is Fail. Auth={}", authentication);
            if(authentication != null){
                log.info("Authentication is Fail. isAuthenticated={}", authentication.isAuthenticated());
                log.info("Authentication is Fail. Principal={}", authentication.getPrincipal());
            }

            throw AppErrorException.of(ResultCode.Error.AUTH_REQUIRED, "User is not authenticated");
        }

        if (authentication instanceof JwtAuthenticationToken jwtToken) {
            String role = jwtToken.getToken().getClaimAsString(Claim.ROLE.getCode());
            if (!role.equals(Role.ADMIN.getCode()) && !role.equals(Role.OPERATOR.getCode())) {
                return joinPoint.proceed();
            }
            userNo = jwtToken.getToken().getClaimAsString(Claim.USER_NO.getCode());
            if (userNo == null) {
                throw AppErrorException.of(ResultCode.Error.AUTH_REQUIRED, "User is not authenticated");
            }
        }

        User user = userRepository.findByUserNoAndIsDeletedFalse(Integer.parseInt(userNo))
            .orElseThrow(() -> AppErrorException.of(ResultCode.Error.AUTH_REQUIRED, "User is not authenticated"));

        // 26.07.29 취약점 진단 수정, '표준 라이브러리의 널 반환 값 역참조-102108'
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

        if (attributes == null) {
            throw AppErrorException.of(ResultCode.Error.AUTH_REQUIRED, "Request context is not available.");
        }

        HttpServletRequest request = attributes.getRequest();

        // 허용 IP 검증
        commonService.checkIp(user, request);

        // 문제 없으면 다음 처리로 넘어감
        return joinPoint.proceed();
    }
}
