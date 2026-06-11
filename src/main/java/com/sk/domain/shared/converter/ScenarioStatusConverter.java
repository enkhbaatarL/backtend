package com.sk.domain.shared.converter;

import com.sk.domain.shared.enums.nextgen.ScenarioStatus;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class ScenarioStatusConverter implements AttributeConverter<ScenarioStatus, String> {

    @Override
    public String convertToDatabaseColumn(ScenarioStatus attribute) {
        return attribute == null ? null : attribute.getCode();
    }

    @Override
    public ScenarioStatus convertToEntityAttribute(String dbData) {
        return dbData == null ? null : ScenarioStatus.from(dbData);
    }

}
