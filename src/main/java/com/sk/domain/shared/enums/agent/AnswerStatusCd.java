package com.sk.domain.shared.enums.agent;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AnswerStatusCd {

    COMPLETE("C", "완료"),
    PENDING("P", "대기"),
    FAIL("F", "실패");

    private final String code;
    private final String description;

}
