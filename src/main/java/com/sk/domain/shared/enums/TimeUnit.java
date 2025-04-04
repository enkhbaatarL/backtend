package com.sk.domain.shared.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum TimeUnit {

    SECOND("s", "초"),
    MINUTE("m", "분"),
    HOUR("h", "시간"),
    DAY("d", "일"),
    WEEK("w", "주"),
    MONTH("M", "월"),
    YEAR("y", "년");

    private final String code;
    private final String description;

}
