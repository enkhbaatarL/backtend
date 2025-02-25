package com.sk.mi.controller;

import com.sk.mi.request.AuthRequest;
import com.sk.mi.request.SignupRequest;
import com.sk.mi.response.AuthResponse;
import com.sk.mi.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

  private final AuthService authService;

  @PostMapping("/signup")
  public ResponseEntity<String> signup(@RequestBody SignupRequest signupRequest) {
    try{
      String message = authService.signUp(signupRequest);
      return ResponseEntity.ok(message);
    } catch (IllegalArgumentException e) {
      return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
    }
  }

  @PostMapping("/login")
  public ResponseEntity<AuthResponse> login(@RequestBody AuthRequest authRequest) {
    try{
      AuthResponse response = authService.login(authRequest);
      return ResponseEntity.ok(response);
    } catch (IllegalArgumentException e) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
    }
  }

  @PostMapping("/refresh")
  public ResponseEntity<String> refreshToken(@RequestHeader("Authorization") String refreshToken) {
    try {
      refreshToken = refreshToken.trim();
      String newAccessToken = authService.refreshToken(refreshToken);
      return ResponseEntity.ok(newAccessToken);
    } catch (IllegalArgumentException e) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(e.getMessage());
    }
  }
}