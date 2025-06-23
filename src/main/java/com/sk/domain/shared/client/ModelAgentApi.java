package com.sk.domain.shared.client;

import com.sk.domain.shared.client.dto.request.ModelAgentReq;
import com.sk.domain.shared.client.dto.response.ModelAgentRes;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

import java.util.Map;

@HttpExchange
public interface ModelAgentApi {

    @PostExchange("/{appId}/invoke")
    ModelAgentRes postMessage(@PathVariable String appId, @RequestHeader Map<String, Object> headers, @RequestBody ModelAgentReq req);

}
