package com.sk.common.interceptor;

import com.sk.common.utils.MDCUtil;
import com.sk.common.utils.StringUtil;
import com.sk.domain.shared.entity.master.tlm.ApiAccessLog;
import com.sk.domain.shared.enums.Claim;
import com.sk.domain.shared.enums.Role;
import com.sk.domain.shared.repository.master.tlm.ApiAccessLogRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class ApiAccessLogInterceptor implements HandlerInterceptor {

    private final ApiAccessLogService apiAccessLogService;

    @Override
    @Transactional
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        apiAccessLogService.writeApiAccessLog();
    }



}
