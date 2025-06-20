package com.sk.domain.shared.enums.agent;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AnswerStatusCd {

    COMPLETE("C", "완료", "완료"),
    PENDING("P", "대기", "대기"),
    FAIL("F", "실패", "실패"),
    FAIL_FORBIDDEN_WORDS("F1", "실패", "금칙어"),
    FAIL_EXCEED_LENGTH("F2", "실패", "길이제한 초과"),
    FAIL_EXCEED_TOKEN("F3", "실패", "토큰 한도 초과"),
    FAIL_TIMED_OUT("F4", "실패", "에이전트 타임아웃");

    private final String code;
    private final String description;
    private final String descriptionDetail;

    public static AnswerStatusCd of(String code) {
        for (AnswerStatusCd status : AnswerStatusCd.values()) {
            if (status.getCode().equals(code)) {
                return status;
            }
        }
        return AnswerStatusCd.FAIL;
    }

}
