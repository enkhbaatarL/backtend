package com.sk.common.config.security.oauth2;

import com.sk.common.utils.AESUtil;
import org.springframework.security.crypto.password.PasswordEncoder;

public class AesPasswordEncoder implements PasswordEncoder {

    private final String secretKey;

    public AesPasswordEncoder(String secretKey) {
        this.secretKey = secretKey;
    }

    @Override
    public String encode(CharSequence rawPassword) {
        return AESUtil.encrypt(rawPassword.toString(), secretKey);
    }

    @Override
    public boolean matches(CharSequence rawPassword, String encodedPassword) {
        // encodedPassword 는 DB에서 꺼낸 AES 암호화된 값
        String decrypted = AESUtil.decrypt(encodedPassword, secretKey);
        return rawPassword.toString().equals(decrypted);
    }

}
