package com.sk.domain.shared.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ChartRenderType {

    DATE("date"),
    MAIN("main"),
    REFERENCE("ref"),
    OPEN("open"),
    CLOSE("close"),
    HIGH("high"),
    LOW("low");

    private final String fieldName;

}
