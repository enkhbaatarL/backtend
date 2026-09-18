package com.sk.common.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

@Slf4j
@Component
public class ExceptionHandlerFilter extends OncePerRequestFilter implements Ordered {

    private final HandlerExceptionResolver handlerExceptionResolver;

    public ExceptionHandlerFilter(@Qualifier("handlerExceptionResolver") HandlerExceptionResolver handlerExceptionResolver) {
        this.handlerExceptionResolver = handlerExceptionResolver;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) {

        try {
            filterChain.doFilter(request, response);
        } catch (Exception e) {
            // 로깅은 위임받은 GlobalExceptionHandler가 담당하므로 여기서는 중복 로깅하지 않고, resolver가 처리하지 못한 예외만 최후 방어선으로 남긴다
            if (handlerExceptionResolver.resolveException(request, response, null, e) == null) {
                log.error("Unresolved error in filter chain : {}", e.getMessage(), e);
            }
        }

    }

    /**
     * HttpLoggingFilter 안쪽에 둔다.
     * 바깥에 두면 HttpLoggingFilter의 finally(응답 로깅, MDC 정리)가 먼저 끝난 뒤에 에러 로그와 에러 본문이 만들어져
     * 에러 로그의 uuid가 유실되고 res 로그의 body가 빈다.
     */
    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 1;
    }

}
