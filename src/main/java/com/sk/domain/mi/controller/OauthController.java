package com.sk.domain.mi.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "01. Oauth2 API", description = "Oauth2 API 입니다.")
@Slf4j
@RestController
@RequestMapping("/oauth/token")
@RequiredArgsConstructor
public class OauthController {



}
