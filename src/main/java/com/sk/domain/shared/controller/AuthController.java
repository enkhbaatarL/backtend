package com.sk.domain.shared.controller;

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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Tag(name = "01. Auth API", description = "Auth API 입니다.")
@Slf4j
@RestController
@RequestMapping("/api/v1/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    //TODO: 리프레시 토큰 => 쿠키로
    @ApiOrder(1)
    @Operation(summary = "토큰 갱신 API", description = "Access Token, Refresh Token을 갱신합니다. (Refresh Token Rotation)")
    @PostMapping("/token-generation")
    @ApiResponse(responseCode = "200", description = "토큰 갱신 성공")
    public BaseResponse<GenerateTokenRes> generateToken(
            @Parameter(hidden = true) @RequestHeader(value = "Authorization", required = true) String authHeader,
            @RequestHeader(value = "X-Refresh-Token", required = true) String refreshToken) {
        return BaseResponseFactory
                .create(authService.generateToken(authHeader, refreshToken));
    }
}
