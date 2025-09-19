package com.sk.common.converter;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import org.springframework.boot.jackson.JsonComponent;

@JsonComponent
public class BigDecimalTwoScaleSerializer extends JsonSerializer<BigDecimal> {


    @Override
    public void serialize(BigDecimal bigDecimal, JsonGenerator jsonGenerator,
        SerializerProvider serializerProvider) throws IOException {
        if (bigDecimal == null) {
            jsonGenerator.writeNull();
            return;
        }
        BigDecimal scaled = bigDecimal.setScale(2, RoundingMode.HALF_UP);
        // 항상 두 자리: "123.40"
        jsonGenerator.writeString(scaled.toPlainString());
    }
}
