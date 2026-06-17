package com.sk.domain.shared.converter;

import com.sk.domain.shared.enums.nextgen.ProbabilityDirection;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class ProbabilityDirectionConverter implements AttributeConverter<ProbabilityDirection, String> {

    @Override
    public String convertToDatabaseColumn(ProbabilityDirection attribute) {
        // DB 적재 코드는 대문자 (예: SUP)
        return attribute == null ? null : attribute.name();
    }

    @Override
    public ProbabilityDirection convertToEntityAttribute(String dbData) {
        // 대소문자는 from()이 equalsIgnoreCase로 처리. 여기선 null/공백만 방어.
        return (dbData == null || dbData.isBlank()) ? null : ProbabilityDirection.from(dbData.trim());
    }

}
