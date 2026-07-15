package com.sk.common.filter;

import com.sk.common.config.database.DataSourceContextHolder;
import com.sk.common.filter.wrapper.CustomContentCachingRequestWrapper;
import com.sk.common.filter.wrapper.CustomContentCachingResponseWrapper;
import com.sk.common.utils.MDCUtil;
import com.sk.common.utils.StringUtil;
import com.sk.domain.shared.service.permissions.AuthorizationService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

@Slf4j
@Component
@RequiredArgsConstructor
public class HttpLoggingFilter extends OncePerRequestFilter implements Ordered {

    // Custom Header Key
    private static final String X_COMPANY_NO_KEY = "X-Company-No";

    private final List<String> passUriStartsWith = List.of("/swagger-ui", "/v3/api-docs", "/favicon.ico");
    private final AtomicLong id = new AtomicLong(0);

    private final AuthorizationService authorizationService;

    public static void main(String[] arges){

        String test = "http://localhost:8080/api/v1/mi/mari-agent/chat-rooms/8926f090-952f-4003-8165-caf1127db1f3/stream";
        System.out.println(test.matches(".*/stream/?(\\\\?.*)?$"));

    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

        Enumeration<String> headerNames = request.getHeaderNames();

        if (passUriStartsWith.stream().anyMatch(pass -> request.getRequestURI().startsWith(pass))) {
            filterChain.doFilter(request, response);
            return;
        }

        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            String headerValue = request.getHeader(headerName);
            log.debug("Header: {}={}", headerName, headerValue);
        }

        String contentType = request.getContentType();
        String requestUri = request.getRequestURI();

