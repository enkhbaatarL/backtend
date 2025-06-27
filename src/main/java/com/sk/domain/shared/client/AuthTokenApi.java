package com.sk.domain.shared.client;

import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

import com.sk.domain.shared.client.dto.response.AuthTokenRes;

@HttpExchange
public interface AuthTokenApi {

    @GetExchange("/collector/auth/token")
    AuthTokenRes getToken();
}
