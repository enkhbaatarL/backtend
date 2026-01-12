package com.sk.domain.shared.client;

import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

import com.sk.domain.shared.client.dto.response.AuthTokenRes;
import org.springframework.web.service.annotation.PostExchange;

@HttpExchange
public interface AuthTokenApi {
    @PostExchange(value = "/api/v1/auth/login", contentType = "application/x-www-form-urlencoded")
    AuthTokenRes auth(
        @RequestParam("grant_type") String grantType,
        @RequestParam("username") String username,
        @RequestParam("password") String password,
        @RequestParam("client_id") String clientId
    );
}
