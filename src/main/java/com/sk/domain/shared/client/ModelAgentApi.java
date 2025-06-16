package com.sk.domain.shared.client;

import com.sk.domain.shared.client.dto.request.ModelAgentReq;
import com.sk.domain.shared.client.dto.response.ModelAgentRes;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

@HttpExchange
public interface ModelAgentApi {

    @PostExchange("/{uuid}/invoke")
    ModelAgentRes postMessage(@PathVariable("uuid") String uuid, @RequestBody ModelAgentReq req);

}
