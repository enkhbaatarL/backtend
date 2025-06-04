package com.sk.domain.shared.controller;

import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sk.common.annotation.ApiOrder;
import com.sk.common.model.BaseResponse;
import com.sk.common.model.BaseResponseFactory;
import com.sk.domain.shared.dto.response.auth.GenerateTokenRes;
import com.sk.domain.shared.service.AuthService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Tag(name = "02. Auth API", description = "Auth API 입니다.")
@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @ApiOrder(1)
    @Operation(summary = "토큰 갱신 API", description = "Access Token(헤더)과 Refresh Token(쿠키)을 갱신합니다. (Refresh Token Rotation)")
    @PostMapping("/token-generation")
    @ApiResponse(responseCode = "200", description = "SAML 콜백 성공")
    public BaseResponse<GenerateTokenRes> generateToken(
            @Parameter(hidden = true) @RequestHeader(value = "Authorization", required = true) String authHeader,
            @Parameter(hidden = true) @CookieValue(value = "X-Refresh-Token", required = true) String refreshToken,
            HttpServletResponse response) {
        return BaseResponseFactory
                .create(authService.generateToken(authHeader, refreshToken, response));
    }
}
