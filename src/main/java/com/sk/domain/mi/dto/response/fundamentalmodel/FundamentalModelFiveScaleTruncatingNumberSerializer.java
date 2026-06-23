package com.sk.domain.mi.dto.response.fundamentalmodel;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;

public class FundamentalModelFiveScaleTruncatingNumberSerializer extends JsonSerializer<Double> {

    public static final int SCALE = 5;

    public static Double truncate(BigDecimal value) {
        return truncateToBigDecimal(value).doubleValue();
    }

    public static Double truncate(double value) {
        return truncate(BigDecimal.valueOf(value));
    }

    private static BigDecimal truncateToBigDecimal(BigDecimal value) {
        return value.setScale(SCALE, RoundingMode.DOWN).stripTrailingZeros();
    }

    @Override
    public void serialize(Double value, JsonGenerator generator, SerializerProvider serializers) throws IOException {
        if (value == null) {
            generator.writeNull();
            return;
        }
        generator.writeNumber(truncateToBigDecimal(BigDecimal.valueOf(value)));
    }
}
