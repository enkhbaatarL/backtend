package com.sk.domain.shared.enums;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.Getter;

@Getter
public enum ScreenElementCode {

    AI_FORECAST_MODEL("AI_FORECAST_MODEL", true),
    SIGNAL_MAP("SIGNAL_MAP", true);

    private static final Set<String> FORECAST_DATA_REQUIRED_CODES = Arrays.stream(values())
        .filter(ScreenElementCode::isForecastDataRequired)
        .map(ScreenElementCode::getCode)
        .collect(Collectors.toUnmodifiableSet());

    private final String code;
    private final boolean forecastDataRequired;

    ScreenElementCode(String code, boolean forecastDataRequired) {
        this.code = code;
        this.forecastDataRequired = forecastDataRequired;
    }

    // Forecast 데이터가 없을 때 노출하면 안 되는 화면요소인지 확인한다.
    public static boolean requiresForecastData(String code) {
        return FORECAST_DATA_REQUIRED_CODES.contains(code);
    }

}
