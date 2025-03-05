package com.sk.domain.admin.service;

import com.sk.domain.admin.dto.request.TestReq;
import com.sk.domain.admin.dto.response.TestRes;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class TestService {

    public TestRes getUserInfo(TestReq testReq) {
        return TestRes.of(testReq);
    }

}
