package com.sk.domain.shared.client;

import com.sk.domain.mi.dto.request.nextgen.EventVerificationReq;
import com.sk.domain.mi.dto.response.nextgen.EventVerificationRes;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.PostExchange;

public interface EventVerificationAgentApi {
    @PostExchange(value = "/events/verifications", contentType = "application/json", accept = "application/json")
    EventVerificationRes verify(@RequestBody EventVerificationReq request);
}
