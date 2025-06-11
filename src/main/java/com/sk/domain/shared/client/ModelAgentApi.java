package com.sk.domain.shared.client;

import com.sk.domain.shared.client.dto.request.ModelAgentReq;
import com.sk.domain.shared.client.dto.response.ModelAgentRes;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

@HttpExchange
public interface ModelAgentApi {

    @PostExchange("/api/v1/smaple")
    ModelAgentRes postMessage(@RequestBody ModelAgentReq req);

}
