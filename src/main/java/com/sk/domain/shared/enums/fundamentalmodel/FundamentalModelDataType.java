package com.sk.domain.shared.enums.fundamentalmodel;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Arrays;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum FundamentalModelDataType {

    DEMAND("DEMAND", "Demand"),
    PRODUCTION("PRODUCTION", "Production"),
    LNG_IMPORTS("LNG_IMPORTS", "LNG Imports"),
    LNG_EXPORTS("LNG_EXPORTS", "LNG Exports"),
    PIPELINE_IMPORTS("PIPELINE_IMPORTS", "Pipeline Imports"),
    PIPELINE_EXPORTS("PIPELINE_EXPORTS", "Pipeline Exports");

    @JsonValue
    private final String code;
    private final String label;

    @JsonCreator
    public static FundamentalModelDataType from(String value) {
        return Arrays.stream(values())
            .filter(type -> type.name().equalsIgnoreCase(value) || type.code.equalsIgnoreCase(value))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Unknown FundamentalModelDataType: " + value));
    }
}
