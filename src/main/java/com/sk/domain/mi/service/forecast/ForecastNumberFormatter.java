package com.sk.domain.mi.service.forecast;

import java.math.BigDecimal;
import java.math.RoundingMode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ForecastNumberFormatter {

    private final ForecastDecimalScalePolicy forecastDecimalScalePolicy;

    public int resolveScale(int companyNo) {
        return forecastDecimalScalePolicy.resolveScale(companyNo);
    }

    public String scaleToPlainString(Double value, int companyNo) {
        if (value == null) {
            return null;
        }
        int scale = resolveScale(companyNo);
        return BigDecimal.valueOf(value).setScale(scale, RoundingMode.HALF_UP).toPlainString();
    }

    public String scaleToPlainString(String value, int companyNo) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            int scale = resolveScale(companyNo);
            return new BigDecimal(value).setScale(scale, RoundingMode.HALF_UP).toPlainString();
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
