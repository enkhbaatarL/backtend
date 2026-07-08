package com.sk.domain.shared.converter;

import com.sk.domain.shared.enums.nextgen.BasecaseReadMode;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class BasecaseReadModeConverter implements AttributeConverter<BasecaseReadMode, String> {

    @Override
    public String convertToDatabaseColumn(BasecaseReadMode attribute) {
        return attribute == null ? null : attribute.getCode();
    }

    @Override
    public BasecaseReadMode convertToEntityAttribute(String dbData) {
        // 대소문자는 from()이 equalsIgnoreCase로 처리. 여기선 null/공백만 방어.
        return (dbData == null || dbData.isBlank()) ? null : BasecaseReadMode.from(dbData.trim());
    }

}
