package com.sk.domain.shared.controller;

import java.net.URI;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.sk.common.model.BaseResponse;
import com.sk.common.model.BaseResponseFactory;
import com.sk.domain.shared.service.SamlService;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

// @Tag(name = "00. SAML API", description = "SAML API 입니다.")
@Slf4j
@RestController
@RequestMapping("/saml2")
@RequiredArgsConstructor
public class SamlController {

    private final SamlService samlService;

    @GetMapping("/login/user")
    public ResponseEntity<Void> loginUser() {
        return ResponseEntity.status(HttpStatus.FOUND)
            .location(URI.create("/saml2/authenticate?registrationId=mi"))
            .build();
    }

    @GetMapping("/callback")
    public BaseResponse<Boolean> samlCallback(
        @RequestParam(value = "sessionId", required = true) String sessionId,
        HttpServletResponse response
    ) {
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
