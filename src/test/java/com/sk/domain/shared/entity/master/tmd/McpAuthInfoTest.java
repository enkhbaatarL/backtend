package com.sk.domain.shared.entity.master.tmd;

import static org.assertj.core.api.Assertions.assertThat;

import com.sk.domain.admin.dto.request.mcp.McpAuthInfoReq;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("MCP 인증 정보 엔티티")
class McpAuthInfoTest {

    @Test
    @DisplayName("삭제하면 ID 를 d_{등록번호}_{원래ID} 로 바꿔 같은 ID 로 재등록할 수 있게 한다")
    void renamesIdOnDelete() {
        McpAuthInfo mcpAuthInfo = McpAuthInfo.builder()
            .mcpAuthRegNo(12)
            .mcpAuthId("skcc-mcp")
            .isDeleted(false)
            .build();

        mcpAuthInfo.delete();

        assertThat(mcpAuthInfo.getMcpAuthId()).isEqualTo("d_12_skcc-mcp");
        assertThat(mcpAuthInfo.getIsDeleted()).isTrue();
    }

    @Test
    @DisplayName("삭제된 ID 는 입력 규칙에 맞지 않아 새 입력과 겹칠 수 없고 컬럼 길이 안에 들어간다")
    void deletedIdNeverCollidesWithInput() {
        String longestId = "a".repeat(McpAuthInfoReq.MCP_AUTH_ID_MAX_LENGTH);
        McpAuthInfo mcpAuthInfo = McpAuthInfo.builder()
            .mcpAuthRegNo(Integer.MAX_VALUE)
            .mcpAuthId(longestId)
            .isDeleted(false)
            .build();

        mcpAuthInfo.delete();

        assertThat(mcpAuthInfo.getMcpAuthId()).doesNotMatch(McpAuthInfoReq.MCP_AUTH_ID_REGEXP);
        assertThat(mcpAuthInfo.getMcpAuthId()).hasSizeLessThanOrEqualTo(70);
    }

}
