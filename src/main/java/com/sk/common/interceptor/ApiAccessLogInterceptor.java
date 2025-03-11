package com.sk.common.interceptor;

import com.sk.common.utils.MDCUtil;
import com.sk.domain.shared.entity.master.tlm.ApiAccessLog;
import com.sk.domain.shared.repository.master.tim.ApiAccessLogRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.HandlerInterceptor;

@Slf4j
@Component
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ApiAccessLogInterceptor implements HandlerInterceptor {

    private final ApiAccessLogRepository apiAccessLogRepository;

    @Async
    @Override
    @Transactional
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {

        String apiEndpointUrl = request.getRequestURI();
        String httpMethodName = request.getMethod();
        String errorMessageContent = String.valueOf(request.getAttribute("exception"));

        ApiAccessLog accessLog = ApiAccessLog.of(apiEndpointUrl + "/" + request.getQueryString(), httpMethodName, "SYSTEM", MDCUtil.getValue(MDCUtil.REQUEST_BODY), errorMessageContent);
        apiAccessLogRepository.save(accessLog);

    }

}
