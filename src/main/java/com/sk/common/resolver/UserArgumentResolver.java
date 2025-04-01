package com.sk.common.resolver;

import org.springframework.core.MethodParameter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import com.sk.common.annotation.UserResolve;
import com.sk.common.enums.ResultCode;
import com.sk.common.exception.AppErrorException;
import com.sk.domain.shared.entity.master.tmd.User;
import com.sk.domain.shared.repository.master.tmd.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
@RequiredArgsConstructor
public class UserArgumentResolver implements HandlerMethodArgumentResolver {

    private final UserRepository userRepository;

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        // @UserResolve 어노테이션이 붙어있는 파라미터인지 확인
        return parameter.hasParameterAnnotation(UserResolve.class) && parameter.getParameterType().equals(User.class);
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
            NativeWebRequest webRequest, WebDataBinderFactory binderFactory) throws Exception {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw AppErrorException.of(ResultCode.Error.AUTH_REQUIRED, "User is not authenticated");
        }

        JwtAuthenticationToken jwtToken = (JwtAuthenticationToken) authentication.getPrincipal();
        String userNo = jwtToken.getToken().getClaimAsString("userNo");


        if (userNo == null) {
            throw AppErrorException.of(ResultCode.Error.AUTH_REQUIRED, "User is not authenticated");
        }

        return userRepository.findByUserNoAndIsDeletedFalse(Integer.parseInt(userNo))
                .orElseThrow(() -> AppErrorException.of(ResultCode.Error.AUTH_REQUIRED, "User is not authenticated"));
    }
}
