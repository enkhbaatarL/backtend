package com.sk.domain.shared.entity.master.tlm;

import static org.assertj.core.api.Assertions.assertThat;

import com.sk.domain.shared.enums.Role;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("API 접근 로그 엔티티")
class ApiAccessLogTest {

    @Test
    // [MCP Feedback] 길이 제한 없는 MCP 피드백 body 로 req_data_cntt(text, 65,535 byte) 저장이 실패하지 않게 한다
    @DisplayName("요청 body 는 16,300자로 잘라 text 컬럼(65,535 byte)에 들어가게 한다")
    void truncatesLongRequestBody() {
        ApiAccessLog log = ApiAccessLog.of("/api/v1/mcp/mi/next-gen/user-feedback", "POST", "😀".repeat(100_000), null, null, Role.USER, "user");

        assertThat(log.getRequestDataCntt()).hasSize(16_300);
        assertThat(log.getRequestDataCntt().getBytes(StandardCharsets.UTF_8).length).isLessThanOrEqualTo(65_535);
    }

    @Test
    @DisplayName("짧은 body 와 null 은 그대로 둔다")
    void keepsShortBody() {
        assertThat(ApiAccessLog.of("/u", "POST", "{\"a\":1}", null, null, Role.USER, "user").getRequestDataCntt()).isEqualTo("{\"a\":1}");
        assertThat(ApiAccessLog.of("/u", "GET", null, null, null, Role.USER, "user").getRequestDataCntt()).isNull();
    }
}
