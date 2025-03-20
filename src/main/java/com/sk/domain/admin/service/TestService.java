package com.sk.domain.admin.service;

import com.sk.MailService;
import com.sk.common.utils.JwtUtil;
import com.sk.domain.admin.dto.request.TestReq;
import com.sk.domain.admin.dto.response.TestRes;
import com.sk.domain.shared.entity.master.tmd.User;
import com.sk.domain.shared.enums.UserRoleCd;
import com.sk.domain.shared.repository.master.tmd.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class TestService {

    private final JwtUtil jwtUtil;
    private final MailService mailService;
    private final UserRepository userRepository;

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

    public String getToken(String loginUserId, UserRoleCd role) {
        User user = userRepository.findByUserLoginIdAndUserRoleCdAndIsDeletedFalse(loginUserId, role)
            .orElseThrow(() -> new IllegalArgumentException("사용자 정보가 없습니다."));
        return jwtUtil.generateAccessToken(user);
    }

}
