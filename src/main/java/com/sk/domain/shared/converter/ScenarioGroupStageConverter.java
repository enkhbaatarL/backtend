package com.sk.domain.shared.converter;

import com.sk.domain.shared.enums.nextgen.ScenarioGroupStage;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class ScenarioGroupStageConverter implements AttributeConverter<ScenarioGroupStage, String> {

    @Override
    public String convertToDatabaseColumn(ScenarioGroupStage attribute) {
        return attribute == null ? null : attribute.getCode();
    }

    @Override
    public ScenarioGroupStage convertToEntityAttribute(String dbData) {
        return dbData == null ? null : ScenarioGroupStage.from(dbData);
    }

}
