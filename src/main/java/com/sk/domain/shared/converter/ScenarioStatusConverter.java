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
        // 대소문자는 from()이 equalsIgnoreCase로 처리. 여기선 null/공백만 방어.
        return (dbData == null || dbData.isBlank()) ? null : ScenarioStatus.from(dbData.trim());
    }

}
