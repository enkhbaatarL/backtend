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
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
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
    @ApiResponse(responseCode = "200", description = "토큰 갱신 성공", headers = {
            @Header(name = "Authorization", description = "새로운 Access Token (형식: Bearer <token>)", schema = @Schema(type = "string")),
            @Header(name = "Set-Cookie", description = "새로운 Refresh Token (쿠키: X-Refresh-Token=<token>; Max-Age=604800; Path=/; Secure; HttpOnly;)", schema = @Schema(type = "string"))
    }, content = @Content(mediaType = "application/json", schema = @Schema(implementation = BaseResponse.class), examples = @ExampleObject(value = """
            {
                "status": true,
                "code": "string",
                "message": "string",
                "data": true
            }
            """)))
    public BaseResponse<GenerateTokenRes> generateToken(
            @Parameter(hidden = true) @RequestHeader(value = "Authorization", required = true) String authHeader,
            @Parameter(description = "Refresh Token (쿠키에서 전달)", required = true) @CookieValue(value = "X-Refresh-Token", required = true) String refreshToken) {
        return BaseResponseFactory
                .create(authService.generateToken(authHeader, refreshToken));
    }
}
