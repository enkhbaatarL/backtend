package com.sk.domain.shared.controller;

import com.sk.domain.shared.dto.request.external.ExternalLoginReq;
import com.sk.domain.shared.dto.response.auth.GenerateTokenRes;
import com.sk.domain.shared.dto.response.external.ExternalLoginRes;
import com.sk.domain.shared.service.AuthService;
import com.sk.domain.shared.service.ExternalService;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import jakarta.validation.Valid;

import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.sk.common.annotation.ApiOrder;
import com.sk.common.model.BaseResponse;
import com.sk.common.model.BaseResponseFactory;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Profile("!dedicated")
@Tag(name = "07. External Login API", description = "외부 로그인 API 입니다.")
@Slf4j
@RestController
@RequestMapping("/api/v1/external")
@RequiredArgsConstructor
public class ExternalController {

    private final ExternalService externalService;
    private final AuthService authService;

    @ApiOrder(1)
    @Operation(summary = "외부 로그인", description = "아이디/패스워드를 이용한 외부 로그인을 진행합니다.")
    @ApiResponse(responseCode = "200", description = "외부 로그인 성공")
    @PostMapping("/login")
    public BaseResponse<ExternalLoginRes> externalLogin(
            @RequestBody @Valid ExternalLoginReq loginReq,
            HttpServletResponse response) {
        return BaseResponseFactory.create(externalService.externalLogin(loginReq, response));
    }

    @Parameters({
            @Parameter(name = "sessionId", description = "외부 로그인 세션 ID", required = true)
    })
    @ApiOrder(2)
    @Operation(summary = "외부 로그인 콜백", description = "외부 로그인 콜백을 진행합니다.")
    @ApiResponse(responseCode = "200", description = "외부 로그인 콜백 성공")
    @GetMapping("/callback")
    public BaseResponse<GenerateTokenRes> loginCallback(
            @RequestParam(value = "sessionId", required = true) String sessionId,
            HttpServletResponse response) {
        return BaseResponseFactory.create(authService.loginCallback(sessionId, response));
    }
}
