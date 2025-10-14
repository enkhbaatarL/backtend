package com.sk.common.aop;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sk.common.model.BaseResponse;
import com.sk.common.model.BaseResponseFactory;
import com.sk.common.model.PaginationRes;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

@Slf4j
@Aspect
@Component
public class DummyResponseAspect {

    @Value("${dummy.enabled:false}")
    private boolean dummyResponseEnabled;

    @Value("${dummy.data.path:dummy-data}")
    private String dummyDataPath;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private HttpServletRequest request;

    @Around("execution(* com.sk.domain.mi.controller..*(..))")
    public Object handleDummyResponse(ProceedingJoinPoint joinPoint) throws Throwable {

        boolean isJapanese = isJapaneseLanguage();

        // 일본어이고, dummy 모드가 활성화된 경우
        if (isJapanese && dummyResponseEnabled) {
            return createDummyData(joinPoint);
        }

        return joinPoint.proceed();
    }

    private boolean isJapaneseLanguage() {
        String acceptLanguage = request.getHeader("accept-language");

        if (acceptLanguage == null || acceptLanguage.isEmpty()) {
            return false;
        }

        return acceptLanguage.startsWith("ja");
    }

    private Object createDummyData(ProceedingJoinPoint joinPoint) throws Throwable {
        try {
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            Method method = signature.getMethod();
            Class<?> returnType = signature.getReturnType();

            // HTTP 요청 경로로 JSON 파일명 결정
            String requestPath = request.getRequestURI();
            String jsonFileName = determineJsonFileName(requestPath, method);

            // 매칭되는 URL 패턴이 없으면 원래 API 호출
            if (jsonFileName == null) {
                log.debug("No matching dummy data pattern for: {}, proceeding with actual API",
                    requestPath);
                return joinPoint.proceed();
            }

            log.debug("Loading dummy data from: {}/{}.json", dummyDataPath, jsonFileName);

            return loadFromJson(jsonFileName, returnType, method);

        } catch (Exception e) {
            log.error("Failed to create dummy data", e);
            return null;
        }
    }

    private String determineJsonFileName(String path, Method method) {
        return Arrays.stream(PathPattern.values())
            .filter(pattern -> pattern.matches(path))
            .map(PathPattern::getFileName)
            .findFirst()
            .orElse(null);
    }

    private Object loadFromJson(String fileName, Class<?> returnType, Method method)
        throws IOException {
        String jsonFile = dummyDataPath + "/" + fileName + ".json";
        Resource resource = new ClassPathResource(jsonFile);

        if (!resource.exists()) {
            log.warn("Dummy data file not found: {}, returning empty response", jsonFile);
            return wrapInBaseResponse(generateDefaultDummy(returnType), returnType);
        }

        // BaseResponse 타입 체크
        if (BaseResponse.class.isAssignableFrom(returnType)) {
            return loadBaseResponse(resource, method);
        }

        // 일반 객체
        if (Collection.class.isAssignableFrom(returnType)) {
            JavaType javaType = objectMapper.getTypeFactory()
                .constructCollectionType(List.class, getGenericType(method));
            return objectMapper.readValue(resource.getInputStream(), javaType);
        }

        return objectMapper.readValue(resource.getInputStream(), returnType);
    }

