package com.sk.common.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class BooleanToYNAttributeConverter implements AttributeConverter<Boolean, String> {

    @Override
    public String convertToDatabaseColumn(Boolean attribute) {
        return attribute ? "Y" : "N";
    }

    @Override
    public Boolean convertToEntityAttribute(String yn) {
        return "Y".equalsIgnoreCase(yn);
    }

}
