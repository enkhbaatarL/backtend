package com.sk.domain.shared.client;

import com.sk.domain.mi.dto.request.nextgen.EventRecommendationReq;
import com.sk.domain.mi.dto.response.nextgen.EventRecommendationRes;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.PostExchange;

public interface EventRecommendationAgentApi {
    @PostExchange(value = "/events/recommendations", contentType = "application/json", accept = "application/json")
    EventRecommendationRes recommend(@RequestBody EventRecommendationReq request);
}
