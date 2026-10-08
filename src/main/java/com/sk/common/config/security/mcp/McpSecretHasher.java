package com.sk.common.config.security.mcp;

import com.sk.common.enums.ResultCode;
import com.sk.common.exception.SystemErrorException;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * [MCP 인증] MCP Basic Auth 시크릿 생성·해시·검증.
 * <ul>
 *   <li>생성: DRBG(Hash_DRBG, SHA-256) 32바이트 → Base64URL(패딩 없음) 43자</li>
 *   <li>저장: PBKDF2-HMAC-SHA256, 키별 16바이트 랜덤 솔트, 반복 횟수는 키마다 DB 에 저장 (KISA 권고: SHA-2 계열 일방향 암호화 + 솔트)</li>
 *   <li>검증: 저장된 반복 횟수로 다시 계산해 상수 시간 비교</li>
 * </ul>
 */
@Component
public class McpSecretHasher {

    public static final String ALGORITHM_NM = "PBKDF2-HMAC-SHA256";

    private static final String JCA_ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final int SECRET_BYTES = 32;   // 256bit
    private static final int SALT_BYTES = 16;     // 128bit
    private static final int HASH_BITS = 256;
    private static final int PREFIX_LENGTH = 4;
    private static final int MIN_ITERATIONS = 1000;

    private static final Base64.Encoder SECRET_ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Encoder STORE_ENCODER = Base64.getEncoder();
    private static final Base64.Decoder STORE_DECODER = Base64.getDecoder();

    private final SecureRandom secureRandom;
    private final int iterations;

    // 존재하지 않는 ID 에도 같은 비용의 해시 계산을 수행해 응답 시간으로 ID 존재 여부가 드러나지 않게 함
    private final McpSecretHash dummyHash;

    public McpSecretHasher(@Value("${mcp.auth.hash-iterations}") int iterations) {
        if (iterations < MIN_ITERATIONS) {
            throw new IllegalStateException("mcp.auth.hash-iterations must be >= %d".formatted(MIN_ITERATIONS));
        }
        this.iterations = iterations;
        this.secureRandom = drbg();
        this.dummyHash = hash(generateSecret());
    }

    /** 평문 시크릿을 생성합니다. 호출자는 이 값을 저장하지 말고 발급 응답으로만 내려야 합니다. */
    public String generateSecret() {
        return SECRET_ENCODER.encodeToString(randomBytes(SECRET_BYTES));
    }

    /** 설정된 반복 횟수와 새 솔트로 시크릿을 해시합니다. */
    public McpSecretHash hash(String secret) {
        byte[] salt = randomBytes(SALT_BYTES);
        byte[] hash = pbkdf2(secret, salt, iterations);
        return new McpSecretHash(
            STORE_ENCODER.encodeToString(hash),
            STORE_ENCODER.encodeToString(salt),
            ALGORITHM_NM,
            iterations,
            secret.substring(0, PREFIX_LENGTH));
    }

    /** 저장된 값(알고리즘·솔트·반복 횟수)으로 다시 계산해 상수 시간으로 비교합니다. */
    public boolean matches(String secret, String algorithmNm, String saltValue, Integer iterationCnt, String hashValue) {
        if (secret == null || !ALGORITHM_NM.equals(algorithmNm) || iterationCnt == null || iterationCnt < 1) {
            return false;
        }
        byte[] expected = STORE_DECODER.decode(hashValue);
        byte[] actual = pbkdf2(secret, STORE_DECODER.decode(saltValue), iterationCnt);
        return MessageDigest.isEqual(expected, actual);
    }

    /** 존재하지 않는 ID 일 때 호출합니다. 결과는 항상 false 입니다. */
    public boolean matchesDummy(String secret) {
        matches(secret == null ? "" : secret, dummyHash.algorithmNm(), dummyHash.saltValue(), dummyHash.iterationCnt(), dummyHash.hashValue());
        return false;
    }

    private byte[] pbkdf2(String secret, byte[] salt, int iterationCnt) {
        PBEKeySpec spec = new PBEKeySpec(secret.toCharArray(), salt, iterationCnt, HASH_BITS);
        try {
            return SecretKeyFactory.getInstance(JCA_ALGORITHM).generateSecret(spec).getEncoded();
        } catch (GeneralSecurityException e) {
            throw SystemErrorException.of(ResultCode.Error.SEC_ENCRYPTION_FAILED, e.getMessage());
        } finally {
            spec.clearPassword();
        }
    }

    private byte[] randomBytes(int length) {
        byte[] bytes = new byte[length];
        secureRandom.nextBytes(bytes);
        return bytes;
    }

    private static SecureRandom drbg() {
        try {
            // JDK 기본 DRBG 메커니즘은 Hash_DRBG(SHA-256) 이며 thread-safe 입니다.
            return SecureRandom.getInstance("DRBG");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("DRBG SecureRandom is not available", e);
        }
    }

}
