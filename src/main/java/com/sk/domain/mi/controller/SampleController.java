package com.sk.domain.mi.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/sample")
@RequiredArgsConstructor
public class SampleController {

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