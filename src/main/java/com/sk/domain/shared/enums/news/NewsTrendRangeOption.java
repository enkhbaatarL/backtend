package com.sk.domain.shared.enums.news;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum NewsTrendRangeOption {
    LAST_7_DAYS("LAST_7_DAYS", "최근 7일"),
    LAST_14_DAYS("LAST_14_DAYS", "최근 14일");

    private final String code;
    private final String description;

    NewsTrendRangeOption(String code, String description) {
        this.code = code;
        this.description = description;
    }

    @JsonCreator
    public static NewsTrendRangeOption fromCode(String code) {
        for (NewsTrendRangeOption option : NewsTrendRangeOption.values()) {
            if (option.code.equalsIgnoreCase(code)) {
                return option;
            }
        }
        return LAST_14_DAYS;
    }

    @JsonValue
    public String getCode() {
        return code;
    }

    public DateRangeOption toDateRangeOption() {
        return switch (this) {
            case LAST_7_DAYS -> DateRangeOption.LAST_7_DAYS;
            case LAST_14_DAYS -> DateRangeOption.LAST_14_DAYS;
            default -> throw new IllegalStateException("지원하지 않는 NewsTrendRangeOption 값입니다: " + this);
        };
    }
}
