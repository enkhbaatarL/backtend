package com.sk.domain.shared.converter;

import com.sk.domain.shared.enums.nextgen.ScenarioGroupStatus;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class ScenarioGroupStatusConverter implements AttributeConverter<ScenarioGroupStatus, String> {

    @Override
    public String convertToDatabaseColumn(ScenarioGroupStatus attribute) {
        return attribute == null ? null : attribute.getCode();
    }

    @Override
    public ScenarioGroupStatus convertToEntityAttribute(String dbData) {
        return dbData == null ? null : ScenarioGroupStatus.from(dbData);
    }

}
