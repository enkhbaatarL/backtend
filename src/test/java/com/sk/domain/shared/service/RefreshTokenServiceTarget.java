package com.sk.domain.shared.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class RefreshTokenServiceTarget {

    @Autowired
    RefreshTokenService refreshTokenService;

    @Test
    void saveRefreshToken() {
        refreshTokenService.saveRefreshToken(123, "aaaaaaaaaaaaaaaaaa", 1742449657);
    }

    @Test
    void getRefreshToken() {
        String refreshToken = refreshTokenService.getRefreshToken(123);
        System.out.println(refreshToken);
    }

    @Test
    void validateRefreshToken() {
        boolean v1 = refreshTokenService.validateRefreshToken(123, "aaaaaaaaaaaaaaaaaa");
        System.out.println(v1);
        boolean v2 = refreshTokenService.validateRefreshToken(123, "aaaaaaaaaaaaaaaaab");
        System.out.println(v2);
    }

    @Test
    void deleteRefreshToken() {
        refreshTokenService.deleteRefreshToken(1234);
    }

}
