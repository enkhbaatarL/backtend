package com.sk.domain.mi.dto.response.fundamentalmodel;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;

public class FundamentalModelPriceTwoScaleRoundingNumberSerializer extends JsonSerializer<Double> {

    private static final int SCALE = 2;

    @Override
    public void serialize(Double value, JsonGenerator generator, SerializerProvider serializers) throws IOException {
        if (value == null) {
            generator.writeNull();
            return;
        }
        generator.writeNumber(BigDecimal.valueOf(value).setScale(SCALE, RoundingMode.HALF_UP));
    }
}
