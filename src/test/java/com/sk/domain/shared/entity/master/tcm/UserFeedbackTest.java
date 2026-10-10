package com.sk.domain.shared.entity.master.tcm;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sk.domain.shared.dto.request.common.McpUserFeedbackReq;
import com.sk.domain.shared.dto.request.common.UserFeedbackReq;
import com.sk.domain.shared.enums.FeedbackTypeCd;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * [MCP Feedback] 화면·MCP 피드백 저장값과 MCP 요청의 관대한 처리.
 */
@DisplayName("사용자 피드백 엔티티")
class UserFeedbackTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    @DisplayName("화면 피드백은 채널 SCREEN, 대상 ID 없음으로 저장한다 (fedb_chnl_cd NOT NULL)")
    void screenFeedbackUsesScreenChannel() {
        UserFeedback feedback = UserFeedback.of(
            new UserFeedbackReq("Event List", "Event", FeedbackTypeCd.UX_UI, "내용"), null);

        assertThat(feedback.getFeedbackChannelCd()).isEqualTo("SCREEN");
        assertThat(feedback.getFeedbackTargetId()).isNull();
        assertThat(feedback.getFeedbackPageNm()).isEqualTo("Event List");
        assertThat(feedback.getFeedbackSectionNm()).isEqualTo("Event");
        assertThat(feedback.getFeedbackTypeCd()).isEqualTo("ux_ui");
    }

    @Test
    @DisplayName("MCP 피드백은 채널 MCP 로 대상 종류·ID·이름·내용을 저장한다")
    void mcpFeedbackStoresTarget() {
        UserFeedback feedback = UserFeedback.ofMcp(
            new McpUserFeedbackReq("  전망 요약이 뉴스와 다릅니다.  ", "agent_output", "Scenario Group", "12", "EU 저장 보조금 시나리오"), null);

        assertThat(feedback.getFeedbackChannelCd()).isEqualTo("MCP");
        assertThat(feedback.getFeedbackSectionNm()).isEqualTo("Scenario Group");
        assertThat(feedback.getFeedbackTargetId()).isEqualTo(12L);
        assertThat(feedback.getFeedbackPageNm()).isEqualTo("EU 저장 보조금 시나리오");
        assertThat(feedback.getFeedbackTypeCd()).isEqualTo("agent_output");
        assertThat(feedback.getFeedbackContent()).isEqualTo("전망 요약이 뉴스와 다릅니다.");
    }

    @ParameterizedTest(name = "[{index}] 유형 ''{0}'' → {1}")
    @CsvSource(value = {
        "agent_output, agent_output", "AGENT_OUTPUT, agent_output", "Agent Output, agent_output", "agent, agent_output",
        "UX/UI, ux_ui", "ui, ux_ui", "feature-suggestion, feature_suggestion", "기능 제안, feature_suggestion",
        "기타, etc", "data_error, etc", "모르는값, etc", "NULL, etc"
    }, nullValues = "NULL")
    @DisplayName("유형은 대소문자·별칭을 허용하고 모르면 etc 로 저장한다")
    void normalizesFeedbackType(String input, String expected) {
        UserFeedback feedback = UserFeedback.ofMcp(new McpUserFeedbackReq("내용", input, null, null, null), null);

        assertThat(feedback.getFeedbackTypeCd()).isEqualTo(expected);
    }

    @ParameterizedTest(name = "[{index}] 섹션 ''{0}'' → {1}")
    @CsvSource(value = {
        "Event, Event", "event, Event", "이벤트, Event", "scenario-group, Scenario Group", "SCENARIO_GROUP, Scenario Group",
        "시나리오 그룹, Scenario Group", "scenario, Scenario", "시나리오, Scenario", "Overview, Overview", "NULL, NULL", "'  ', NULL"
    }, nullValues = "NULL")
    @DisplayName("대상 종류는 지원값으로 맞추고, 모르는 값은 원문 그대로 저장한다")
    void normalizesSection(String input, String expected) {
        UserFeedback feedback = UserFeedback.ofMcp(new McpUserFeedbackReq("내용", null, input, null, null), null);

        assertThat(feedback.getFeedbackSectionNm()).isEqualTo(expected);
    }

    @ParameterizedTest(name = "[{index}] ID ''{0}'' → {1}")
    @CsvSource(value = {"1269, 1269", "' 1269 ', 1269", "#1269, 1269", "인도 이벤트, NULL", "0, NULL", "-3, NULL", "12.5, NULL",
        "99999999999999999999, NULL", "NULL, NULL"}, nullValues = "NULL")
    @DisplayName("대상 ID 는 숫자일 때만 저장하고, 아니면 거부하지 않고 비워 둔다")
    void parsesTargetId(String input, Long expected) {
        UserFeedback feedback = UserFeedback.ofMcp(new McpUserFeedbackReq("내용", null, "Event", input, null), null);

        assertThat(feedback.getFeedbackTargetId()).isEqualTo(expected);
    }

    @Test
    @DisplayName("이름·모르는 섹션은 300자로 자르고, 내용은 2000자를 넘어도 그대로 저장한다")
    void truncatesNamesButKeepsLongContent() {
        String longContent = "가".repeat(50_000);
        UserFeedback feedback = UserFeedback.ofMcp(
            new McpUserFeedbackReq(longContent, null, "x".repeat(400), null, "y".repeat(400)), null);

        assertThat(feedback.getFeedbackPageNm()).hasSize(300);
        assertThat(feedback.getFeedbackSectionNm()).hasSize(300);
        assertThat(feedback.getFeedbackContent()).hasSize(50_000);
    }

    @Test
    @DisplayName("MCP 요청 검증은 내용이 비었을 때만 실패한다")
    void validatesOnlyContent() {
        assertThat(validator.validate(new McpUserFeedbackReq(" ", "agent_output", "Event", "1", "a"))).hasSize(1);
        assertThat(validator.validate(new McpUserFeedbackReq(null, null, null, null, null))).hasSize(1);
        assertThat(validator.validate(
            new McpUserFeedbackReq("가".repeat(10_000), "모름", "모름", "모름", "z".repeat(1_000)))).isEmpty();
    }

    @Test
    @DisplayName("JSON 의 숫자 targetId 도 문자열로 받아 역직렬화 오류가 나지 않는다")
    void deserializesNumericTargetId() throws Exception {
        McpUserFeedbackReq request = new ObjectMapper().readValue(
            "{\"feedbackContent\":\"내용\",\"targetId\":1269,\"feedbackTypeCd\":\"unknown\"}", McpUserFeedbackReq.class);

        assertThat(request.targetId()).isEqualTo("1269");
        assertThat(UserFeedback.ofMcp(request, null).getFeedbackTargetId()).isEqualTo(1269L);
    }
}
