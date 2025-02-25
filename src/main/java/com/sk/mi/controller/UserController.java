package com.sk.mi.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/user")
public class UserController {

    @PreAuthorize("hasRole('USER')")
    @GetMapping("/profile")
    public ResponseEntity<String> getUserProfile(Authentication authentication) {
        Jwt jwt = (Jwt) authentication.getPrincipal();
        String username = jwt.getSubject();
        return ResponseEntity.ok("user profile of " + username);
    }

    @PreAuthorize("hasAuthority('REFINITIV_VIEW')")
    @GetMapping("/data/view")
    public ResponseEntity<String> viewData() {
        return ResponseEntity.ok("Viewing Refinitiv Data.");
    }

    @PreAuthorize("hasAuthority('REFINITIV_DOWNLOAD')")
    @GetMapping("/data/download")
    public ResponseEntity<String> downloadData() {
        return ResponseEntity.ok("Downloading Refinitiv Data.");
    }

    @GetMapping("/claims")
    public ResponseEntity<Map<String, Object>> getClaims(JwtAuthenticationToken jwtToken) {
        Map<String, Object> claims = jwtToken.getToken().getClaims();
        return ResponseEntity.ok(claims);
    }
}
