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
        // URL 경로 기반으로 파일명 결정
        if (path.contains("/notice/filter")) {
            return "notice-filter";
        } else if (path.matches(".*/notices/\\d+")) {
            return "notice-detail";
        } else if (path.contains("/notices/popup")) {
            return "notice-popup";
        } else if (path.contains("/notice")) {
            return "notice-list";
        } else if (path.contains("/faq/filter")) {
            return "faq-filter";
        } else if (path.contains("/faq")) {
            return "faq-list";
        } else if (path.matches(".*/dashboard/companies/\\d+/market-datas")) {
            return "dashboard-market-datas";
        } else if (path.matches(".*/dashboard/companies/\\d+/data/exists")) {
            return "dashboard-exists";
        } else if (path.contains("/newsletter/featured")) {
            return "newsletter-featured";
        } else if (path.matches(".*/newsletter/\\d+/\\d+$")) {
            return "newsletter-detail";
        } else if (path.matches(".*/newsletter/?$")) {
            return "newsletter-list";
        }

        // 매칭되는 패턴이 없으면 null 반환
        return null;
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
                }

                // 일반 데이터 타입
                Class<?> dataClass = (Class<?>) dataType;
                Object data = objectMapper.readValue(resource.getInputStream(), dataClass);
                return BaseResponseFactory.create(data);
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
}
