package com.sk.common.config.security.saml;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.saml2.provider.service.authentication.Saml2AuthenticatedPrincipal;
import org.springframework.security.saml2.provider.service.authentication.Saml2Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sk.common.utils.JwtUtil;
//import com.sk.domain.shared.entity.master.User;
//import com.sk.domain.shared.repository.master.UserRepository;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class CustomSamlAdminAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private final JwtUtil jwtUtil;
//    private final UserRepository userRepository;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
            Authentication authentication) throws IOException, ServletException {

        Saml2Authentication saml2Authentication = (Saml2Authentication) authentication;
        Saml2AuthenticatedPrincipal principal = (Saml2AuthenticatedPrincipal) saml2Authentication.getPrincipal();

        log.info("SAML 관리자 인증 성공 {}", principal.getName());

        // TODO: User / Admin 구분 해서 데이터 조회 필요

        // SAML 속성에서 사용자 정보 추출
//        String username = principal.getName();
//        User user = userRepository.findByUsername(username)
//                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
//
//        // JWT 토큰 생성
//        String accessToken = jwtUtil.generateAccessToken(user);
//        String refreshToken = jwtUtil.generateRefreshToken(username);

        // 토큰 응답 생성
        Map<String, Object> tokenResponse = new HashMap<>();
//        tokenResponse.put("access_token", accessToken);
//        tokenResponse.put("refresh_token", refreshToken);
        tokenResponse.put("token_type", "Bearer");

        // JSON 응답 전송
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(new ObjectMapper().writeValueAsString(tokenResponse));
    }
}