    private Object loadBaseResponse(Resource resource, Method method) throws IOException {
        // BaseResponse의 제네릭 타입 추출
        Type returnType = method.getGenericReturnType();

        if (returnType instanceof ParameterizedType) {
            ParameterizedType paramType = (ParameterizedType) returnType;
            Type[] typeArgs = paramType.getActualTypeArguments();

            if (typeArgs.length > 0) {
                Type dataType = typeArgs[0];

                // PaginationRes<T> 타입 체크
                if (dataType instanceof ParameterizedType) {
                    ParameterizedType dataParamType = (ParameterizedType) dataType;

                    if (PaginationRes.class.isAssignableFrom(
                        (Class<?>) dataParamType.getRawType())) {
                        // PaginationRes의 제네릭 타입 추출
                        Type[] paginationTypeArgs = dataParamType.getActualTypeArguments();
                        Class<?> itemType = (Class<?>) paginationTypeArgs[0];

                        JavaType paginationType = objectMapper.getTypeFactory()
                            .constructParametricType(PaginationRes.class, itemType);

                        PaginationRes<?> paginationData = objectMapper.readValue(
                            resource.getInputStream(), paginationType);

                        return BaseResponseFactory.create(paginationData);
                    }

                    // 다른 ParameterizedType (예: List<T>, Set<T> 등) 처리
                    JavaType javaType = objectMapper.getTypeFactory()
                        .constructType(dataParamType);
                    Object data = objectMapper.readValue(resource.getInputStream(), javaType);
                    return BaseResponseFactory.create(data);
                }

                // 일반 데이터 타입 (Class)
                if (dataType instanceof Class) {
                    Class<?> dataClass = (Class<?>) dataType;
                    Object data = objectMapper.readValue(resource.getInputStream(), dataClass);
                    return BaseResponseFactory.create(data);
                }
            }
        }

        return null;
    }

    private Object wrapInBaseResponse(Object data, Class<?> returnType) {
        if (BaseResponse.class.isAssignableFrom(returnType)) {
            return BaseResponseFactory.create(data);
        }
        return data;
    }

    private Class<?> getGenericType(Method method) {
        Type returnType = method.getGenericReturnType();

        if (returnType instanceof ParameterizedType) {
            ParameterizedType paramType = (ParameterizedType) returnType;
            Type[] typeArgs = paramType.getActualTypeArguments();
            if (typeArgs.length > 0) {
                return (Class<?>) typeArgs[0];
            }
        }

        return Object.class;
    }

    private Object generateDefaultDummy(Class<?> returnType) {
        if (Collection.class.isAssignableFrom(returnType)) {
            return Collections.emptyList();
        }
        return null;
    }

    private enum PathPattern {
        // Notice 관련
        NOTICE_FILTER("/notice/filter", "notice-filter"),
        NOTICE_DETAIL(".*/notices/\\d+", "notice-detail", true),
        NOTICE_POPUP("/notices/popup", "notice-popup"),
        NOTICE_LIST("/notice", "notice-list"),

        // FAQ 관련
        FAQ_FILTER("/faq/filter", "faq-filter"),
        FAQ_LIST("/faq", "faq-list"),

        // Dashboard 관련
        DASHBOARD_MARKET_DATAS(".*/dashboard/companies/\\d+/market-datas", "dashboard-market-datas",
            true),
        DASHBOARD_EXISTS(".*/dashboard/companies/\\d+/data/exists", "dashboard-exists", true),
        DASHBOARD_SUMMARY(".*/dashboard/companies/\\d+/summary", "dashboard-summary", true),

        // Signal Map 관련
        SIGNAL_MAP_PERIODS(".*/signal-map/companies/\\d+/periods", "signal-map-periods", true),

        // Newsletter 관련
        NEWSLETTER_FEATURED("/newsletter/featured", "newsletter-featured"),
        NEWSLETTER_DETAIL(".*/newsletter/\\d+/\\d+$", "newsletter-detail", true),
        NEWSLETTER_LIST(".*/newsletter/?$", "newsletter-list", true),

        // Mari Agent 관련
        MAJOR_QUESTIONS("/mari-agent/major-questions", "major-questions");

        private final String pattern;
        private final String fileName;
        private final boolean isRegex;

        PathPattern(String pattern, String fileName) {
            this(pattern, fileName, false);
        }

        PathPattern(String pattern, String fileName, boolean isRegex) {
            this.pattern = pattern;
            this.fileName = fileName;
            this.isRegex = isRegex;
        }

        public boolean matches(String path) {
            return isRegex ? path.matches(pattern) : path.contains(pattern);
        }

        public String getFileName() {
            return fileName;
        }
    }
}
