package com.sk.mi.controller;

import com.sk.mi.request.AuthRequest;
import com.sk.mi.request.SignupRequest;
import com.sk.mi.response.AuthResponse;
import com.sk.mi.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/sample")
@RequiredArgsConstructor
public class SampleController {

  private final AuthService authService;

  @PostMapping("/use")
  public ResponseEntity<String> use() {
    try{
      String message = "jwt use test";
      return ResponseEntity.ok(message);
    } catch (IllegalArgumentException e) {
      return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
    }
  }
}