package com.sk.domain.shared.enums.topic;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum TopicApprovalStatusCode {
    PENDING("PEN", "Pending", "토픽 승인 대기"),
    APPROVED("APRV", "Approved", "토픽 승인"),
    REJECTED("RJCT", "Not Approved", "토픽 승인 거절");

    private final String code;
    private final String name;
    private final String description;
}
