package com.sk.domain.shared.enums.chart;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

@Getter
@RequiredArgsConstructor
public enum RenderAs {

    DATE("date", 0, false, null), // description RDB로 관리됨.
    MAIN("main", 1, true, null),
    REFERENCE("ref_%s", 2, false, null),
    CLOSE("close", 1, true, null),
    OPEN("open", 2, false, null),
    HIGH("high", 3, false, null),
    LOW("low", 4, false, null),
    MA("ma_%s", null, false, "이동평균선"),
    BB("bb_%s", null, false, "볼린저밴드"),
    VOLUME("volume", null, false, "거래량");

    private final String id;
    private final Integer index;
    private final Boolean isMajor;
    private final String displayDescription;

    private static final List<RenderAs> majorTypes;

    static {
        majorTypes = List.of(MAIN, CLOSE);
    }

    public static boolean isMajorType(RenderAs type) {
        return majorTypes.contains(type);
    }

    public String getRenderingId(String suffix) {
        return String.format(id, suffix);
    }

}
