package com.sk.domain.shared.enums.newsletter;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PublishStatusCode {
    PUBLISHING(0, "PUBLISHING", "발행중"),
    PUBLISHED(1, "PUBLISHED", "발행완료"),
    SUSPENDED(2, "SUSPENDED", "발행중단");

    private final int code;
    private final String status;
    private final String description;
}