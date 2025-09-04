package com.sk.common.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class BcryptHashUtil {

    private final BCryptPasswordEncoder bcryptPasswordEncoder = new BCryptPasswordEncoder(12);

    /**
     * BCrypt를 사용하여 비밀번호 해시 생성
     *
     * @param plainPassword 평문 비밀번호
     * @return BCrypt 해시된 비밀번호
     */
    public String generateBcryptHash(String plainPassword) {
        if (plainPassword == null || plainPassword.trim().isEmpty()) {
            throw new IllegalArgumentException("비밀번호는 null이거나 빈 값일 수 없습니다.");
        }

        try {
            String hashedPassword = bcryptPasswordEncoder.encode(plainPassword);
            log.info("BCrypt 해시 생성 완료 - 원본 길이: {}, 해시 길이: {}",
                    plainPassword.length(), hashedPassword.length());
            return hashedPassword;
        } catch (Exception e) {
            log.error("BCrypt 해시 생성 중 오류 발생: {}", e.getMessage());
            throw new RuntimeException("비밀번호 해시 생성에 실패했습니다.", e);
        }
    }

    /**
     * BCrypt 해시된 비밀번호 검증
     *
     * @param plainPassword 평문 비밀번호
     * @param hashedPassword BCrypt 해시된 비밀번호
     * @return 검증 결과
     */
    public boolean verifyBcryptPassword(String plainPassword, String hashedPassword) {
        if (plainPassword == null || hashedPassword == null) {
            return false;
        }

        try {
            boolean matches = bcryptPasswordEncoder.matches(plainPassword, hashedPassword);
            log.debug("BCrypt 비밀번호 검증 결과: {}", matches);
            return matches;
        } catch (Exception e) {
            log.error("BCrypt 비밀번호 검증 중 오류 발생: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 비밀번호 강도 검증 (선택적 사용)
     *
     * @param password 검증할 비밀번호
     * @return 검증 결과
     */
    public boolean validatePasswordStrength(String password) {
        if (password == null) {
            return false;
        }

        // 기본적인 비밀번호 강도 검증 규칙
        // - 최소 8자 이상
        // - 최소 1개의 대문자, 소문자, 숫자, 특수문자 포함
        boolean lengthCheck = password.length() >= 8;
        boolean uppercaseCheck = password.matches(".*[A-Z].*");
        boolean lowercaseCheck = password.matches(".*[a-z].*");
        boolean digitCheck = password.matches(".*[0-9].*");
        boolean specialCharCheck = password.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?].*");

        boolean isValid = lengthCheck && uppercaseCheck && lowercaseCheck && digitCheck && specialCharCheck;

        log.debug("비밀번호 강도 검증 결과 - 길이: {}, 대문자: {}, 소문자: {}, 숫자: {}, 특수문자: {}, 전체: {}",
                lengthCheck, uppercaseCheck, lowercaseCheck, digitCheck, specialCharCheck, isValid);

        return isValid;
    }
}
