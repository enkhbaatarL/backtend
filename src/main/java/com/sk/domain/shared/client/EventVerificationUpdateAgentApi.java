package com.sk.domain.shared.client;

import com.sk.domain.mi.dto.response.nextgen.EventVerificationUpdateRes;
import com.sk.domain.shared.client.dto.request.EventVerificationUpdateAgentReq;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.PostExchange;

public interface EventVerificationUpdateAgentApi {
    @PostExchange(value = "/events/{miEventId}/verification-updates", contentType = "application/json", accept = "application/json")
    EventVerificationUpdateRes update(@PathVariable("miEventId") Long miEventId,
                                      @RequestBody EventVerificationUpdateAgentReq request);
}