        if (isMultipartRequest(contentType) || isDownloadResponse(requestUri)) {

            initializeMDC(request);
            logRequest(request);

            try {
                filterChain.doFilter(request, response);
            } finally {
                logResponse(response);
                MDCUtil.clear();
                DataSourceContextHolder.clearDataSourceKey();
            }

        } else if (isStreamResponse(requestUri)){
            CustomContentCachingRequestWrapper cachingRequestWrapper = new CustomContentCachingRequestWrapper(request);
            initializeMDC(cachingRequestWrapper);
            logRequest(request);

            try {
                filterChain.doFilter(cachingRequestWrapper, response);
            } finally {
                logResponse(response);
                MDCUtil.clear();
                DataSourceContextHolder.clearDataSourceKey();
            }
        } else {

            CustomContentCachingRequestWrapper cachingRequestWrapper = new CustomContentCachingRequestWrapper(request);
            CustomContentCachingResponseWrapper cachingResponseWrapper = new CustomContentCachingResponseWrapper(response);

            initializeMDC(cachingRequestWrapper);
            logRequest(cachingRequestWrapper);

            try {
                filterChain.doFilter(cachingRequestWrapper, cachingResponseWrapper);
            } finally {
                logResponse(cachingResponseWrapper);
                cachingResponseWrapper.copyBodyToResponse();
                MDCUtil.clear();
                DataSourceContextHolder.clearDataSourceKey();
            }

        }

    }

    /**
     * MDC initialize
     */
    private void initializeMDC(HttpServletRequest request) {
        long reqSeqId = id.incrementAndGet();
        long startTime = System.currentTimeMillis();
        MDCUtil.setValue(MDCUtil.REQUEST_SEQ_ID, String.valueOf(reqSeqId));
        MDCUtil.setValue(MDCUtil.REQUEST_METHOD, request.getMethod());
        MDCUtil.setValue(MDCUtil.REQUEST_UUID, RandomStringUtils.secure().next(8, true, true));
        MDCUtil.setValue(MDCUtil.REQUEST_START_TIME, String.valueOf(startTime));
        MDCUtil.setValue(MDCUtil.REQUEST_URI, request.getRequestURI());
        MDCUtil.setValue(MDCUtil.ACCESS_IP_ADDRESS, request.getRemoteAddr());
        MDCUtil.setValue(MDCUtil.REQUEST_X_COMPANY_NO, request.getHeader(X_COMPANY_NO_KEY));
    }

    /**
     * MDC initialize
     *
     * @param request CustomContentCachingRequestWrapper
     */
    private void initializeMDC(CustomContentCachingRequestWrapper request) {
        long reqSeqId = id.incrementAndGet();
        long startTime = System.currentTimeMillis();
        MDCUtil.setValue(MDCUtil.REQUEST_SEQ_ID, String.valueOf(reqSeqId));
        MDCUtil.setValue(MDCUtil.REQUEST_METHOD, request.getMethod());
        MDCUtil.setValue(MDCUtil.REQUEST_UUID, RandomStringUtils.secure().next(8, true, true));
        MDCUtil.setValue(MDCUtil.REQUEST_START_TIME, String.valueOf(startTime));
        MDCUtil.setJsonValue(MDCUtil.HEADER_MAP, request.getHeaderMap());
        MDCUtil.setValue(MDCUtil.REQUEST_BODY, new String(request.getContentAsByteArray(), StandardCharsets.UTF_8));
        MDCUtil.setValue(MDCUtil.REQUEST_URI, request.getRequestURI());
        MDCUtil.setValue(MDCUtil.REQUEST_QUERY_STRING, request.getQueryString());
        MDCUtil.setValue(MDCUtil.ACCESS_IP_ADDRESS, request.getRemoteAddr());
        MDCUtil.setValue(MDCUtil.REQUEST_X_COMPANY_NO, request.getHeader(X_COMPANY_NO_KEY));
    }

    /**
     * logging request
     *
     * @param request ContentCachingRequestWrapper
     */
    private void logRequest(ContentCachingRequestWrapper request) {

        String uri = request.getRequestURI();
        byte[] requestBody = request.getContentAsByteArray();

        if (!StringUtils.isEmpty(request.getQueryString()))
            uri += "?" + request.getQueryString();

        log.info("req uri={}, method={}, body={}, ip={}",
            uri,
            request.getMethod(),
            StringUtil.stripNewlinesAndTabs(new String(requestBody, StandardCharsets.UTF_8)),
            request.getRemoteAddr());

    }

    private void logRequest(HttpServletRequest request) {

        String uri = request.getRequestURI();

        if (!StringUtils.isEmpty(request.getQueryString()))
            uri += "?" + request.getQueryString();

        log.info("req uri={}, method={}, ip={}", uri, request.getMethod(), request.getRemoteAddr());

    }

    /**
     * logging response
     *
     * @param response ContentCachingResponseWrapper
     */
    private void logResponse(ContentCachingResponseWrapper response) {

        String uri = MDCUtil.getValue(MDCUtil.REQUEST_URI);
        long startTime = Long.parseLong(MDCUtil.getValue(MDCUtil.REQUEST_START_TIME));

        if (uri.equalsIgnoreCase("/whoami"))
            return;

        if(response.getContentType() != null && response.getContentType().contains("octet-stream")){
            log.info("res uri={}, totalTime={}, body skip(octet-stream)",
                uri,
                System.currentTimeMillis() - startTime);
        }else{
            log.info("res uri={}, body={}, totalTime={}",
                uri,
                new String(response.getContentAsByteArray(), StandardCharsets.UTF_8),
                System.currentTimeMillis() - startTime);
        }
    }

    /**
     * logging response
     */
    private void logResponse(HttpServletResponse response) {

        String uri = MDCUtil.getValue(MDCUtil.REQUEST_URI);
        long startTime = Long.parseLong(MDCUtil.getValue(MDCUtil.REQUEST_START_TIME));

        log.info("res uri={}, status={}, contentLength={}, totalTime={}",
            uri,
            response.getStatus(),
            response.getHeader("Content-Length"),
            System.currentTimeMillis() - startTime);

    }

    private boolean isMultipartRequest(String contentType) {
        return contentType != null && contentType.toLowerCase().startsWith("multipart/");
    }

    private boolean isDownloadResponse(String requestUri) {
        return requestUri.matches(".*/(download(s)?|export(s)?|file(s)?|attachment(s)?|image(s)?)(/.*)?$")
            || requestUri.matches(".*\\.(xlsx|xls|csv|pdf|zip|tar|7z|exe|jpg|png|mp4|mp3)$");
    }

    private boolean isStreamResponse(String requestUri) {
        return requestUri.matches(".*/stream/?(\\\\?.*)?$");
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 1;
    }
}
