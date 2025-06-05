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
    FAIL_EXCEED_LENGTH("F2", "실패", "길이제한 초과");

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
