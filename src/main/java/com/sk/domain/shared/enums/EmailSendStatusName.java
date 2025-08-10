package com.sk.domain.shared.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum EmailSendStatusName {
    PENDING("PENDING", "대기"),
    SENDING("SENDING", "발송중"),
    SUCCESS("SUCCESS", "발송성공"),
    FAILED("FAILED", "발송실패");

    private final String code;
    private final String name;
}
