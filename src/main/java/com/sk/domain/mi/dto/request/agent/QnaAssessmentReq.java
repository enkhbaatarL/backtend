package com.sk.domain.mi.dto.request.agent;

import com.sk.domain.shared.enums.agent.AssessmentFeedbackCd;
import io.swagger.v3.oas.annotations.media.Schema;

public record QnaAssessmentReq(

    @Schema(description = "추천 여부", example = "true", defaultValue = "false")
    Boolean isRecommended,

    @Schema(
        description = "피드백 코드, BUG_ISSUE(버그문제), NO_POSSIBLE_RESPONSE(가능한 응답이 없음), NOT_FOLLOW_REQUEST(요청에 제대로 따르지 않음), INACCURATE_INFORMATION(정보가 정확하지 않음), INCOMPLETE_RESPONSE(응답이 완전하지 않음), OTHER(기타)",
        example = "BUG_ISSUE",
        allowableValues = {"BUG_ISSUE","NO_POSSIBLE_RESPONSE", "NOT_FOLLOW_REQUEST", "INACCURATE_INFORMATION", "INCOMPLETE_RESPONSE", "OTHER"}
    )
    AssessmentFeedbackCd feedbackCd,

    @Schema(
        description = "피드백 내용 (feedbackCd가 'OTHER'인 경우에 피드백 내용을 추가할 수 있음)", example = "너무 느림")
    String feedbackComment

) {

}
