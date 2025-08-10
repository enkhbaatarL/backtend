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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class ApiAccessLogService {
    private final ApiAccessLogRepository apiAccessLogRepository;

    @Async
    @Transactional
    public void writeApiAccessLog() {

        String url = Optional.ofNullable(MDCUtil.getValue(MDCUtil.REQUEST_QUERY_STRING))
            .map(s -> StringUtils.join(MDCUtil.getValue(MDCUtil.REQUEST_URI), "?", MDCUtil.getValue(MDCUtil.REQUEST_QUERY_STRING)))
            .orElse(MDCUtil.getValue(MDCUtil.REQUEST_URI));

        if(url == null){
            log.info("Url is null. do not save.");
            return;
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        String role = null, subject = null;
        if (authentication != null && authentication.isAuthenticated() && !"anonymousUser".equals(authentication.getPrincipal())) {
            if (authentication instanceof JwtAuthenticationToken jwtToken) {
                role = jwtToken.getToken().getClaimAsString(Claim.ROLE.getCode());
                subject = jwtToken.getToken().getSubject();
            }
        }

        ApiAccessLog accessLog = ApiAccessLog.of(
            url,
            MDCUtil.getValue(MDCUtil.REQUEST_METHOD),
            StringUtil.stripNewlinesAndTabs(MDCUtil.getValue(MDCUtil.REQUEST_BODY)),
            MDCUtil.getValue(MDCUtil.RESPONSE_ERROR_MSG),
            MDCUtil.getValue(MDCUtil.ACCESS_IP_ADDRESS),
            ObjectUtils.isNotEmpty(role) ? Role.valueOf(role) : Role.SYSTEM,
            StringUtils.isNotEmpty(subject) ? subject : "SYSTEM"
        );

        apiAccessLogRepository.save(accessLog);
    }
}
