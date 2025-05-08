package com.sk.domain.shared.enums.agent;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AssessmentFeedbackCd {

    BUG_ISSUE("01", "버그문제"),
    NO_POSSIBLE_RESPONSE("02", "가능한 응답이 없음"),
    NOT_FOLLOW_REQUEST("03", "요청에 제대로 따르지 않음"),
    INACCURATE_INFORMATION("04", "정보가 정확하지 않음"),
    INCOMPLETE_RESPONSE("05", "응답이 완전하지 않음"),
    OTHER("06", "기타");

    private final String code;
    private final String description;

}
