package com.sk.domain.shared.enums;

import lombok.Getter;

public enum ActivityLogType {
    CUST("CUSTOMER", "고객사"),
    COMP("COMPETITOR","경쟁사"),
    REGU("REGULATION", "법규"),
    TECH("TECHNOLOGY","기술"),
    OTHE("OTHER", "기타");

    private final String code;

    @Getter
    private final String name;

    ActivityLogType(String code, String name) {
        this.code = code;
        this.name = name;
    }
}
