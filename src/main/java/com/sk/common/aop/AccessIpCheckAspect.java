package com.sk.common.aop;

import java.util.List;

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
import com.sk.domain.shared.enums.UserRoleCd;
import com.sk.domain.shared.repository.master.tam.AdminPermissionRepository;
import com.sk.domain.shared.repository.master.tmd.UserRepository;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@Aspect
@Component
@RequiredArgsConstructor
public class AccessIpCheckAspect {
    
    private final AdminPermissionRepository adminPermissionRepository;
    private final UserRepository userRepository;
    
    @Around("@annotation(com.sk.common.annotation.AccessIpCheck) || @within(com.sk.common.annotation.AccessIpCheck)")
    public Object accessIpCheck(ProceedingJoinPoint joinPoint) throws Throwable {

        String userNo = null;

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
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

        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
        String clientIp = getClientIp(request);

        List<AdminPermission> existingPermissions = adminPermissionRepository.findByIdUserNoAndIsDeletedFalse(user.getUserNo());

        // IP 검증 로직 (예: 허용된 IP 목록에 clientIp가 포함되어 있는지 확인)
        boolean isAllowedIp = existingPermissions.stream()
                .anyMatch(permission -> permission.getId().getIpAddress().equals(clientIp)); // 허용 IP 필드가 있다고 가정

        if (!isAllowedIp) {
            throw AppErrorException.of(ResultCode.Error.AUTH_IP_NOT_ALLOWED, String.format("권한 없는 IP 접근: %s", clientIp));
        }

        // 문제 없으면 다음 처리로 넘어감
        return joinPoint.proceed();
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_CLIENT_IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // 다중 IP가 포함된 경우 첫 번째 IP 사용
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}
