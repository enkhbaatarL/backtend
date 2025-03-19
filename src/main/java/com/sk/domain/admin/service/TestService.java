package com.sk.domain.admin.service;

import com.sk.MailService;
import com.sk.domain.admin.dto.request.TestReq;
import com.sk.domain.admin.dto.response.TestRes;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class TestService {

    private final MailService mailService;

    public TestRes getUserInfo(TestReq testReq) {
        return TestRes.of(testReq);
    }

    public Boolean sendMail() {
        String to = "hoyoung@twolinecode.com";
        String subject = "Test Mail";
        String data = "asd";
        mailService.sendMail(to, subject, data);
        return true;
    }
}
