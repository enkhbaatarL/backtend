package com.sk.common.config.security.mcp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Base64;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("MCP 시크릿 해시")
class McpSecretHasherTest {

    private final McpSecretHasher hasher = new McpSecretHasher(1000);

    @Test
    @DisplayName("시크릿은 256비트 난수를 Base64URL 43자로 만들고 매번 다르다")
    void generatesUrlSafe256BitSecret() {
        String secret = hasher.generateSecret();

        assertThat(secret).hasSize(43).matches("^[A-Za-z0-9_-]+$");
        assertThat(Base64.getUrlDecoder().decode(secret)).hasSize(32);
        assertThat(hasher.generateSecret()).isNotEqualTo(secret);
    }

    @Test
    @DisplayName("해시는 키마다 다른 솔트를 쓰고 알고리즘·반복 횟수·앞자리를 함께 남긴다")
    void hashesWithSaltAndParameters() {
        String secret = hasher.generateSecret();

        McpSecretHash first = hasher.hash(secret);
        McpSecretHash second = hasher.hash(secret);

        assertThat(first.algorithmNm()).isEqualTo("PBKDF2-HMAC-SHA256");
        assertThat(first.iterationCnt()).isEqualTo(1000);
        assertThat(first.prefixValue()).isEqualTo(secret.substring(0, 4));
        assertThat(Base64.getDecoder().decode(first.saltValue())).hasSize(16);
        assertThat(Base64.getDecoder().decode(first.hashValue())).hasSize(32);
        assertThat(first.hashValue()).doesNotContain(secret);
        assertThat(first.saltValue()).isNotEqualTo(second.saltValue());
        assertThat(first.hashValue()).isNotEqualTo(second.hashValue());
    }

    @Test
    @DisplayName("저장된 파라미터로 검증하고 틀린 시크릿·알고리즘은 거부한다")
    void matchesOnlyCorrectSecret() {
        String secret = hasher.generateSecret();
        McpSecretHash hash = hasher.hash(secret);

        assertThat(hasher.matches(secret, hash.algorithmNm(), hash.saltValue(), hash.iterationCnt(), hash.hashValue())).isTrue();
        assertThat(hasher.matches(secret + "x", hash.algorithmNm(), hash.saltValue(), hash.iterationCnt(), hash.hashValue())).isFalse();
        assertThat(hasher.matches(null, hash.algorithmNm(), hash.saltValue(), hash.iterationCnt(), hash.hashValue())).isFalse();
        assertThat(hasher.matches(secret, "SHA-1", hash.saltValue(), hash.iterationCnt(), hash.hashValue())).isFalse();
    }

    @Test
    @DisplayName("설정 반복 횟수가 바뀌어도 기존 키는 키별로 저장된 반복 횟수로 검증된다")
    void verifiesWithStoredIterations() {
        String secret = hasher.generateSecret();
        McpSecretHash oldHash = hasher.hash(secret);

        McpSecretHasher upgraded = new McpSecretHasher(5000);

        assertThat(upgraded.matches(secret, oldHash.algorithmNm(), oldHash.saltValue(), oldHash.iterationCnt(), oldHash.hashValue())).isTrue();
        assertThat(upgraded.hash(secret).iterationCnt()).isEqualTo(5000);
    }

    @Test
    @DisplayName("반복 횟수 설정은 1000 미만을 허용하지 않는다")
    void rejectsTooFewIterations() {
        assertThatThrownBy(() -> new McpSecretHasher(999)).isInstanceOf(IllegalStateException.class);
    }

}
