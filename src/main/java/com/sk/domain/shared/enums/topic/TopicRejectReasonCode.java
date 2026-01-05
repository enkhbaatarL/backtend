package com.sk.domain.shared.enums.topic;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum TopicRejectReasonCode {
    LOW_DATA("LDAT", "Low Data", "Low Data"),
    IRRELEVANT("IRR", "Irrelevant", "Irrelevant"),
    DUPLICATE("DUP", "Duplicate", "Duplicate");

    private final String code;
    private final String name;
    private final String description;
}
