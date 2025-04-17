package com.sk.domain.shared.controller;

import java.net.URI;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sk.common.annotation.ApiOrder;
import com.sk.common.model.BaseResponse;
import com.sk.common.model.BaseResponseFactory;
import com.sk.domain.shared.dto.request.saml.SamlCallbackReq;
import com.sk.domain.shared.service.SamlService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Tag(name = "00. SAML API", description = "SAML API 입니다.")
@Slf4j
@RestController
@RequestMapping("/login/saml2")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class SamlController {

    private final SamlService samlService;

    @GetMapping("/user")
    public ResponseEntity<Void> loginUser() {
        return ResponseEntity.status(HttpStatus.FOUND)
            .location(URI.create("/saml2/authenticate?registrationId=mi"))
            .build();
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
    
    @ApiOrder(1)
    @Operation(summary = "SAML 콜백 API", description = "SAML 콜백 API로 JWT 토큰을 발급합니다.")
    @ApiResponse(responseCode = "200", description = "JWT 토큰 발급 성공")
    @PostMapping("/callback")
    public BaseResponse<Boolean> callback(@RequestBody @Valid SamlCallbackReq samlCallbackReq, HttpServletResponse response) {
        return BaseResponseFactory.create(samlService.callback(samlCallbackReq, response));
    }
}
