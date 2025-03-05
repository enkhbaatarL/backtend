package com.sk.mi;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class OAuthTest {

    @Test
    void passwordEncoderTest() {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String encodedSecret = encoder.encode("test-secret");
        System.out.println(encodedSecret);
    }

}
