package com.sk.domain.shared.controller;

import java.net.URI;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.sk.common.annotation.ApiOrder;
import com.sk.common.model.BaseResponse;
import com.sk.common.model.BaseResponseFactory;
import com.sk.domain.shared.service.SamlService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Tag(name = "00. SAML API", description = "SAML API 입니다.")
@Slf4j
@RestController
@RequestMapping("/api/v1/saml2")
@RequiredArgsConstructor
public class SamlController {

    private final SamlService samlService;

    @ApiOrder(1)
    @Operation(summary = "SAML 로그인 (호출이 아닌 해당 결로로 리다이렉트)", description = "SAML 로그인을 진행합니다.")
    @ApiResponse(responseCode = "200", description = "SAML 로그인 성공")
    @GetMapping("/login/user")
    public ResponseEntity<Void> loginUser() {
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create("/saml2/authenticate?registrationId=mi"))
                .build();
    }

    @Parameters({
        @Parameter(name = "sessionId", description = "SAML 세션 ID", required = true)
    })
    @ApiOrder(2)
    @Operation(summary = "SAML 콜백", description = "SAML 콜백을 진행합니다.")
    @ApiResponse(responseCode = "200", description = "SAML 콜백 성공")
    @GetMapping("/callback")
    public BaseResponse<Boolean> samlCallback(
            @RequestParam(value = "sessionId", required = true) String sessionId,
            HttpServletResponse response) {
        return BaseResponseFactory.create(samlService.samlCallback(sessionId, response));
    }

    /**
     * 정책 변경에 따른 미사용
    @GetMapping("/admin")
    public ResponseEntity<Void> loginAdmin() {
        return ResponseEntity.status(HttpStatus.FOUND)
            .location(URI.create("/saml2/authenticate?registrationId=mi-admin"))
            .build();
    }
    */
}
