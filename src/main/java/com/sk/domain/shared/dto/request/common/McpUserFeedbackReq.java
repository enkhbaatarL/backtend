package com.sk.domain.shared.dto.request.common;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * [MCP Feedback] MCP 사용자 피드백 등록 요청.
 * 사용자는 시스템 값을 모르므로 내용 외에는 검증하지 않는다. 형식이 틀린 값으로 400 이 나지 않도록 모두 String 으로 받고 서비스에서 정규화한다.
 * 필드 description 은 그대로 MCP tool 인자 설명이 된다.
 */
@Schema(description = "MCP 사용자 피드백 등록 요청")
public record McpUserFeedbackReq(

    @Schema(description = "사용자의 피드백 원문. 요약하거나 바꿔 쓰지 말고 사용자의 말을 그대로 넣습니다. 길이 제한은 없습니다.",
        example = "이 이벤트의 전망 요약이 최근 뉴스와 맞지 않습니다.", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "피드백 내용은 필수입니다.")
    String feedbackContent,

    @Schema(description = """
        피드백 유형. agent_output: AI·MCP 가 제공한 분석·요약·전망·답변 내용에 대한 의견(내용·데이터 관련은 대부분 이것) / \
        ux_ui: 화면이나 사용 방식, 응답 형태의 불편·개선 / feature_suggestion: 없는 기능·데이터·조회 조건 요청 / etc: 그 외. \
        판단이 어려우면 etc 를 넣거나 생략합니다.""",
        allowableValues = {"agent_output", "ux_ui", "feature_suggestion", "etc"}, example = "agent_output")
    String feedbackTypeCd,

    @Schema(description = """
        피드백 대상 종류. Event: 특정 이벤트 / Scenario Group: 특정 시나리오 그룹 / Scenario: 특정 개별 시나리오. \
        특정 대상이 없거나 사용 전반에 대한 피드백이면 생략합니다.""",
        allowableValues = {"Event", "Scenario Group", "Scenario"}, example = "Event")
    String feedbackSectionNm,

    @Schema(description = """
        피드백 대상의 ID. feedbackSectionNm 이 Event 면 eventId, Scenario Group 이면 scenarioGroupId, Scenario 면 scenarioId 입니다. \
        이벤트·시나리오 조회 결과에 나온 값을 그대로 넣고, 확실하지 않으면 추측하지 말고 생략합니다.""",
        type = "integer", format = "int64", example = "1269")
    String targetId,

    @Schema(description = """
        피드백 대상의 이름(이벤트 제목, 시나리오 그룹명, 시나리오명) 또는 피드백 주제. \
        ID 를 모를 때도 사용자가 부른 이름을 넣습니다. 300자를 넘으면 잘립니다.""",
        example = "[인도 가스정책] Control Order 유지·JKM 반등")
    String feedbackPageNm

) {}
