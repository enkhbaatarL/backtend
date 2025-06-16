package com.sk.domain.shared.client;

import com.sk.domain.shared.client.dto.request.ModelAgentReq;
import com.sk.domain.shared.client.dto.response.ModelAgentRes;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

@HttpExchange
public interface ModelAgentApi {

    @PostExchange("/8af869ae-fb18-4011-ad3f-df37728741e9/invoke")
    ModelAgentRes postMessage(@RequestBody ModelAgentReq req);

}
