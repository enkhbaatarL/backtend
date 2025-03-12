package com.sk.common.interceptor;

import com.sk.common.utils.MDCUtil;
import com.sk.common.utils.StringUtil;
import com.sk.domain.shared.entity.master.tlm.ApiAccessLog;
import com.sk.domain.shared.repository.master.tlm.ApiAccessLogRepository;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
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

        ApiAccessLog accessLog = ApiAccessLog.of(
            StringUtils.join(MDCUtil.getValue(MDCUtil.REQUEST_URI), "?", MDCUtil.getValue(MDCUtil.REQUEST_QUERY_STRING)),
            MDCUtil.getValue(MDCUtil.REQUEST_METHOD),
            "SYSTEM", // todo Auditing
            StringUtil.stripNewlinesAndTabs(MDCUtil.getValue(MDCUtil.REQUEST_BODY)),
            MDCUtil.getValue(MDCUtil.RESPONSE_ERROR_MSG)
        );

        apiAccessLogRepository.save(accessLog);

    }

}
