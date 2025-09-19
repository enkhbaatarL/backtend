package com.sk.common.converter;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import org.springframework.boot.jackson.JsonComponent;

@JsonComponent
public class DoubleTwoScaleSerializer extends JsonSerializer<Double> {


    @Override
    public void serialize(Double doubleVal, JsonGenerator jsonGenerator,
        SerializerProvider serializerProvider) throws IOException {
        if (doubleVal == null) {
            jsonGenerator.writeNull();
            return;
        }
        BigDecimal scaled = BigDecimal.valueOf(doubleVal).setScale(2, RoundingMode.HALF_UP);
        // 항상 두 자리: "123.40"
        jsonGenerator.writeString(scaled.toPlainString());
    }
}
