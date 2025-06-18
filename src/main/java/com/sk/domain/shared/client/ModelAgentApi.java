package com.sk.domain.shared.client;

import com.sk.domain.shared.client.dto.request.ModelAgentReq;
import com.sk.domain.shared.client.dto.response.ModelAgentRes;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

@HttpExchange
public interface ModelAgentApi {

    @PostExchange("/e3064bc4-1871-4072-8f79-ed5271c50f81/invoke")
    ModelAgentRes postMessage(@RequestBody ModelAgentReq req);

}
