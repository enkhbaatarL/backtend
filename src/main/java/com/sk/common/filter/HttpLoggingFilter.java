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

    /**
     * 이 필터는 최외곽이라 여기서 나간 예외는 아무도 처리하지 못한다.
     * 따라서 로깅·MDC 작업은 모두 예외를 삼켜 요청 처리에 영향을 주지 않고,
     * 밖으로 나가는 예외는 filterChain 에서 올라온 것(안쪽 ExceptionHandlerFilter 가 이미 처리한 것)만 남긴다.
     * MDC 와 데이터소스 키 정리는 어떤 경로로 끝나든 실행되도록 최상위 finally 에 둔다.
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

        if (passUriStartsWith.stream().anyMatch(pass -> request.getRequestURI().startsWith(pass))) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            runQuietly("request header logging", () -> logHeaders(request));

            String contentType = request.getContentType();
            String requestUri = request.getRequestURI();

            if (isMultipartRequest(contentType) || isDownloadResponse(requestUri)) {
                doFilterWithoutCaching(request, response, filterChain);
                return;
            }

            CustomContentCachingRequestWrapper cachingRequestWrapper = cacheRequest(request);

            // 요청 본문 캐싱은 로깅 목적이므로, 실패해도 요청 자체는 캐싱 없이 진행한다
            if (cachingRequestWrapper == null) {
                doFilterWithoutCaching(request, response, filterChain);
                return;
            }

            if (isStreamResponse(requestUri)) {
                runQuietly("MDC initialize", () -> initializeMDC(cachingRequestWrapper));
                runQuietly("request logging", () -> logRequest(request));

                try {
                    filterChain.doFilter(cachingRequestWrapper, response);
                } finally {
                    runQuietly("response logging", () -> logResponse(response));
                }
                return;
            }

            CustomContentCachingResponseWrapper cachingResponseWrapper = new CustomContentCachingResponseWrapper(response);

            runQuietly("MDC initialize", () -> initializeMDC(cachingRequestWrapper));
            runQuietly("request logging", () -> logRequest(cachingRequestWrapper));

            try {
                filterChain.doFilter(cachingRequestWrapper, cachingResponseWrapper);
            } finally {
                // 응답 로깅이 실패해도 본문 전송은 반드시 수행한다
                runQuietly("response logging", () -> logResponse(cachingResponseWrapper));
                cachingResponseWrapper.copyBodyToResponse();
            }

        } finally {
            MDCUtil.clear();
            DataSourceContextHolder.clearDataSourceKey();
        }

    }

    /**
     * 요청/응답 본문 캐싱 없이 처리한다. multipart·다운로드 응답과 본문 캐싱 실패가 같은 경로를 쓴다.
     */
    private void doFilterWithoutCaching(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
        throws ServletException, IOException {

        runQuietly("MDC initialize", () -> initializeMDC(request));
        runQuietly("request logging", () -> logRequest(request));

        try {
            filterChain.doFilter(request, response);
        } finally {
            runQuietly("response logging", () -> logResponse(response));
        }
    }

    /**
     * @return 본문을 캐싱한 request wrapper, 캐싱에 실패하면 null
     */
    private CustomContentCachingRequestWrapper cacheRequest(HttpServletRequest request) {
        try {
            return new CustomContentCachingRequestWrapper(request);
        } catch (IOException e) {
            log.warn("request body caching failed, proceeding without caching : {}", e.getMessage());
            return null;
        }
    }

    /**
     * 로깅 실패가 요청 처리나 컨텍스트 정리를 막지 않도록 예외를 삼킨다.
     */
    private void runQuietly(String action, Runnable task) {
        try {
            task.run();
        } catch (Exception e) {
            log.warn("http logging failed - {} : {}", action, e.getMessage());
        }
    }

    // 26.07.29 취약점 진단 수정, '표준 라이브러리의 널 반환 값 역참조-102109,102110'
    private void logHeaders(HttpServletRequest request) {
        Enumeration<String> headerNames = request.getHeaderNames();

        while (headerNames != null && headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            String headerValue = request.getHeader(headerName);
            log.debug("Header: {}={}", headerName, headerValue);
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

    /**
     * 최외곽에 둔다.
     * ExceptionHandlerFilter보다 바깥이어야 에러 응답이 response wrapper에 담긴 뒤 이 필터의 finally가 실행되어,
     * 에러 로그에 uuid가 남고 res 로그의 body에 에러 본문이 기록된다.
     */
    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
