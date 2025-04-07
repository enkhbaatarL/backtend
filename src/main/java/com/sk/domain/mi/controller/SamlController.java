package com.sk.domain.mi.controller;

import java.net.URI;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Tag(name = "00. SAML API", description = "SAML API 입니다.")
@Slf4j
@RestController
@RequestMapping("/login/saml2")
@RequiredArgsConstructor
public class SamlController {

    @GetMapping("/user")
    public ResponseEntity<Void> loginUser() {
        return ResponseEntity.status(HttpStatus.FOUND)
            .location(URI.create("/saml2/authenticate?registrationId=mi-user"))
            .build();
    }

    @GetMapping("/admin")
    public ResponseEntity<Void> loginAdmin() {
        return ResponseEntity.status(HttpStatus.FOUND)
            .location(URI.create("/saml2/authenticate?registrationId=mi-admin"))
            .build();
    }

}
