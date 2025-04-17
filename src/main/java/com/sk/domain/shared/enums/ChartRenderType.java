package com.sk.domain.shared.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

@Getter
@RequiredArgsConstructor
public enum ChartRenderType {

    DATE("DATE", false, null),
    MAIN("MAIN", true, null),
    REFERENCE("REF", false, null),
    OPEN("OPEN", false, null),
    CLOSE("CLOSE", true, null),
    HIGH("HIGH", false, null),
    LOW("LOW", false, null),
    MA("MA", false, "이동평균"),
    BB("BB", false, "볼린저밴드"),
    UNKNOWN("UNKNOWN", false, null);

    private final String code;
    private final Boolean isMajor;
    private final String description;

    private static final List<ChartRenderType> majorTypes;

    static {
        majorTypes = List.of(MAIN, CLOSE);
    }

    public static boolean isMajorType(ChartRenderType type) {
        return majorTypes.contains(type);
    }

}
