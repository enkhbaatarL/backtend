package com.sk.domain.shared.client;

import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

import com.sk.domain.shared.client.dto.ModelAgentRes;

@HttpExchange
public interface ModelAgentApi {

    @PostExchange("/api/v1/smaple")
    ModelAgentRes postMessage(@RequestBody String req);

}
