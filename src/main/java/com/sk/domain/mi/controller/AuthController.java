//package com.sk.domain.mi.controller;
//
//import com.sk.domain.mi.dto.request.AuthReq;
//import com.sk.domain.mi.dto.request.SignupReq;
//import com.sk.domain.mi.dto.response.AuthRes;
//import com.sk.domain.mi.service.AuthService;
//import lombok.RequiredArgsConstructor;
//import org.springframework.http.HttpStatus;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.*;
//
//@RestController
//@RequestMapping("/api/auth")
//@RequiredArgsConstructor
//public class AuthController {
//
//  private final AuthService authService;
//
//  @PostMapping("/signup")
//  public ResponseEntity<String> signup(@RequestBody SignupReq signupReq) {
//    try{
//      String message = authService.signUp(signupReq);
//      return ResponseEntity.ok(message);
//    } catch (IllegalArgumentException e) {
//      return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
//    }
//  }
//
//  @PostMapping("/login")
//  public ResponseEntity<AuthRes> login(@RequestBody AuthReq authReq) {
//    try{
//      AuthRes response = authService.login(authReq);
//      return ResponseEntity.ok(response);
//    } catch (IllegalArgumentException e) {
//      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
//    }
//  }
//
//  @PostMapping("/refresh")
//  public ResponseEntity<String> refreshToken(@RequestHeader("Authorization") String refreshToken) {
//    try {
//      refreshToken = refreshToken.trim();
//      String newAccessToken = authService.refreshToken(refreshToken);
//      return ResponseEntity.ok(newAccessToken);
//    } catch (IllegalArgumentException e) {
//      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(e.getMessage());
//    }
//  }
//}
