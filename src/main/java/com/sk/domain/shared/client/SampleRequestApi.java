package com.sk.domain.shared.client;

import com.sk.domain.shared.client.dto.SampleReq;
import com.sk.domain.shared.client.dto.SampleRes;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

@HttpExchange
public interface SampleRequestApi {

    @PostExchange("/api/v1/smaple")
    SampleRes postInferenceExecution(@RequestBody SampleReq sampleReq);

}
